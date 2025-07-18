/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Michael Scharf (Wind River) - [200541] Extract from TerminalConnectorExtension.TerminalConnectorProxy
 * Martin Oberhuber (Wind River) - [225853][api] Provide more default functionality in TerminalConnectorImpl
 * Uwe Stieber (Wind River) - [282996] [terminal][api] Add "hidden" attribute to terminal connector extension point
 *******************************************************************************/
package org.eclipse.terminal.internal.connector;

import java.io.OutputStream;
import java.util.Optional;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.terminal.connector.ISettingsStore;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.connector.Logger;
import org.eclipse.terminal.connector.TerminalConnectorExtension;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.internal.control.impl.TerminalMessages;

/**
 * An {@link ITerminalConnector} instance, also known as terminal connection
 * type, for maintaining a single terminal connection.
 *
 * It provides all terminal connector functions that can be provided by static
 * markup without loading the actual implementation class. The actual
 * {@link AbstractTerminalConnector} implementation class is lazily loaded by the
 * provided {@link TerminalConnector.Factory} interface when needed. class, and
 * delegates to the actual implementation when needed. The following methods can
 * be called without initializing the contributed implementation class:
 * {@link #getId()}, {@link #getName()}, {@link #getSettingsSummary()},{@link #load(ISettings)},
 * {@link #setTerminalSize(int, int)}, {@link #save(ISettings)},
 * {@link #getAdapter(Class)}
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *                Clients can get terminal connector instances through the
 *                {@link TerminalConnectorExtension} class.
 */
public class TerminalConnector implements ITerminalConnector {
	/**
	 * Creates an instance of {@link AbstractTerminalConnector}. This is used to lazily load
	 * classed defined in extensions.
	 *
	 */
	public interface Factory {
		/**
		 * Factory method to create the actual terminal connector implementation
		 * when needed.
		 *
		 * @return a Connector
		 * @throws Exception
		 */
		AbstractTerminalConnector makeConnector() throws Exception;
	}

	/**
	 * The factory for creating impl instances.
	 */
	private final TerminalConnector.Factory fTerminalConnectorFactory;
	/**
	 * The (display) name of the TerminalConnector
	 */
	private final String fName;
	/**
	 * The unique id the connector
	 */
	private final String fId;
	/**
	 * Flag to mark the connector as hidden.
	 */
	private final boolean fHidden;
	/**
	 * The connector
	 */
	private AbstractTerminalConnector fConnector;
	/**
	 * If the initialization of the class specified in the extension fails,
	 * this variable contains the error
	 */
	private Exception fException;
	/**
	 * The store might be set before the real connector is initialized.
	 * This keeps the value until the connector is created.
	 */
	private ISettingsStore fStore;

	/**
	 * Constructor for the terminal connector.
	 *
	 * @param terminalConnectorFactory Factory for lazily instantiating the
	 *            {@link AbstractTerminalConnector} when needed.
	 * @param id terminal connector ID. The connector is publicly known under
	 *            this ID.
	 * @param name translatable name to display the connector in the UI.
	 */
	public TerminalConnector(TerminalConnector.Factory terminalConnectorFactory, String id, String name,
			boolean hidden) {
		fTerminalConnectorFactory = terminalConnectorFactory;
		fId = id;
		fName = name;
		fHidden = hidden;
	}

	@Override
	public String getInitializationErrorMessage() {
		getConnectorImpl();
		if (fException != null) {
			return fException.getLocalizedMessage();
		}
		return null;
	}

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public boolean isHidden() {
		return fHidden;
	}

	private AbstractTerminalConnector getConnectorImpl() {
		if (!isInitialized()) {
			try {
				fConnector = fTerminalConnectorFactory.makeConnector();
				fConnector.initialize();
			} catch (Exception e) {
				fException = e;
				fConnector = new AbstractTerminalConnector() {
					@Override
					public void connect(ITerminalControl control) {
						// super.connect(control);
						control.setState(TerminalState.CLOSED);
						control.setMsg(getInitializationErrorMessage());
					}

					@Override
					public OutputStream getTerminalToRemoteStream() {
						return null;
					}

					@Override
					public String getSettingsSummary() {
						return null;
					}
				};
				// that's the place where we log the exception
				Logger.logException(e);
			}
			if (fConnector != null && fStore != null) {
				fConnector.load(fStore);
			}
		}
		return fConnector;
	}

	@Override
	public boolean isInitialized() {
		return fConnector != null || fException != null;
	}

	@Override
	public void connect(ITerminalControl control) {
		getConnectorImpl().connect(control);
	}

	@Override
	public void disconnect() {
		getConnectorImpl().disconnect();
	}

	@Override
	public OutputStream getTerminalToRemoteStream() {
		return getConnectorImpl().getTerminalToRemoteStream();
	}

	@Override
	public String getSettingsSummary() {
		if (fConnector != null) {
			return getConnectorImpl().getSettingsSummary();
		} else {
			return TerminalMessages.NotInitialized;
		}
	}

	@Override
	public boolean isLocalEcho() {
		return getConnectorImpl().isLocalEcho();
	}

	@Override
	public void load(ISettingsStore store) {
		if (fConnector == null) {
			fStore = store;
		} else {
			getConnectorImpl().load(store);
		}
	}

	@Override
	public void setDefaultSettings() {
		getConnectorImpl().setDefaultSettings();
	}

	@Override
	public void save(ISettingsStore store) {
		// no need to save the settings: it cannot have changed
		// because we are not initialized....
		if (fConnector != null) {
			getConnectorImpl().save(store);
		}
	}

	@Override
	public void setTerminalSize(int newWidth, int newHeight) {
		// we assume that setTerminalSize is called also after
		// the terminal has been initialized. Else we would have to cache
		// the values....
		if (fConnector != null) {
			fConnector.setTerminalSize(newWidth, newHeight);
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		AbstractTerminalConnector connector = null;
		if (isInitialized()) {
			connector = getConnectorImpl();
		}
		// if we cannot create the connector then we cannot adapt...
		if (connector != null) {
			// maybe the connector is adaptable
			if (connector instanceof IAdaptable) {
				Object result = ((IAdaptable) connector).getAdapter(adapter);
				// Not sure if the next block is needed....
				if (result == null) {
					//defer to the platform
					result = Platform.getAdapterManager().getAdapter(connector, adapter);
				}
				if (result != null) {
					return adapter.cast(result);
				}
			}
			// maybe the real adapter is what we need....
			if (adapter.isInstance(connector)) {
				return adapter.cast(connector);
			}
		}
		// maybe we have to be adapted....
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public Optional<String> getWorkingDirectory() {
		if (fConnector != null) {
			return fConnector.getWorkingDirectory();
		}
		return Optional.empty();
	}
}