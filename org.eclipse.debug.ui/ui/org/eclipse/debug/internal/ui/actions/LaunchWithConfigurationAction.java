package org.eclipse.debug.internal.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * A cascading sub-menu that shows all launch configuration types pertinent to this action's mode
 * (e.g., 'run' or 'debug').
 */
public abstract class LaunchWithConfigurationAction extends Action implements IMenuCreator, 
																				   IWorkbenchWindowActionDelegate {
	
	private IWorkbenchWindow fWorkbenchWindow;
	private List fActionItems;
	private IAction fAction;
	private ILaunchManager fLaunchManager;
	
	/**
	 * @see IAction#run()
	 */
	public void run() {
		//do nothing 
		//this action just creates a cascading menu.
	}
	
	public LaunchWithConfigurationAction() {
		super();
		setText(ActionMessages.getString("LaunchWithConfigurationAction.New_Configuration_1")); //$NON-NLS-1$
		setMenuCreator(this);
	}
	
	private void createMenuForAction(Menu parent, Action action, int count) {
		StringBuffer label= new StringBuffer();
		//add the numerical accelerator
		if (count < 10) {
			label.append('&');
			label.append(count);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
		if (getActionItems() != null) {
			//have to do our own menu updating
			getActionItems().add(item);
		}
	}
	
	/**
	 * @see IMenuCreator#dispose()
	 */
	public void dispose() {
		setActionItems(null);
	}
	
	/**
	 * @see IMenuCreator#getMenu(Control)
	 */
	public Menu getMenu(Control parent) {
		return null;
	}
	
	/**
	 * @see IMenuCreator#getMenu(Menu)
	 */
	public Menu getMenu(Menu parent) {
		String activePerspID = getActivePerspectiveID();
		Map shortcutMap = DebugUIPlugin.getDefault().getLaunchConfigurationShortcuts();
	
		// Look through the shortcut map and for each config type, see if the active perspective's
		// ID is listed.  If it is, add a shortcut for creating a new configuration of that type
		// to the menu
		Menu menu= new Menu(parent);
		int menuCount= 1;
		Iterator keyIterator = shortcutMap.keySet().iterator();
		while (keyIterator.hasNext()) {
			String configTypeID = (String) keyIterator.next();
			List perspList = (List) shortcutMap.get(configTypeID);
			if ((perspList.contains(activePerspID)) || (perspList.contains("*"))) {  //$NON-NLS-1$
				ILaunchConfigurationType configType = getLaunchManager().getLaunchConfigurationType(configTypeID);
				if (populateMenu(configType, menu, menuCount)) {
					menuCount++;
				}
			}
		}
		
		// If NO configuration types listed the current perspective, add ALL configuration types
		// to the menu.  This is to avoid an empty cascading menu.
		if (menuCount == 1) {
			ILaunchConfigurationType[] allConfigTypes = getLaunchManager().getLaunchConfigurationTypes();
			for (int i = 0; i < allConfigTypes.length; i++) {
				ILaunchConfigurationType configType = allConfigTypes[i];
				if (populateMenu(configType, menu, menuCount)) {
					menuCount++;
				}
			}
		}
		
		return menu;
	}
	
	/**
	 * If the specified configuration type supports the current mode (run or debug), create a
	 * shortcut action and add it to the specified menu.
	 */
	protected boolean populateMenu(ILaunchConfigurationType configType, Menu menu, int menuCount) {
		if (configType.supportsMode(getMode())) {
			OpenLaunchConfigurationsAction openAction = null;
			if (getMode().equals(ILaunchManager.DEBUG_MODE)) {
				openAction = new OpenDebugConfigurations(configType);
			} else {
				openAction = new OpenRunConfigurations(configType);
			}
			createMenuForAction(menu, openAction, menuCount);
			return true;
		} 
		return false;
	}
	
	protected String getActivePerspectiveID() {
		return DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
	}
	
	/**
	 * Determines and returns the selected element that provides context for the launch,
	 * or <code>null</code> if there is no selection.
	 */
	protected Object resolveSelectedElement(IWorkbenchWindow window) {
		if (window == null) {
			return null;
		}
		ISelection selection= window.getSelectionService().getSelection();
		if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
			// there is no obvious selection - go fishing
			selection= null;
			IWorkbenchPage p= window.getActivePage();
			if (p == null) {
				//workspace is closed
				return null;
			}
			IEditorPart editor= p.getActiveEditor();
			Object element= null;
			// first, see if there is an active editor, and try its input element
			if (editor != null) {
				element= editor.getEditorInput();
			}
			return element;
		}
		return ((IStructuredSelection)selection).getFirstElement();
	}
	
	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		setWorkbenchWindow(window);
		setActionItems(new ArrayList(5));
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		run();
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (action instanceof Action) {
			if (fAction == null) {
				((Action)action).setMenuCreator(this);
				fAction = action;				
			}
		} else {
			action.setEnabled(false);
		}
	}
	
	protected List getActionItems() {
		return fActionItems;
	}

	protected void setActionItems(List actionItems) {
		fActionItems = actionItems;
	}
	
	protected void setWorkbenchWindow(IWorkbenchWindow window) {
		fWorkbenchWindow = window;
	}
	
	protected IWorkbenchWindow getWorkbenchWindow() {
		return fWorkbenchWindow;
	}
	
	/**
	 * Lazily populate & return the current launch manager.  Lazy population is useful because this
	 * can be called many times.
	 */
	protected ILaunchManager getLaunchManager() {
		if (fLaunchManager == null) {
			fLaunchManager = DebugPlugin.getDefault().getLaunchManager();
		}
		return fLaunchManager;
	}
	
	/**
	 * Implemented to return one of the constants defined in <code>ILaunchManager</code>
	 * that specifies the launch mode. 
	 */
	public abstract String getMode();
}
