/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.variables.RemoteVariableContentManager;
import org.eclipse.debug.ui.DeferredDebugElementWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;


/**
 * Default deferred workbench adapter for a stack frame 
 */
public class DeferredStackFrame extends DeferredDebugElementWorkbenchAdapter {
    
	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parent) {
		try {
			return ((IStackFrame)parent).getVariables();
		} catch (DebugException e) {
		}
		return EMPTY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		return ((IStackFrame)element).getThread();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#fetchDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IElementCollector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
	    Object[] children = getChildren(object);
	    if (children.length > 0) {
	    	if (collector instanceof RemoteVariableContentManager.VariableCollector) {
	    		RemoteVariableContentManager.VariableCollector remoteCollector = (RemoteVariableContentManager.VariableCollector) collector;
			    for (int i = 0; i < children.length; i++) {
					IVariable child = (IVariable) children[i];
					try {
						IValue value = child.getValue();
						remoteCollector.setHasChildren(child, value.hasVariables());
					} catch (DebugException e) {
					}
				}	    	
	    	}
	        collector.add(children, monitor);
	    }
	    collector.done();
	}	

}
