/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.internal.listener;

import org.openhab.binding.innogysmarthome.handler.InnogyBridgeHandler;
import org.openhab.binding.innogysmarthome.internal.InnogyWebSocket;

/**
 * The {@link EventListener} is called by the {@link InnogyWebSocket} on new Events and by the
 * {@link InnogyBridgeHandler}, if the {@link InnogyWebSocket} stopped.
 *
 * @author Oliver Kuhl - Initial contribution
 */
public interface EventListener {

    /**
     * This method is called, whenever a new event comes from the innogy service (like a device change for example).
     *
     * @param msg
     */
    public void onEvent(String msg);

    /**
     * This method is called, whenever the eventRunner stops and must be restarted immediately.
     */
    public void onEventRunnerStopped();

    /**
     * This method is called, when the eventRunner stops and must be restarted after the given delay in seconds.
     *
     * @param delay long in seconds
     */
    public void onEventRunnerStopped(long delay);
}
