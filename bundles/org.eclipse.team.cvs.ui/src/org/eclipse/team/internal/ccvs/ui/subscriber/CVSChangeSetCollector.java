/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.ui.synchronize.*;

/**
 * Collector that fetches the log for incoming CVS change sets
 */
public class CVSChangeSetCollector extends SyncInfoSetChangeSetCollector implements LogEntryCacheUpdateHandler.ILogsFetchedListener {

    /*
     * Constant used to add the collector to the configuration of a page so
     * it can be accessed by the CVS custom actions
     */
    public static final String CVS_CHECKED_IN_COLLECTOR = CVSUIPlugin.ID + ".CVSCheckedInCollector"; //$NON-NLS-1$
    
    /*
     * Constant used to store the log entry handler in the configuration so it can
     * be kept around over layout changes
     */
    private static final String LOG_ENTRY_HANDLER = CVSUIPlugin.ID + ".LogEntryHandler"; //$NON-NLS-1$
    
    private static final String DEFAULT_INCOMING_SET_NAME = Policy.bind("CVSChangeSetCollector.0"); //$NON-NLS-1$
    
    boolean disposed = false;

    private LogEntryCache logEntryCache;
    
	/* *****************************************************************************
	 * Special sync info that has its kind already calculated.
	 */
	public class CVSUpdatableSyncInfo extends CVSSyncInfo {
		public int kind;
		public CVSUpdatableSyncInfo(int kind, IResource local, IResourceVariant base, IResourceVariant remote, Subscriber s) {
			super(local, base, remote, s);
			this.kind = kind;
		}

		protected int calculateKind() throws TeamException {
			return kind;
		}
	}
	
	private class DefaultCheckedInChangeSet extends CheckedInChangeSet {

	    private Date date = new Date();
	    
