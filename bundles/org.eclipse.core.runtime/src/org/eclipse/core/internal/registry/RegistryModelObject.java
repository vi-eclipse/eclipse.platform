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
package org.eclipse.core.internal.registry;

import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.internal.runtime.InternalPlatform;

/**
 * An object which has the general characteristics of all elements
 * in a plug-in manifest.
 * <p>
 * This class may be subclassed.
 * </p>
 */

public abstract class RegistryModelObject {

	// DTD properties (included in plug-in manifest)
	protected String name = null;

	// transient properties (not included in plug-in manifest)
	private int flags = 0;
	// the last bit is a read-only flag
	// IMPORTANT: One bit in the "flags" integer is used to store the 
	// read-only flag and the other bits are used to store an integer value
	// which can be from -1 to (2**31) - 1. To help with the bit masking, the integer
	// value stored in "flags" is (value + 1). This means that a "flags" value
	// of 0 will NOT be marked as read-only and will return -1 for the start line value.
	static final int M_READ_ONLY = 0x80000000;

	private RegistryModelObject parent;

	/**
	 * Checks that this model object is writeable.  A runtime exception
	 * is thrown if it is not.
	 */
	protected void assertIsWriteable() {
		Assert.isTrue(!isReadOnly(), "Model is read-only"); //$NON-NLS-1$
	}
	/**
	 * Returns the name of this element.
	 * 
	 * @return the name of this element or <code>null</code>
	 */
	public String getName() {
		return name;
	}
	/**
	 * Returns whether or not this model object is read-only.
	 * 
	 * @return <code>true</code> if this model object is read-only,
	 *		<code>false</code> otherwise
	 * @see #markReadOnly
	 */
	public boolean isReadOnly() {
		return (flags & M_READ_ONLY) == M_READ_ONLY;
	}
	/**
	 * Sets this model object and all of its descendents to be read-only.
	 * Subclasses may extend this implementation.
	 *
	 * @see #isReadOnly
	 */
	public void markReadOnly() {
		flags |= M_READ_ONLY;
	}
	/**
	 * Sets the name of this element.
	 * 
	 * @param value the new name of this element.  May be <code>null</code>.
	 */
	public void setName(String value) {
		assertIsWriteable();
		name = value;
	}
	/**
	 * Return the line number for the start tag for this plug-in object. This
	 * is the line number of the element declaration from the plug-in manifest file.
	 * 
	 * @return the line number of the start tag for this object
	 */
	public int getStartLine() {
		return (flags & ~M_READ_ONLY) - 1;
	}
	/**
	 * Set the line number for the start tag for this plug-in object. This is the
	 * line number for the element declaration from the plug-in manifest file.
	 * This value can only be set once, subsequent calls to this method will be
	 * ignored.
	 * 
	 * @param lineNumber the line number of this object's declaration in the file
	 */
	public void setStartLine(int lineNumber) {
		if (getStartLine() == -1)
			flags = (lineNumber + 1) | (flags & M_READ_ONLY);
	}
	/**
	 * Returns the plug-in model (descriptor or fragment) in which this extension is declared.
	 *
	 * @return the plug-in model in which this extension is declared
	 *  or <code>null</code>
	 */
	public Object getParent() {
		return parent;
	}
	/**
	 * Sets the plug-in model in which this extension is declared.
	 * This object must not be read-only.
	 *
	 * @param value the plug-in model in which this extension is declared.  
	 *		May be <code>null</code>.
	 */
	public void setParent(RegistryModelObject value) {
		assertIsWriteable();
		this.parent = value;
	}
	/**
	 * Return a string representation of this object. This value is not to be relied
	 * on and can change at any time. To be used for debugging purposes only.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.getClass() + ":" + getName() + "[" + super.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	public Object getAdapter(Class type) {
		return InternalPlatform.getDefault().getAdapterManager().getAdapter(this, type);
	}

	public RegistryModelObject getRegistry() {
		return parent == null ? this : parent.getRegistry();
	}
	/**
	 * Optimization to replace a non-localized key with its localized value.  Avoids having
	 * to access resource bundles for further lookups.
	 */
	public void setLocalizedName(String value) {
		name = value;
		((ExtensionRegistry) InternalPlatform.getDefault().getRegistry()).setDirty(true);
	}
}
