/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.internal.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.innogysmarthome.InnogyBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ollie.innogysmarthome.InnogyClient;
import in.ollie.innogysmarthome.entity.Location;
import in.ollie.innogysmarthome.entity.Message;
import in.ollie.innogysmarthome.entity.Property;
import in.ollie.innogysmarthome.entity.capability.Capability;
import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.link.CapabilityLink;
import in.ollie.innogysmarthome.exception.ApiException;

/**
 * Manages the structure of the {@link Device}s and the calls to the {@Link InnogyClient} to load the {@link Device}
 * data from the innogy SmartHome web service.
 *
 * @author Oliver Kuhl - Initial contribution
 *
 */
public class DeviceStructureManager {

    private Logger logger = LoggerFactory.getLogger(DeviceStructureManager.class);

    private InnogyClient client;
    private Map<String, Device> deviceMap;
    private Map<String, Device> capabilityToDeviceMap;
    private Map<String, Location> locationMap;
    private String bridgeDeviceId;

    /**
     * Constructs the {@link DeviceStructureManager}.
     *
     * @param client the {@link InnogyClient}
     */
    public DeviceStructureManager(InnogyClient client) {
        this.client = client;
    }

    /**
     * Starts the {@link DeviceStructureManager} by building the device structure.
     *
     * @throws IOException
     * @throws ApiException
     */
    public synchronized void start() throws IOException, ApiException {
        logger.debug("Starting device structure manager.");

        refreshDevices();
        logger.info("Devices loaded. Device structure manager ready.");
    }

    /**
     * Returns the {@link #deviceMap}, a map with the device id and the device.
     *
     * @return
     */
    public Map<String, Device> getDeviceMap() {
        if (deviceMap == null) {
            deviceMap = Collections.synchronizedMap(new HashMap<String, Device>());
        }
        return deviceMap;

    }

    /**
     * Loads all device data from the bridge and stores the {@link Device}s and their states in the
     * {@link DeviceStructureManager}.
     *
     * @throws IOException
     * @throws ApiException
     */
    public void refreshDevices() throws IOException, ApiException {
        List<Device> devices = client.getFullDevices();
        for (Device d : devices) {
            if (InnogyBindingConstants.SUPPORTED_DEVICES.contains(d.getType())) {
                addDeviceToStructure(d);
            } else {
                logger.debug("Device {}:{} ({}) ignored - UNSUPPORTED.", d.getType(), d.getName(), d.getId());
                logger.debug("====================================");
                continue;
            }

            if (d.isController()) {
                bridgeDeviceId = d.getId();
            }
            try {
                logger.debug("Device {}:{} ({}) loaded.", d.getType(), d.getName(), d.getId());
                for (Capability c : d.getCapabilityMap().values()) {
                    logger.debug("> CAP: {} ({})", c.getName(), c.getId());
                    for (Property p : c.getCapabilityState().getStateMap().values()) {
                        logger.debug(">> CAP-State: {} -> {}", p.getName(), p.getValue());
                    }
                }
            } catch (NullPointerException e) {
                logger.warn("NPEX.");
            } catch (Exception e) {
                logger.error("EX: ", e);
            }
            logger.debug("====================================");

        }
    }

    /**
     * Refreshs the {@link Device} with the given id and stores it in the {@link DeviceStructureManager}.
     *
     * @param deviceId
     * @throws IOException
     * @throws ApiException
     */
    public void refreshDevice(String deviceId) throws IOException, ApiException {
        Device d = client.getFullDeviceById(deviceId);
        if (InnogyBindingConstants.SUPPORTED_DEVICES.contains(d.getType())) {
            addDeviceToStructure(d);
        } else {
            logger.debug("Device {}:{} ({}) ignored - UNSUPPORTED.", d.getType(), d.getName(), d.getId());
            return;
        }
        if (d.isController()) {
            bridgeDeviceId = d.getId();
        }
        try {
            logger.debug("Device {} ({}) loaded.", d.getName(), d.getId());
            for (Capability c : d.getCapabilityMap().values()) {
                logger.debug("> CAP: {} ({})", c.getName(), c.getId());
                for (Property p : c.getCapabilityState().getStateMap().values()) {
                    logger.debug(">> CAP-State: {} -> {}", p.getName(), p.getValue());
                }
            }
        } catch (NullPointerException e) {
            logger.warn("NPEX.");
        } catch (Exception e) {
            logger.error("EX: ", e);
        }
    }

    /**
     * Adds the {@link Device} to the structure.
     *
     * @param device
     */
    public void addDeviceToStructure(Device device) {

        if (device.getId() != null) {
            getDeviceMap().put(device.getId(), device);
        }

        if (capabilityToDeviceMap == null) {
            capabilityToDeviceMap = Collections.synchronizedMap(new HashMap<String, Device>());
        }

        for (CapabilityLink cl : device.getCapabilityLinkList()) {
            capabilityToDeviceMap.put(cl.getValue(), device);
        }
    }

    /**
     * Returns the {@link Device} with the given id.
     *
     * @param id
     * @return
     */
    public Device getDeviceById(String id) {
        logger.debug("getDeviceById {}:{}", id, getDeviceMap().containsKey(id));
        return getDeviceMap().get(id);
    }

    /**
     * Returns the {@link Device}, that provides the given capability.
     *
     * @param capabilityLink
     * @return {@link Device} or null
     */
    public Device getDeviceByCapabilityLink(String capabilityLink) {
        return capabilityToDeviceMap.get(capabilityLink);
    }

    /**
     * Returns the {@link Device} with the given deviceLink.
     *
     * @param deviceLink
     * @return {@link Device} or null
     */
    public Device getDeviceByDeviceLink(String deviceLink) {
        return getDeviceById(deviceLink.replace("/device/", ""));
    }

    /**
     * Returns the bridge {@link Device}.
     *
     * @return
     */
    public Device getBridgeDevice() {
        return getDeviceMap().get(bridgeDeviceId);
    }

    @Deprecated
    public Map<String, Device> getDeviceHashMapReference() {
        return getDeviceMap();
    }

    public List<Device> getDeviceList() {
        return new ArrayList<>(getDeviceMap().values());
    }

    /**
     * Returns the {@link Device}, that has the {@link Message} with the given messageId.
     *
     * @param messageId the id of the {@link Message}
     * @return the {@link Device} or null if none found
     */
    public Device getDeviceWithMessageId(String messageId) {
        for (Device d : getDeviceMap().values()) {
            if (d.hasMessages()) {
                for (Message m : d.getMessageList()) {
                    if (messageId.equals(m.getId())) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    public String getCapabilityId(String deviceId, String capabilityType) {
        Device device = getDeviceMap().get(deviceId);
        for (Capability c : device.getCapabilityMap().values()) {
            if (c.getType().equals(capabilityType)) {
                return c.getId();
            }
        }
        return null;
    }

    public List<CapabilityLink> getCapabilityLinkListForDeviceId(String deviceId) {
        Device device = getDeviceMap().get(deviceId);
        if (device != null) {
            return device.getCapabilityLinkList();
        }
        return null;
    }

    /**
     * Adds the {@link Location} to the structure.
     *
     * @param location
     */
    public void addLocationToStructure(Location location) {
        if (locationMap == null) {
            locationMap = Collections.synchronizedMap(new HashMap<String, Location>());
        }

        if (location.getId() != null) {
            locationMap.put(location.getId(), location);
        }
    }

    /**
     * Returns the {@link Location} with the given id.
     *
     * @param id
     * @return
     */
    public Location getLocationById(String id) {
        return locationMap.get(id);
    }

}
