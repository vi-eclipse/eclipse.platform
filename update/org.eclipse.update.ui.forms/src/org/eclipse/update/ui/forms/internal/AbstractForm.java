/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.ui.forms.internal;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.util.*;
import org.eclipse.ui.actions.*;

/**
 * This class implements IForm interface and
 * provides some common services to its subclasses.
 * It handles some common properties like heading
 * text, image and colors, as well as
 * font change processing.
 */

public abstract class AbstractForm implements IForm, IPropertyChangeListener {
	protected FormWidgetFactory factory;
	protected Color headingBackground;
	protected Color headingForeground;
	protected boolean headingVisible = true;
	protected Image headingImage;
	protected String headingText;
	protected Font titleFont;
	private IPropertyChangeListener hyperlinkColorListener;

	public AbstractForm() {
		factory = new FormWidgetFactory();
		titleFont = JFaceResources.getHeaderFont();
		JFaceResources.getFontRegistry().addListener(this);
		IPreferenceStore pstore = JFacePreferences.getPreferenceStore();
		hyperlinkColorListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getProperty().equals(JFacePreferences.HYPERLINK_COLOR)
					|| e.getProperty().equals(
						JFacePreferences.ACTIVE_HYPERLINK_COLOR)) {
					updateHyperlinkColors();
				}
			}
		};
		pstore.addPropertyChangeListener(hyperlinkColorListener);
	}
	
	protected void updateHyperlinkColors() {
		factory.updateHyperlinkColors();
	}

	/**
	 * @see IForm#commitChanges(boolean)
	 */
	public void commitChanges(boolean onSave) {
	}

	/**
	 * @see IForm#createControl(Composite)
	 */
	public abstract Control createControl(Composite parent);

	/**
	 * @see IForm#dispose()
	 */
	public void dispose() {
		factory.dispose();
		JFaceResources.getFontRegistry().removeListener(this);
		IPreferenceStore pstore = JFacePreferences.getPreferenceStore();
		pstore.removePropertyChangeListener(this);
		pstore.removePropertyChangeListener(hyperlinkColorListener);
	}

	/**
	 * @see IForm#doGlobalAction(String)
	 */
	public boolean doGlobalAction(String actionId) {
		return false;
	}

	/**
	 * @see IForm#expandTo(Object)
	 */
	public void expandTo(Object object) {
	}

	/**
	 * @see IForm#getControl()
	 */
	public abstract Control getControl();

	/**
	 * @see IForm#getFactory()
	 */
	public FormWidgetFactory getFactory() {
		return factory;
	}

	/**
	 * @see IForm#getHeadingBackground()
	 */
	public Color getHeadingBackground() {
		return headingBackground;
	}

	/**
	 * @see IForm#getHeadingForeground()
	 */
	public Color getHeadingForeground() {
		return headingForeground;
	}

	/**
	 * @see IForm#getHeadingImage()
	 */
	public Image getHeadingImage() {
		return headingImage;
	}

	/**
	 * @see IForm#getHeading()
	 */
	public String getHeadingText() {
		if (headingText == null)
			return "";
		return headingText;
	}

	/**
	 * @see IForm#initialize(Object)
	 */
	public void initialize(Object model) {
	}

	/**
	 * @see IForm#isHeadingVisible()
	 */
	public boolean isHeadingVisible() {
		return headingVisible;
	}

	/**
	 * @see IForm#registerSection(FormSection)
	 */
	public void registerSection(FormSection section) {
	}

	/**
	 * @see IForm#setFocus()
	 */
	public void setFocus() {
	}

	/**
	 * @see IForm#setHeadingBackground(Color)
	 */
	public void setHeadingBackground(Color newHeadingBackground) {
		this.headingBackground = newHeadingBackground;
	}

	/**
	 * @see IForm#setHeadingForeground(Color)
	 */
	public void setHeadingForeground(Color newHeadingForeground) {
		this.headingForeground = newHeadingForeground;
	}

	/**
	 * @see IForm#setHeadingImage(Image)
	 */
	public void setHeadingImage(Image headingImage) {
		this.headingImage = headingImage;
	}

	/**
	 * @see IForm#setHeadingVisible(boolean)
	 */
	public void setHeadingVisible(boolean newHeadingVisible) {
		this.headingVisible = newHeadingVisible;
	}

	/**
	 * @see IForm#setHeading(String)
	 */
	public void setHeadingText(String headingText) {
		this.headingText = headingText;
	}

	/**
	 * @see IForm#update()
	 */
	public void update() {
	}

	protected boolean canPerformDirectly(String id, Control control) {
		if (control instanceof Text) {
			Text text = (Text) control;
			if (id.equals(ActionFactory.CUT.getId())) {
				text.cut();
				return true;
			}
			if (id.equals(ActionFactory.COPY.getId())) {
				text.copy();
				return true;
			}
			if (id.equals(ActionFactory.PASTE.getId())) {
				text.paste();
				return true;
			}
			if (id.equals(ActionFactory.SELECT_ALL.getId())) {
				text.selectAll();
				return true;
			}
			if (id.equals(ActionFactory.DELETE.getId())) {
				int count = text.getSelectionCount();
				if (count == 0) {
					int caretPos = text.getCaretPosition();
					text.setSelection(caretPos, caretPos + 1);
				}
				text.insert("");
				return true;
			}
		}
		return false;
	}
}