        public DefaultCheckedInChangeSet(String name) {
            setName(name);
        }
        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.CheckedInChangeSet#getAuthor()
         */
        public String getAuthor() {
            return ""; //$NON-NLS-1$
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.CheckedInChangeSet#getDate()
         */
        public Date getDate() {
            return date;
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.ChangeSet#getComment()
         */
        public String getComment() {
            return ""; //$NON-NLS-1$
        }
	    
	}
	
	private class CVSCheckedInChangeSet extends CheckedInChangeSet {

        private final ILogEntry entry;

        public CVSCheckedInChangeSet(ILogEntry entry) {
            this.entry = entry;
    		String date = DateFormat.getDateTimeInstance().format(entry.getDate());
    		String comment = HistoryView.flattenText(entry.getComment());
    		setName("["+entry.getAuthor()+ "] (" + date +") " + comment); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.CheckedInChangeSet#getAuthor()
         */
        public String getAuthor() {
            return entry.getAuthor();
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.CheckedInChangeSet#getDate()
         */
        public Date getDate() {
            return entry.getDate();
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.ChangeSet#getComment()
         */
        public String getComment() {
            return entry.getComment();
        }
	}
	
    public CVSChangeSetCollector(ISynchronizePageConfiguration configuration) {
        super(configuration);
        configuration.setProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR, this);
    }

    public synchronized LogEntryCacheUpdateHandler getLogEntryHandler() {
        LogEntryCacheUpdateHandler handler = (LogEntryCacheUpdateHandler)getConfiguration().getProperty(LOG_ENTRY_HANDLER);
        if (handler == null) {
            handler = initializeLogEntryHandler(getConfiguration());
        }
        handler.setListener(this);
        return handler;
    }
    
    /*
     * Initialize the log entry handler and place it in the configuration
     */
    private LogEntryCacheUpdateHandler initializeLogEntryHandler(ISynchronizePageConfiguration configuration) {
        final LogEntryCacheUpdateHandler logEntryHandler = new LogEntryCacheUpdateHandler(configuration);
        configuration.setProperty(LOG_ENTRY_HANDLER, logEntryHandler);
        // Use an action group to get notified when the configuration is disposed
        configuration.addActionContribution(new SynchronizePageActionGroup() {
            public void dispose() {
                super.dispose();
                LogEntryCacheUpdateHandler handler = (LogEntryCacheUpdateHandler)getConfiguration().getProperty(LOG_ENTRY_HANDLER);
                if (handler != null) {
                    handler.shutdown();
                    getConfiguration().setProperty(LOG_ENTRY_HANDLER, null);
                }
            }
        });
        return logEntryHandler;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.SyncInfoSetChangeSetCollector#add(org.eclipse.team.core.synchronize.SyncInfo[])
     */
    protected void add(SyncInfo[] infos) {
        LogEntryCacheUpdateHandler handler = getLogEntryHandler();
        if (handler != null)
            try {
                handler.fetch(infos);
            } catch (CVSException e) {
                getConfiguration().getSyncInfoSet().addError(new TeamStatus(IStatus.ERROR, CVSUIPlugin.ID, 0, e.getMessage(), e, null));
            }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.SyncInfoSetChangeSetCollector#reset(org.eclipse.team.core.synchronize.SyncInfoSet)
     */
    public void reset(SyncInfoSet seedSet) {
        // Notify thet handler to stop any fetches in progress
        LogEntryCacheUpdateHandler handler = getLogEntryHandler();
        if (handler != null) {
            handler.stopFetching();
        }
        super.reset(seedSet);
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.HierarchicalModelProvider#dispose()
	 */
	public void dispose() {
	    // No longer listen for log entry changes
	    // (The handler is disposed with the page)
	    disposed = true;
        LogEntryCacheUpdateHandler handler = getLogEntryHandler();
        if (handler != null) handler.setListener(null);
		getConfiguration().setProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR, null);
		logEntryCache = null;
		super.dispose();
	}
	
	/**
	 * Fetch the log histories for the remote changes and use this information
	 * to add each resource to an appropriate commit set.
     */
    private void handleRemoteChanges(final SyncInfo[] infos, final LogEntryCache logEntries, final IProgressMonitor monitor) {
        performUpdate(new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) {
                addLogEntries(infos, logEntries, monitor);
            }
        }, true  /* preserver expansion */, monitor);
    }
	
    /*
	 * Add the following sync info elements to the viewer. It is assumed that these elements have associated
	 * log entries cached in the log operation.
	 */
	private void addLogEntries(SyncInfo[] commentInfos, LogEntryCache logs, IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, commentInfos.length * 10);
			if (logs != null) {
				for (int i = 0; i < commentInfos.length; i++) {
					addSyncInfoToCommentNode(commentInfos[i], logs);
					monitor.worked(10);
				}
			}
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * Create a node for the given sync info object. The logs should contain the log for this info.
	 * 
	 * @param info the info for which to create a node in the model
	 * @param log the cvs log for this node
	 */
	private void addSyncInfoToCommentNode(SyncInfo info, LogEntryCache logs) {
	    LogEntryCacheUpdateHandler handler = getLogEntryHandler();
	    if (handler != null) {
			ICVSRemoteResource remoteResource = handler.getRemoteResource(info);
			if(handler.getSubscriber() instanceof CVSCompareSubscriber && remoteResource != null) {
				addMultipleRevisions(info, logs, remoteResource);
			} else {
				addSingleRevision(info, logs, remoteResource);
			}
	    }
	}
	
	/*
	 * Add a single log entry to the model.
	 * 
	 * @param info
	 * @param logs
	 * @param remoteResource
	 */
	private void addSingleRevision(SyncInfo info, LogEntryCache logs, ICVSRemoteResource remoteResource) {
		ILogEntry logEntry = logs.getLogEntry(remoteResource);
	    if (remoteResource != null && !remoteResource.isFolder()) {
			// For incoming deletions grab the comment for the latest on the same branch
			// which is now in the attic.
			try {
				String remoteRevision = ((ICVSRemoteFile) remoteResource).getRevision();
				if (isDeletedRemotely(info)) {
					ILogEntry[] logEntries = logs.getLogEntries(remoteResource);
					for (int i = 0; i < logEntries.length; i++) {
						ILogEntry entry = logEntries[i];
						String revision = entry.getRevision();
						if (entry.isDeletion() && ResourceSyncInfo.isLaterRevision(revision, remoteRevision)) {
							logEntry = entry;
						}
					}
				}
			} catch (TeamException e) {
				// continue and skip deletion checks
			}
	    }
		addRemoteChange(info, remoteResource, logEntry);
	}
	
    /*
	 * Add multiple log entries to the model.
	 * 
	 * @param info
	 * @param logs
	 * @param remoteResource
	 */
	private void addMultipleRevisions(SyncInfo info, LogEntryCache logs, ICVSRemoteResource remoteResource) {
		ILogEntry[] logEntries = logs.getLogEntries(remoteResource);
		if(logEntries == null || logEntries.length == 0) {
			// If for some reason we don't have a log entry, try the latest
			// remote.
			addRemoteChange(info, null, null);
		} else {
			for (int i = 0; i < logEntries.length; i++) {
				ILogEntry entry = logEntries[i];
				addRemoteChange(info, remoteResource, entry);
			}
		}
	}
	
	private boolean isDeletedRemotely(SyncInfo info) {
		int kind = info.getKind();
		if(kind == (SyncInfo.INCOMING | SyncInfo.DELETION)) return true;
		if(SyncInfo.getDirection(kind) == SyncInfo.CONFLICTING && info.getRemote() == null) return true;
		return false;
	}
	
    /*
     * Add the remote change to an incoming commit set
     */
    private void addRemoteChange(SyncInfo info, ICVSRemoteResource remoteResource, ILogEntry logEntry) {
        if (disposed) return;
        LogEntryCacheUpdateHandler handler = getLogEntryHandler();
        if(handler != null && remoteResource != null && logEntry != null && handler.isRemoteChange(info)) {
	        if(requiresCustomSyncInfo(info, remoteResource, logEntry)) {
	        	info = new CVSUpdatableSyncInfo(info.getKind(), info.getLocal(), info.getBase(), (RemoteResource)logEntry.getRemoteFile(), ((CVSSyncInfo)info).getSubscriber());
	        	try {
	        		info.init();
	        	} catch (TeamException e) {
	        		// this shouldn't happen, we've provided our own calculate kind
	        	}
	        }
	        // Only add the info if the base and remote differ
	        IResourceVariant base = info.getBase();
	        IResourceVariant remote = info.getRemote();
	        if ((base == null && remote != null) || (remote == null && base != null) || !base.equals(remote)) {
	            synchronized(this) {
			        ChangeSet set = getChangeSetFor(logEntry);
			        if (set == null) {
			            set = createChangeSetFor(logEntry);
			        	add(set);
			        }
			        set.add(info);
	            }
	        }
        } else {
            // The info was not retrieved for the remote change for some reason.
            // Add the node to the root
            addToDefaultSet(DEFAULT_INCOMING_SET_NAME, info);
        }
    }
    
    private void addToDefaultSet(String name, SyncInfo info) {
        ChangeSet set;
        synchronized(this) {
	        set = getChangeSetFor(name);
	        if (set == null) {
	            set = createDefaultChangeSet(name);
	        	add(set);
	        }
	        set.add(info);
        }
    }
    
    private ChangeSet createDefaultChangeSet(String name) {
        return new DefaultCheckedInChangeSet(name);
    }

    private ChangeSet createChangeSetFor(ILogEntry logEntry) {
        return new CVSCheckedInChangeSet(logEntry);
    }

    private ChangeSet getChangeSetFor(ILogEntry logEntry) {
        ChangeSet[] sets = getSets();
        for (int i = 0; i < sets.length; i++) {
            ChangeSet set = sets[i];
            if (set instanceof CheckedInChangeSet &&
                    set.getComment().equals(logEntry.getComment()) &&
                    ((CheckedInChangeSet)set).getAuthor().equals(logEntry.getAuthor())) {
                return set;
            }
        }
        return null;
    }

    private ChangeSet getChangeSetFor(String name) {
        ChangeSet[] sets = getSets();
        for (int i = 0; i < sets.length; i++) {
            ChangeSet set = sets[i];
            if (set.getName().equals(name)) {
                return set;
            }
        }
        return null;
    }
    
    private boolean requiresCustomSyncInfo(SyncInfo info, ICVSRemoteResource remoteResource, ILogEntry logEntry) {
		// Only interested in non-deletions
		if (logEntry.isDeletion() || !(info instanceof CVSSyncInfo)) return false;
		// Only require a custom sync info if the remote of the sync info
		// differs from the remote in the log entry
		IResourceVariant remote = info.getRemote();
		if (remote == null) return true;
		return !remote.equals(remoteResource);
	}
    
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.SyncInfoSetChangeSetCollector#waitUntilDone(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void waitUntilDone(IProgressMonitor monitor) {
        super.waitUntilDone(monitor);
		monitor.worked(1);
		// wait for the event handler to process changes.
        LogEntryCacheUpdateHandler handler = getLogEntryHandler();
        if (handler != null) {
			while(handler.getEventHandlerJob().getState() != Job.NONE) {
				monitor.worked(1);
				try {
					Thread.sleep(10);		
				} catch (InterruptedException e) {
				}
				Policy.checkCanceled(monitor);
			}
        }
		monitor.worked(1);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.subscriber.LogEntryCacheUpdateHandler.ILogsFetchedListener#logEntriesFetched(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void logEntriesFetched(SyncInfoSet set, LogEntryCache logEntryCache, IProgressMonitor monitor) {
        if (disposed) return;
        // Hold on to the cache so we can use it while commit sets are visible
        this.logEntryCache = logEntryCache;
        handleRemoteChanges(set.getSyncInfos(), logEntryCache, monitor);
    }

    public ICVSRemoteFile getImmediatePredecessor(ICVSRemoteFile file) throws TeamException {
        if (logEntryCache != null)
            return logEntryCache.getImmediatePredecessor(file);
        return null;
    }
}
