package org.openhab.binding.innogysmarthome.internal.listener;

import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.event.Event;

public interface DeviceStatusListener {

    /**
     * This method is called whenever the state of the given device has changed.
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
     * This method us called whenever a device is removed.
     *
     * @param device
     *            The device which is removed.
     */
    public void onDeviceRemoved(Device device);

    /**
     * This method us called whenever a device is added.
     *
     * @param device
     *            The device which is added.
     */
    public void onDeviceAdded(Device device);

    /**
     * This method us called whenever a device config is updated.
     *
     * @param device
     *            The device which config is changed.
     * @param bridge
     *            The innogy SmartHome bridge the device was connected to.
     */
    // public void onDeviceConfigUpdate(Device device);
}
