/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.examples.xml;

import java.util.ArrayList;

import org.eclipse.compare.examples.xml.ui.StatusDialog;
import org.eclipse.compare.examples.xml.ui.StatusInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class is used to add or edit a particular ID Mapping
 */
public class XMLCompareEditOrderedDialog extends StatusDialog {
	
	private Mapping fMapping;
	private ArrayList fIdmapAL;
	private boolean fEdit;
	
	private Text fElementText;
	private Text fSignatureText;

	/*
	 * Constructs a new edit mapping dialog.
	 */		
	public XMLCompareEditOrderedDialog(Shell parent, Mapping mapping, ArrayList idmapAL, boolean edit) {
		super(parent);
	
		int shellStyle= getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);

	
		fEdit= edit;
		if (fEdit)
			setTitle(XMLCompareMessages.getString("XMLCompareEditOrderedDialog.editTitle")); //$NON-NLS-1$
		else
			setTitle(XMLCompareMessages.getString("XMLCompareEditOrderedDialog.newTitle")); //$NON-NLS-1$

		fMapping= mapping;
		fIdmapAL= idmapAL;
	}
	
	/**
	 * Creates and returns the contents of the upper part 
	 * of the dialog (above the button bar).
	 *
	 * Subclasses should override.
	 *
	 * @param ancestor the parent composite to contain the dialog area
	 * @return the dialog area control
	 */	
	protected Control createDialogArea(Composite ancestor) {
		Composite composite= (Composite) super.createDialogArea(ancestor);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		//Element
		Label label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.getString("XMLCompareEditMappingDialog.element")); //$NON-NLS-1$
		label.setLayoutData(new GridData());

		fElementText= new Text(inner, SWT.BORDER);
		fElementText.setText(fMapping.getElement());
		fElementText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fElementText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e){
				doValidation();
			}
		});

		//Signature
		label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.getString("XMLCompareEditMappingDialog.signature")); //$NON-NLS-1$
		label.setLayoutData(new GridData());

		fSignatureText= new Text(inner, SWT.BORDER);
		fSignatureText.setText(fMapping.getSignature());
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(50);
		fSignatureText.setLayoutData(data);
		fSignatureText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e){
				doValidation();
			}
		});
		
		fElementText.setFocus();

		return composite;
	}
	
	/**
	 * Validate user input
	 */	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String text= fElementText.getText();
		String mappingKey= Mapping.getKey(fSignatureText.getText(), text);
		String errormsg= ""; //$NON-NLS-1$
		boolean isError= false;
		if (text.length() == 0) {
			errormsg= XMLCompareMessages.getString("XMLCompareEditMappingDialog.error.noname"); //$NON-NLS-1$
			isError= true;
		} else if (XMLComparePreferencePage.containsInvalidCharacters(text)) {
			if (errormsg == "") errormsg= XMLCompareMessages.getString("XMLCompareEditMappingDialog.error.invalidname"); //$NON-NLS-2$ //$NON-NLS-1$
			isError= true;
		} else if (!fEdit && fIdmapAL.contains(mappingKey)) {
			if (errormsg == "") errormsg= XMLCompareMessages.getString("XMLCompareEditOrderedDialog.error.orderedExists"); //$NON-NLS-1$ //$NON-NLS-2$
			isError= true;
		}
		text= fSignatureText.getText();
		if (XMLComparePreferencePage.containsInvalidCharacters(text)) {
			if (errormsg == "") errormsg= XMLCompareMessages.getString("XMLCompareEditMappingDialog.error.invalidsignature"); //$NON-NLS-2$ //$NON-NLS-1$
			isError= true;
		}
		if (isError) status.setError(errormsg);
		updateStatus(status);
	}
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 */	
	protected void okPressed() {
		fMapping.setElement(fElementText.getText());
		fMapping.setSignature(fSignatureText.getText());
		super.okPressed();
	}
}
