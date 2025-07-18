/*******************************************************************************
 * Copyright (c) 2006, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 *******************************************************************************/
package org.eclipse.terminal.control;

import org.eclipse.terminal.connector.TerminalState;

/**
 * Provided by a view implementation.
 *
 */
public interface ITerminalListener {
	/**
	 * Called when the state of the connection has changed.
	 * @param state
	 */
	void setState(TerminalState state);

	/**
	 * selection has been changed internally e.g. select all
	 * clients might want to react on that
	 * NOTE: this does not include mouse selections
	 * those are handled in separate MouseListeners
	 * TODO should be unified
	 */
	void setTerminalSelectionChanged();

	/**
	 * Set the title of the terminal.
	 *
	 * @param title Terminal title.
	 * @param requestor Item that requests terminal title update.
	 */
	void setTerminalTitle(String title, TerminalTitleRequestor requestor);
}
