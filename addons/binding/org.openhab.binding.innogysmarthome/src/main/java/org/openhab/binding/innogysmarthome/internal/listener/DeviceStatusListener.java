/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.internal.listener;

import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.event.Event;

/**
 * The {@link DeviceStatusListener} is called, when {@link Device}s are added, removed or changed.
 *
 * @author Oliver Kuhl - Initial contribution
 */
public interface DeviceStatusListener {

    /**
     * This method is called whenever the state of the given {@link Device} has changed.
     *
     * @param device
     *            The device which received the state update.
     */
    public void onDeviceStateChanged(Device device);

    /**
     * This method is called whenever the state of a {@link Device} is changed by the given {@link Event}.
     *
     * @param device
     * @param event
     *
     */
    public void onDeviceStateChanged(Device device, Event event);

    /**
     * This method is called whenever a {@link Device} is removed.
     *
     * @param device
     *            The device which is removed.
     */
    public void onDeviceRemoved(Device device);

    /**
     * This method is called whenever a {@link Device} is added.
     *
     * @param device
     *            The device which is added.
     */
    public void onDeviceAdded(Device device);
}
