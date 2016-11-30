/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.handler;

import static org.openhab.binding.innogysmarthome.InnogyBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ollie.innogysmarthome.Constants;
import in.ollie.innogysmarthome.entity.Property;
import in.ollie.innogysmarthome.entity.capability.Capability;
import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.event.Event;
import in.ollie.innogysmarthome.entity.state.CapabilityState;

/**
 * The {@link InnogyDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Oliver Kuhl - Initial contribution
 */
public class InnogyDeviceHandler extends BaseThingHandler implements DeviceStatusListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = SUPPORTED_DEVICE_THING_TYPES;
    private Logger logger = LoggerFactory.getLogger(InnogyDeviceHandler.class);

    private String deviceId;
    private Device device;
    private InnogyBridgeHandler bridgeHandler;

    public InnogyDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand called for channel '{}' of type '{}' with command '{}'", channelUID,
                getThing().getThingTypeUID().getId(), command);

        if (command instanceof RefreshType) {
            Device device = getInnogyBridgeHandler().refreshDevice(deviceId);
            if (device != null) {
                onDeviceStateChanged(device);
            }
            return;
        }

        // TODO: add devices
        // SWITCH
        if (channelUID.getId().equals(CHANNEL_SWITCH)) {
            if (command instanceof OnOffType) {
                getInnogyBridgeHandler().commandSwitchDevice(deviceId, OnOffType.ON.equals(command));
            }

            // SET_TEMPERATURE
        } else if (channelUID.getId().equals(CHANNEL_SET_TEMPERATURE)) {
            if (command instanceof DecimalType) {
                DecimalType pointTemperature = (DecimalType) command;
                getInnogyBridgeHandler().commandUpdatePointTemperature(deviceId, pointTemperature.doubleValue());
            }

            // ALARM
        } else if (channelUID.getId().equals(CHANNEL_ALARM)) {
            if (command instanceof OnOffType) {
                getInnogyBridgeHandler().commandSwitchAlarm(deviceId, OnOffType.ON.equals(command));
            }

        } else {
            logger.debug("UNSUPPORTED channel {} for device {}.", channelUID.getId(), deviceId);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing innogy SmartHome device handler.");
        initializeThing((getBridge() == null) ? null : getBridge().getStatus());
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {}", bridgeStatusInfo);
        initializeThing(bridgeStatusInfo.getStatus());
    }

    private void initializeThing(ThingStatus bridgeStatus) {
        logger.debug("initializeThing thing {} bridge status {}", getThing().getUID(), bridgeStatus);
        final String configDeviceId = (String) getConfig().get(PROPERTY_ID);
        if (configDeviceId != null) {
            deviceId = configDeviceId;
            // note: this call implicitly registers our handler as a listener on
            // the bridge
            if (getInnogyBridgeHandler() != null) {
                if (bridgeStatus == ThingStatus.ONLINE) {
                    updateStatus(ThingStatus.ONLINE);
                    initializeProperties();
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "device id unknown");
        }

    }

    private synchronized void initializeProperties() {
        Device device = getDevice();
        if (device != null) {
            Map<String, String> properties = editProperties();
            properties.put(PROPERTY_ID, device.getId());
            if (device.hasSerialNumber()) {
                properties.put(PROPERTY_SERIAL_NUMBER, device.getSerialnumber());
            }
            properties.put(PROPERTY_VENDOR, device.getManufacturer());
            properties.put(PROPERTY_VERSION, device.getVersion());
            if (device.hasLocation()) {
                properties.put(PROPERTY_LOCATION, device.getLocation().getName());
            }
            properties.put(PROPERTY_TIME_OF_ACCEPTANCE,
                    device.getTimeOfAcceptance().toString(Constants.FORMAT_DATETIME));
            properties.put(PROPERTY_TIME_OF_DISCOVERY, device.getTimeOfDiscovery().toString(Constants.FORMAT_DATETIME));
            updateProperties(properties);

            // TODO: check device state first! E.g. there is no state, when device is still in configuration state.
            onDeviceStateChanged(device);
        } else {
            logger.warn("initializeProperties: device is null");
        }
    }

    private Device getDevice() {
        if (getInnogyBridgeHandler() != null) {
            return getInnogyBridgeHandler().getDeviceById(deviceId);
        }
        return null;
    }

    private synchronized InnogyBridgeHandler getInnogyBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof InnogyBridgeHandler) {
                this.bridgeHandler = (InnogyBridgeHandler) handler;
                this.bridgeHandler.registerDeviceStatusListener(this);
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    @Override
    public synchronized void onDeviceStateChanged(Device device) {
        if (deviceId.equals(device.getId())) {
            logger.debug("onDeviceStateChanged called with device {}/{}", device.getName(), device.getId());

            for (Capability c : device.getCapabilityMap().values()) {
                logger.debug("->capability:{} ({}/{})", c.getId(), c.getType(), c.getName());
                // TODO: ADD DEVICES
                switch (c.getType()) {
                    case Capability.TYPE_VARIABLEACTUATOR:
                        Boolean variableActuatorState = c.getCapabilityState().getVariableActuatorState();
                        if (variableActuatorState != null) {
                            updateState(CHANNEL_SWITCH, variableActuatorState ? OnOffType.ON : OnOffType.OFF);
                        }
                        break;
                    case Capability.TYPE_SWITCHACTUATOR:
                        Boolean switchActuatorState = c.getCapabilityState().getSwitchActuatorState();
                        if (switchActuatorState != null) {
                            updateState(CHANNEL_SWITCH, switchActuatorState ? OnOffType.ON : OnOffType.OFF);
                        }
                        break;
                    case Capability.TYPE_TEMPERATURESENSOR:
                        Double temperatureSensorState = c.getCapabilityState().getTemperatureSensorState();
                        if (temperatureSensorState != null) {
                            logger.debug("-> Temperature sensor state: {}", temperatureSensorState);
                            DecimalType temp = new DecimalType(temperatureSensorState);
                            updateState(CHANNEL_TEMPERATURE, temp);
                        } else {
                            logger.debug("State for {} is STILL NULL!! cstate-id: {}, c-id: {}", c.getType(),
                                    c.getCapabilityState().getId(), c.getId());
                        }
                        break;
                    case Capability.TYPE_THERMOSTATACTUATOR:
                        Double thermostatActuatorState = c.getCapabilityState().getThermostatActuatorState();
                        if (thermostatActuatorState != null) {
                            DecimalType pointTemp = new DecimalType(thermostatActuatorState);
                            updateState(CHANNEL_SET_TEMPERATURE, pointTemp);
                        }
                        break;
                    case Capability.TYPE_HUMIDITYSENSOR:
                        Double humidityState = c.getCapabilityState().getHumiditySensorState();
                        if (humidityState != null) {
                            DecimalType humidity = new DecimalType(humidityState);
                            updateState(CHANNEL_HUMIDITY, humidity);
                        }
                        break;
                    case Capability.TYPE_WINDOWDOORSENSOR:
                        Boolean contactState = c.getCapabilityState().getWindowDoorSensorState();
                        if (contactState != null) {
                            updateState(CHANNEL_CONTACT, contactState ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
                        }
                        break;
                    case Capability.TYPE_SMOKEDETECTORSENSOR:
                        Boolean smokeState = c.getCapabilityState().getSmokeDetectorSensorState();
                        if (smokeState != null) {
                            updateState(CHANNEL_SMOKE, smokeState ? OnOffType.ON : OnOffType.OFF);
                        }
                        break;
                    case Capability.TYPE_ALARMACTUATOR:
                        Boolean alarmState = c.getCapabilityState().getAlarmActuatorState();
                        if (alarmState != null) {
                            updateState(CHANNEL_ALARM, alarmState ? OnOffType.ON : OnOffType.OFF);
                        }
                        break;
                    case Capability.TYPE_MOTIONDETECTIONSENSOR:
                        Double motionState = c.getCapabilityState().getMotionDetectionSensorState();
                        if (motionState != null) {
                            DecimalType motionCount = new DecimalType(motionState);
                            logger.debug("Motion state {} -> count {}", motionState, motionCount);
                            updateState(CHANNEL_MOTION_COUNT, motionCount);
                        } else {
                            logger.debug("State for {} is STILL NULL!! cstate-id: {}, c-id: {}", c.getType(),
                                    c.getCapabilityState().getId(), c.getId());
                        }
                        break;
                    case Capability.TYPE_LUMINANCESENSOR:
                        Double luminanceState = c.getCapabilityState().getLuminanceSensorState();
                        if (luminanceState != null) {
                            DecimalType luminance = new DecimalType(luminanceState);
                            updateState(CHANNEL_MOTION_COUNT, luminance);
                        }
                        break;

                    default:
                        logger.debug("Unsupported capability type {}.", c.getType());
                }

            }
        } else {
            logger.debug("DeviceId {} not relevant for this handler (responsible for id {})", device.getId(), deviceId);
        }
    }

    @Override
    public synchronized void onDeviceStateChanged(Device device, Event event) {
        if (deviceId.equals(device.getId())) {
            logger.debug("DeviceId {} relevant for this handler.", device.getId(), deviceId);

            if (event.isLinkedtoCapability()) {
                String linkId = event.getLinkId();
                for (Property p : event.getPropertyList()) {
                    logger.debug("State changed {} to {}.", p.getName(), p.getValue());
                    HashMap<String, Capability> capabilityMap = device.getCapabilityMap();
                    Capability capability = capabilityMap.get(linkId);
                    logger.debug("Loaded Capability {}, {} with id {}, device {} from device id {}",
                            capability.getType(), capability.getName(), capability.getId(),
                            capability.getDeviceLink().get(0).getValue(), device.getId());

                    // TODO: ADD DEVICES
                    // VariableActuator
                    if (capability.isTypeVariableActuator()) {
                        CapabilityState capabilityState = capability.getCapabilityState();
                        capabilityState.setVariableActuatorState((boolean) p.getValue());
                        onDeviceStateChanged(device);

                        // SwitchActuator
                    } else if (capability.isTypeSwitchActuator()) {
                        CapabilityState capabilityState = capability.getCapabilityState();
                        capabilityState.setSwitchActuatorState((boolean) p.getValue());
                        onDeviceStateChanged(device);

                        // TemperatureSensor
                    } else if (capability.isTypeTemperatureSensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_TEMPERATURE_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setTemperatureSensorState((double) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                            // TODO: FrostWarning
                        }

                        // ThermostatActuator
                    } else if (capability.isTypeThermostatActuator()) {
                        // if(p.getName().equals())
                        // Es gibt unterschiedliche Properties. Eher nach Property, als nach Capability unterscheiden?
                        // Oder eben beides...
                        if (p.getName().equals(CapabilityState.STATE_NAME_THERMOSTAT_ACTUATOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setThermostatActuatorState((double) p.getValue());
                            logger.debug("Thermostat ActuatorState: {}", capabilityState.getThermostatActuatorState());
                            logger.debug("Thermostat ActuatorState from device: {}", device.getCapabilityMap()
                                    .get(linkId).getCapabilityState().getThermostatActuatorState());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                            // TODO: WindowReduction, OperationMode
                        }

                        // HumiditySensor
                    } else if (capability.isTypeHumiditySensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_HUMIDITY_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setHumiditySensorState((double) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                            // TODO: MoldWarning
                        }

                        // WindowDoorSensor
                    } else if (capability.isTypeWindowDoorSensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_WINDOW_DOOR_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setWindowDoorSensorState((boolean) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                        }

                        // SmokeDetectorSensor
                    } else if (capability.isTypeSmokeDetectorSensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_SMOKE_DETECTOR_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setSmokeDetectorSensorState((boolean) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                        }

                        // AlarmActuator
                    } else if (capability.isTypeAlarmActuator()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_ALARM_ACTUATOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setAlarmActuatorState((boolean) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                        }

                        // MotionDetectionSensor
                    } else if (capability.isTypeMotionDetectionSensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_MOTION_DETECTION_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setMotionDetectionSensorState((double) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                        }

                        // LuminanceSensor
                    } else if (capability.isTypeLuminanceSensor()) {
                        if (p.getName().equals(CapabilityState.STATE_NAME_LUMINANCE_SENSOR)) {
                            CapabilityState capabilityState = capability.getCapabilityState();
                            capabilityState.setLuminanceSensorState((double) p.getValue());
                            onDeviceStateChanged(device);
                        } else {
                            logger.debug("Capability-property {} not yet supported.", p.getName());
                        }

                    } else {
                        logger.debug("Unsupported capability type {}.", capability.getType());
                    }
                }

            }
        } else {
            logger.debug("DeviceId {} not relevant for this handler (responsible for id {})", device.getId(), deviceId);
        }
    }

    @Override
    public synchronized void onDeviceRemoved(Device device) {
        logger.debug("onDeviceRemoved called with device {}/{}", device.getName(), device.getId());
        this.device = null;
        if (device.getId().equals(deviceId)) {
            updateStatus(ThingStatus.OFFLINE);
        } else {
            logger.debug("onDeviceRemoved called WITH WRONG ID?!?!");
        }
    }

    @Override
    public synchronized void onDeviceAdded(Device device) {
        logger.debug("onDeviceAdded called with device {}/{}", device.getName(), device.getId());
        this.device = device;
        if (device.getId().equals(deviceId)) {
            updateStatus(ThingStatus.ONLINE);
            onDeviceStateChanged(device);
        }
    }
}
