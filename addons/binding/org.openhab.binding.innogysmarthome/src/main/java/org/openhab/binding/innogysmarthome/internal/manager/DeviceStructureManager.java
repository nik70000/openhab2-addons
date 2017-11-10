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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.innogysmarthome.InnogyBindingConstants;
import org.openhab.binding.innogysmarthome.internal.client.InnogyClient;
import org.openhab.binding.innogysmarthome.internal.client.entity.Message;
import org.openhab.binding.innogysmarthome.internal.client.entity.Property;
import org.openhab.binding.innogysmarthome.internal.client.entity.capability.Capability;
import org.openhab.binding.innogysmarthome.internal.client.entity.device.Device;
import org.openhab.binding.innogysmarthome.internal.client.entity.link.CapabilityLink;
import org.openhab.binding.innogysmarthome.internal.client.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the structure of the {@link Device}s and the calls to the {@link InnogyClient} to load the {@link Device}
 * data from the innogy SmartHome web service.
 *
 * @author Oliver Kuhl - Initial contribution
 *
 */
public class DeviceStructureManager {

    private final Logger logger = LoggerFactory.getLogger(DeviceStructureManager.class);

    private final InnogyClient client;
    private final Map<String, Device> deviceMap;
    private final Map<String, Device> capabilityToDeviceMap;
    private String bridgeDeviceId;

    /**
     * Constructs the {@link DeviceStructureManager}.
     *
     * @param client the {@link InnogyClient}
     */
    public DeviceStructureManager(InnogyClient client) {
        this.client = client;
        deviceMap = Collections.synchronizedMap(new HashMap<String, Device>());
        capabilityToDeviceMap = Collections.synchronizedMap(new HashMap<String, Device>());
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
        writeExampleConfigToLog();
    }

    /**
     * Returns the {@link #deviceMap}, a map with the device id and the device.
     *
     * @return
     */
    public Map<String, Device> getDeviceMap() {
        return deviceMap;
    }

    /**
     * Loads all device data from the bridge and stores the {@link Device}s and their states in the
     * {@link DeviceStructureManager}.
     *
     * @throws IOException
     * @throws ApiException
     */
    private void refreshDevices() throws IOException, ApiException {
        List<Device> devices = client.getFullDevices();
        for (Device d : devices) {
            handleRefreshedDevice(d);
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
        handleRefreshedDevice(d);
    }

    /**
     * Stores the newly refreshed {@link Device} in the {@link DeviceStructureManager} structure and logs the
     * {@link Device}s details and state, if the debug logging is enabled.
     *
     * @param d the {@link Device}
     */
    private void handleRefreshedDevice(Device d) {
        if (InnogyBindingConstants.SUPPORTED_DEVICES.contains(d.getType())) {
            addDeviceToStructure(d);
        } else {
            logger.debug("Device {}:'{}' by {} ({}) ignored - UNSUPPORTED.", d.getType(), d.getName(),
                    d.getManufacturer(), d.getId());
            logger.debug("====================================");
            return;
        }

        if (d.isController()) {
            bridgeDeviceId = d.getId();
        }

        if (logger.isDebugEnabled()) {
            try {
                logger.debug("Device {}:'{}' by {} ({}) loaded.", d.getType(), d.getName(), d.getManufacturer(),
                        d.getId());
                for (Capability c : d.getCapabilityMap().values()) {
                    logger.debug("> CAP: {}/{} ({})", c.getType(), c.getName(), c.getId());
                    if (d.isRadioDevice() && !d.isReachable()) {
                        logger.debug(">> CAP-State: unknown (device NOT REACHABLE).");
                    } else {
                        if (c.getCapabilityState() != null) {
                            for (Property p : c.getCapabilityState().getStateMap().values()) {
                                logger.debug(">> CAP-State: {} -> {} ({})", p.getName(), p.getValue(),
                                        p.getLastchanged());
                            }
                        } else {
                            logger.debug(">> CAP-State: unknown (NULL)");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("EX: ", e);
            }
            logger.debug("====================================");
        }
    }

    /**
     * Writes an exemplary openHAB configuration for the found {@link Device}s to the logfile
     * which then can be copied to the config files easily for initial use of the things.
     */
    private void writeExampleConfigToLog() {
        Device bridge = getBridgeDevice();
        if (bridge == null) {
            logger.warn("Cannot create example config. No bridge found - add one first!");
            return;
        }

        String bridgeId = "SHC";
        List<String> exampleConfig = new ArrayList<String>();
        String exampleThingId;
        String exampleThingLocation;
        Map<String, String> deviceThingIdMap = new HashMap<String, String>();

        Collection<Device> devices = getDeviceList();
        for (Device d : devices) {
            if (d.isController()) {
                continue;
            }

            exampleThingId = "ngy" + d.getType() + d.getId().substring(0, 5);
            if (d.getLocation() != null) {
                exampleThingLocation = d.getLocation().getName();
            } else {
                exampleThingLocation = null;
            }
            deviceThingIdMap.put(d.getId(), exampleThingId);
            exampleConfig
                    .add(String.format("\tThing %s %s \"%s\" [ id=\"%s\" ]", d.getType(), exampleThingId,
                            d.getName() + (exampleThingLocation != null
                                    ? " (" + exampleThingLocation + ")\" @ \"" + exampleThingLocation : ""),
                            d.getId()));
        }

        Collections.sort(exampleConfig);
        exampleConfig.add(0,
                String.format(
                        "Bridge innogysmarthome:bridge:%s \"innogy SmartHome Controller\" [ refreshtoken=\"<insert-your-refresh-token-here>\" ] {",
                        bridgeId));
        exampleConfig.add("}");
        logger.info("EXAMPLE Thing configuration (copy to your 'innogy.things' config file):\n{}",
                StringUtils.join(exampleConfig, "\n"));

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

        for (CapabilityLink cl : device.getCapabilityLinkList()) {
            capabilityToDeviceMap.put(cl.getValue(), device);
        }
    }

    /**
     * Returns the {@link Device} with the given id.
     *
     * @param id
     * @return the {@link Device} or null, if it does not exist
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
     * Returns the bridge {@link Device}.
     *
     * @return
     */
    public Device getBridgeDevice() {
        return getDeviceMap().get(bridgeDeviceId);
    }

    /**
     * Returns a {@link Collection} of all {@link Device}s handled by the {@link DeviceStructureManager}.
     *
     * @return
     */
    public Collection<Device> getDeviceList() {
        return Collections.unmodifiableCollection(getDeviceMap().values());
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

    /**
     * Returns the id of the {@link Capability} for {@link Device} with the given id and the given capabilityType.
     *
     * @param deviceId
     * @param capabilityType
     * @return the id of the found {@link Capability} or null
     */
    public String getCapabilityId(String deviceId, String capabilityType) {
        Device device = getDeviceMap().get(deviceId);
        for (Capability c : device.getCapabilityMap().values()) {
            if (c.getType().equals(capabilityType)) {
                return c.getId();
            }
        }
        return null;
    }
}
