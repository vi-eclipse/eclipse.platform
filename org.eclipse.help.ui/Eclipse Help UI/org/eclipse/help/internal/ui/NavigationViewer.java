package org.eclipse.help.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.help.internal.contributions.InfoSet;
import org.eclipse.help.internal.navigation.*;
import org.eclipse.help.internal.contributors.*;
import org.eclipse.help.internal.HelpSystem;

/**
 * Navigation Viewer.  Contains combo for InfoSet selection and Workbook for display
 * of views.
 */
public class NavigationViewer implements ISelectionProvider {
	private Composite contents;
	private NavigationWorkbook workbook;

	private ArrayList infoSetIds;
	private Combo infoSetsCombo;

	private InfoSet currentInfoset;

	/**
	 * NavigationViewer constructor comment.
	 */
	public NavigationViewer(Composite parent) {
		super();

		// Create a list of available Info Sets
		infoSetIds = new ArrayList();
		infoSetIds.addAll(HelpSystem.getNavigationManager().getInfoSetIds());

		createControl(parent);
	}
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		workbook.addSelectionChangedListener(listener);
	}
	/**
	 */
	protected Control createControl(Composite parent) {
		contents = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridData gd = new GridData();
		gd.horizontalAlignment = gd.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = gd.BEGINNING;
		gd.grabExcessVerticalSpace = false;

		// Create combo for selection of Info Sets
		if (infoSetIds.size() > 1) {
			infoSetsCombo =
				new Combo(contents, SWT.DROP_DOWN | SWT.READ_ONLY /*| SWT.FLAT*/);
			infoSetsCombo.setLayoutData(gd);
			infoSetsCombo.setBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			HelpNavigationManager navManager = HelpSystem.getNavigationManager();
			for (int i = 0; i < infoSetIds.size(); i++) {
				infoSetsCombo.add(navManager.getInfoSetLabel((String) infoSetIds.get(i)));
			}
			infoSetsCombo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					int index = ((Combo) e.widget).getSelectionIndex();
					final String id = (String) infoSetIds.get(index);
					// Switching to another infoset may be time consuming
					// so display the busy cursor
					BusyIndicator.showWhile(null, new Runnable()
					{
						public void run()
						{
							try
							{
								InfoSet selectedInfoset = HelpSystem.getNavigationManager().getInfoSet(id);
								if(currentInfoset != selectedInfoset)
									setInput(selectedInfoset);
							}catch(Exception e){}
						}
					});
				}
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		}

		gd = new GridData();
		gd.horizontalAlignment = gd.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = gd.FILL;
		gd.grabExcessVerticalSpace = true;

		workbook = new NavigationWorkbook(contents);
		workbook.getControl().setLayoutData(gd);

		WorkbenchHelp.setHelp(
			contents,
			new String[] {
				IHelpUIConstants.NAVIGATION_VIEWER,
				IHelpUIConstants.EMBEDDED_HELP_VIEW});
		return contents;
	}
	public Control getControl() {
		return contents;
	}
	public Object getInput() {
		return currentInfoset;
	}
	/**
	 * Returns the current selection for this provider.
	 * 
	 * @return the current selection
	 */
	public ISelection getSelection() {
		return workbook.getSelection();
	}
	/**
	 * Removes the given selection change listener from this selection provider.
	 * Has no affect if an identical listener is not registered.
	 *
	 * @param listener a selection changed listener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		workbook.removeSelectionChangedListener(listener);
	}
	/**
	 * @param input an InfoSet or Contribution[]
	 *  if Infoset, then Pages will be created containing trees representations
	 *   of Infoset's children (InfoViews)
	 *  If Array of Contribution, the elements can be either InfoView or InfoSet.
	 *   Pages will be created containing trees representations
	 *   of InfoView element and each of Infoset's children (InfoViews)
	 */
	public void setInput(Object input) {
		if (input instanceof InfoSet) {
			currentInfoset = (InfoSet) input;

			// set global infoset and navigation model
			HelpSystem.getNavigationManager().setCurrentInfoSet(currentInfoset.getID());

			// If more than 1 infoset, then select it from the combo box
			if (infoSetIds.size() > 1) {
				int index = infoSetIds.indexOf(currentInfoset.getID());
				if (index != -1)
					infoSetsCombo.select(index);

				// remove selection, so it is gray not blue;
				infoSetsCombo.clearSelection();
			}

			// show this infoset    
			workbook.display(currentInfoset);

			// update htmlViewer
			setSelection(new StructuredSelection(currentInfoset));
		}else{
			workbook.display(input);
		}
	}
	/**
	 * Sets the selection current selection for this selection provider.
	 *
	 * @param selection the new selection
	 */
	public void setSelection(ISelection selection) {
		workbook.setSelection(selection);
	}
}

