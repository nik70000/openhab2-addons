/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome.handler;

import static org.openhab.binding.innogysmarthome.InnogyBindingConstants.*;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.innogysmarthome.InnogyBindingConstants;
import org.openhab.binding.innogysmarthome.internal.DeviceStructureManager;
import org.openhab.binding.innogysmarthome.internal.InnogyWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.gson.Gson;

import in.ollie.innogysmarthome.Configuration;
import in.ollie.innogysmarthome.InnogyClient;
import in.ollie.innogysmarthome.entity.capability.Capability;
import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.event.Event;
import in.ollie.innogysmarthome.exception.ApiException;
import in.ollie.innogysmarthome.exception.ConfigurationException;
import in.ollie.innogysmarthome.exception.ControllerOfflineException;
import in.ollie.innogysmarthome.exception.InvalidActionTriggeredException;
import in.ollie.innogysmarthome.exception.InvalidAuthCodeException;
import in.ollie.innogysmarthome.exception.SessionExistsException;

/**
 * The {@link InnogyBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Oliver Kuhl - Initial contribution
 */
public class InnogyBridgeHandler extends BaseBridgeHandler implements CredentialRefreshListener, EventListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);
    private Logger logger = LoggerFactory.getLogger(InnogyBridgeHandler.class);
    private Configuration config = null;
    private InnogyClient client = null;
    private DeviceStructureManager deviceStructMan = null;

    private Set<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArraySet<>();

    private ScheduledFuture<?> reinitJob;

    private class Initializer implements Runnable {

        InnogyBridgeHandler bridgeHandler = null;
        // Configuration config;

        public Initializer(InnogyBridgeHandler bridgeHandler, Configuration config) {
            this.bridgeHandler = bridgeHandler;
            // this.config = config;
        }

        @Override
        public void run() {
            String id = getThing().getUID().getId();
            client = new InnogyClient(id, config);
            client.setCredentialRefreshListener(InnogyBridgeHandler.this);
            try {
                logger.info("Initializing innogy SmartHome client...");
                client.initialize();
            } catch (Exception e) {
                if (!handleClientException(e)) {
                    logger.error("Error initializing innogy SmartHome client.");
                    return;
                }
            }

            if (StringUtils.isNotBlank(client.getConfig().getRefreshToken())) {
                getThing().getConfiguration().put(REFRESH_TOKEN, client.getConfig().getRefreshToken());
                if (StringUtils.isNotBlank(client.getConfig().getAccessToken())) {
                    getThing().getConfiguration().put(ACCESS_TOKEN, client.getConfig().getAccessToken());
                }
                org.eclipse.smarthome.config.core.Configuration configuration = editConfiguration();
                configuration.put(AUTH_CODE, "");
                updateConfiguration(configuration);
                config.setAuthCode("");
            }

            deviceStructMan = new DeviceStructureManager(client);
            try {
                deviceStructMan.start();
            } catch (Exception e) {
                if (!handleClientException(e)) {
                    logger.error("Error starting device structure manager.");
                    return;
                }
            }

            updateStatus(ThingStatus.ONLINE);
            setBridgeProperties();

            // scheduler.schedule(new WebSocketRunner(bridgeHandler), 0, TimeUnit.SECONDS);
            onEventRunnerStopped();
        }

        private void setBridgeProperties() {
            if (deviceStructMan != null) {
                Device bridgeDevice = deviceStructMan.getBridgeDevice();

                Map<String, String> properties = editProperties();
                properties.put(PROPERTY_VENDOR, bridgeDevice.getManufacturer());
                properties.put(PROPERTY_SERIAL_NUMBER, bridgeDevice.getSerialnumber());
                properties.put(PROPERTY_ID, bridgeDevice.getId());
                properties.put(PROPERTY_FIRMWARE_VERSION, bridgeDevice.getFirmwareVersion());
                properties.put(PROPERTY_HARDWARE_VERSION, bridgeDevice.getHardwareVersion());
                properties.put(PROPERTY_SOFTWARE_VERSION, bridgeDevice.getSoftwareVersion());
                properties.put(PROPERTY_IP_ADDRESS, bridgeDevice.getIpAddress());
                properties.put(PROPERTY_MAC_ADDRESS, bridgeDevice.getMacAddress());
                properties.put(PROPERTY_REGISTRATION_TIME, bridgeDevice.getRegistrationTimeFormattedString());
                updateProperties(properties);

            } else {
                logger.error("device structure manager is not available.");
            }

        }
    };

    private class WebSocketRunner implements Runnable {

        private InnogyBridgeHandler bridgeHandler;

        public WebSocketRunner(InnogyBridgeHandler bridgeHandler) {
            this.bridgeHandler = bridgeHandler;
        }

        @Override
        public void run() {
            logger.info("Starting web socket.");
            String webSocketUrl = WEBSOCKET_API_URL_EVENTS.replace("{token}", (String) getConfig().get(ACCESS_TOKEN));
            logger.debug("WebSocket URL: {}",
                    webSocketUrl.substring(0, 70) + "..." + webSocketUrl.substring(webSocketUrl.length() - 10));

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(true); // The magic

            // Resource keyStoreResource = Resource.newResource(this.getClass().getResource("/truststore.jks"));
            // sslContextFactory.setKeyStoreResource(keyStoreResource);
            // sslContextFactory.setKeyStorePassword("password");
            // sslContextFactory.setKeyManagerPassword("password");

            WebSocketClient webSocketClient = new WebSocketClient(sslContextFactory);

            try {
                sslContextFactory.start();
                webSocketClient.start();
                // TODO : kann jetty autom. reconnect?
                InnogyWebSocket socket = new InnogyWebSocket(bridgeHandler);
                Future<Session> fut = webSocketClient.connect(socket, URI.create(webSocketUrl));
                Session session = fut.get();
                session.setIdleTimeout(WEBSOCKET_TIMEOUT);

                // send alive ping
                session.getRemote().sendPing(ByteBuffer.wrap("Alive?".getBytes()));
            } catch (Exception e) {
                if (!handleClientException(e)) {
                    logger.error("Error starting Websocket.");
                    return;
                }
            }
        }

    }

    /**
     * Constructs a new {@link InnogyBridgeHandler}.
     *
     * @param bridge
     */
    public InnogyBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     * org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // not needed
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {
        logger.debug("Initializing innogy SmartHome BridgeHandler...");

        // Start an extra thread to readout the configuration and check the connection, because it takes sometimes more
        // than 5000 milliseconds and the handler will suspend (ThingStatus.UNINITIALIZED).
        Configuration config = loadAndCheckConfig();

        if (config != null) {
            logger.debug(config.toString());
            // scheduler.execute(new Initializer(this, config));
            Initializer i = new Initializer(this, config);
            i.run();

        }
    }

    /**
     * Schedules a re-initialization in the given future.
     *
     * @param seconds
     */
    private void scheduleReinitialize(long seconds) {
        logger.info("Scheduling reinitialize in {} seconds.", seconds);
        reinitJob = scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                initialize();
            }
        }, seconds, TimeUnit.SECONDS);
    }

    /**
     * Schedules a re-initialization using the default {@link InnogyBindingConstants#REINITIALIZE_DELAY_SECONDS}.
     */
    private void scheduleReinitialize() {
        scheduleReinitialize(REINITIALIZE_DELAY_SECONDS);
    }

    /**
     * Loads the {@link org.eclipse.smarthome.config.core.Configuration} of the bridge thing, creates an new
     * {@link Configuration}, checks and returns it.
     *
     * @return the {@link Configuration} for the {@link InnogyClient}.
     */
    private Configuration loadAndCheckConfig() {
        org.eclipse.smarthome.config.core.Configuration thingConfig = super.getConfig();

        if (config == null) {
            config = new Configuration();
        }

        // load and check connection and authorization data
        config.setClientId(CLIENT_ID);
        config.setClientSecret(CLIENT_SECRET);

        if (StringUtils.isNotBlank((String) thingConfig.get(ACCESS_TOKEN))) {
            config.setAccessToken(thingConfig.get(ACCESS_TOKEN).toString());
        }
        if (StringUtils.isNotBlank((String) thingConfig.get(REFRESH_TOKEN))) {
            config.setRefreshToken(thingConfig.get(REFRESH_TOKEN).toString());
        }

        if (config.checkTokens()) {
            return config;
        } else {
            if (StringUtils.isNotBlank((String) thingConfig.get(AUTH_CODE))) {
                config.setAuthCode(thingConfig.get(AUTH_CODE).toString());
                return config;
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Cannot connect to innogy SmartHome service. Please set auth-code!");
                return null;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {
        logger.debug("Disposing innogy SmartHome bridge handler '{}'", getThing().getUID().getId());
        if (reinitJob != null) {
            reinitJob.cancel(true);
            reinitJob = null;
        }

        // TODO: dispose Websocket!!!!!

        if (client != null) {
            client.dispose();
            client = null;
        }

        deviceStructMan = null;

        super.dispose();
        logger.info("innogy SmartHome bridge handler shut down.");
    }

    public boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null deviceStatusListener.");
        }
        boolean result = deviceStatusListeners.add(deviceStatusListener);
        if (result) {
            // onUpdate();
            // TODO initially set current device states
        }
        return result;
    }

    public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null deviceStatusListener.");
        }
        boolean result = deviceStatusListeners.remove(deviceStatusListener);
        if (result) {
            // clearDeviceList();
        }
        return result;
    }

    /**
     * Loads a list of {@link Device}s from the bridge and returns them.
     *
     * @return a list of {@link Device}s
     */
    public List<Device> loadDevices() {
        List<Device> devices = null;
        if (client != null) {
            try {
                devices = deviceStructMan.getDeviceList();
            } catch (Exception e) {
                logger.error("Bridge cannot search for new devices.", e);
            }
        }

        return devices;
    }

    public Device getDeviceById(String deviceId) {
        return deviceStructMan.getDeviceById(deviceId);
    }

    /**
     *
     * @param deviceId
     */
    public Device refreshDevice(String deviceId) {
        if (deviceStructMan == null) {
            return null;
        }

        Device device = null;
        try {
            deviceStructMan.refreshDevice(deviceId);
            device = deviceStructMan.getDeviceById(deviceId);
        } catch (Exception e) {
            handleClientException(e);
        }
        return device;

    }

    // CredentialRefreshListener implementation

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.api.client.auth.oauth2.CredentialRefreshListener#onTokenResponse(com.google.api.client.auth.oauth2.
     * Credential, com.google.api.client.auth.oauth2.TokenResponse)
     */
    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
        String accessToken = credential.getAccessToken();
        config.setAccessToken(accessToken);
        getThing().getConfiguration().put(ACCESS_TOKEN, accessToken);
        logger.debug("innogy access token saved (onTokenResponse): {}",
                accessToken.substring(0, 10) + "..." + accessToken.substring(accessToken.length() - 10));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.google.api.client.auth.oauth2.CredentialRefreshListener#onTokenErrorResponse(com.google.api.client.auth.
     * oauth2.Credential, com.google.api.client.auth.oauth2.TokenErrorResponse)
     */
    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
        String accessToken = credential.getAccessToken();
        config.setAccessToken(accessToken);
        getThing().getConfiguration().put(ACCESS_TOKEN, accessToken);
        logger.debug("innogy access token saved (onTokenErrorResponse): {}",
                accessToken.substring(0, 10) + "..." + accessToken.substring(accessToken.length() - 10));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.innogysmarthome.handler.EventListener#onEvent(java.lang.String)
     */
    @Override
    public void onEvent(String msg) {
        logger.debug("onEvent called. Msg: {}", msg);

        try {
            Gson gson = new Gson();
            Event[] eventArray = gson.fromJson(msg, Event[].class);
            for (Event event : eventArray) {
                logger.debug("Event found: Type:{} Capability:{}", event.getType(),
                        event.getLink() != null ? event.getLink().getValue() : "(no link)");
                switch (event.getType()) {
                    case Event.TYPE_STATE_CHANGED:

                        // CAPABILITY
                        if (event.isLinkedtoCapability()) {
                            // TODO: funktioniert getDeviceByCapabilityLink auch mit Device-Link?!?
                            Device device = deviceStructMan.getDeviceByCapabilityLink(event.getLink().getValue());
                            if (device != null) {
                                for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
                                    deviceStatusListener.onDeviceStateChanged(device, event);
                                }
                            } else {
                                logger.debug("Device is null for Capability {}", event.getLink().getValue());
                            }

                            // DEVICE
                        } else if (event.isLinkedtoDevice()) {
                            Device device = deviceStructMan.getDeviceByDeviceLink(event.getLinkId());
                            if (device != null) {
                                logger.debug("DEVICE STATE CHANGED!!!");
                                for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
                                    deviceStatusListener.onDeviceStateChanged(device, event);
                                }
                            } else {
                                logger.debug("DEVICE STATE CHANGED - BUT NULL!!!!");
                            }
                        } else {
                            logger.debug("link type {} not supported (yet?)", event.getLinkType());
                        }
                        break;

                    case Event.TYPE_DISCONNECT:
                        logger.info("Websocket disconnected. Reason: {}", event.getPropertyList().get(0).getValue());
                        onEventRunnerStopped();
                        break;

                    case Event.TYPE_CONFIG_CHANGED:
                        logger.info("Configuration changed to version {}. Restarting innogy binding...",
                                event.getConfigurationVersion());
                        dispose();
                        scheduleReinitialize(0);
                        break;

                    default:
                        logger.debug("Unknown eventtype {}.", event.getType());
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error with Event: {}", e);
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.innogysmarthome.handler.EventListener#onEventRunnerStopped(long)
     */
    @Override
    public void onEventRunnerStopped(long delay) {
        // scheduler.schedule(new WebSocketRunner(this), delay, TimeUnit.SECONDS);
        WebSocketRunner wsr = new WebSocketRunner(this);
        wsr.run();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.innogysmarthome.handler.EventListener#onEventRunnerStopped()
     */
    @Override
    public void onEventRunnerStopped() {
        onEventRunnerStopped(0);
    }

    public void commandSwitchDevice(String deviceId, boolean state) {

        try {
            // TODO: ADD DEVICES
            // VariableActuator
            String deviceType = deviceStructMan.getDeviceById(deviceId).getType();
            if (deviceType.equals(DEVICE_VARIABLE_ACTUATOR)) {
                String capabilityId = deviceStructMan.getCapabilityId(deviceId, Capability.TYPE_VARIABLEACTUATOR);
                client.setVariableActuatorState(capabilityId, state);

                // PSS / PSSO / ISS2
            } else if (deviceType.equals(DEVICE_PSS) || deviceType.equals(DEVICE_PSSO)
                    || deviceType.equals(DEVICE_ISS2)) {
                String capabilityId = deviceStructMan.getCapabilityId(deviceId, Capability.TYPE_SWITCHACTUATOR);
                client.setSwitchActuatorState(capabilityId, state);
            }
        } catch (Exception e) {
            handleClientException(e);
        }
    }

    public void commandUpdatePointTemperature(String deviceId, double pointTemperature) {
        try {
            String capabilityId = deviceStructMan.getCapabilityId(deviceId, Capability.TYPE_THERMOSTATACTUATOR);
            client.setPointTemperatureState(capabilityId, pointTemperature);
        } catch (Exception e) {
            handleClientException(e);
        }
    }

    public void commandSwitchAlarm(String deviceId, boolean alarmState) {
        try {
            if (deviceStructMan.getDeviceById(deviceId).getType().equals(DEVICE_WSD)) {
                String capabilityId = deviceStructMan.getCapabilityId(deviceId, Capability.TYPE_ALARMACTUATOR);
                client.setAlarmActuatorState(capabilityId, alarmState);
            }
        } catch (Exception e) {
            handleClientException(e);
        }
    }

    /**
     * Handles all Exceptions of the client communication.
     *
     * @param e the Exception
     * @return boolean true, if binding should continue.
     */
    private boolean handleClientException(Exception e) {

        // Session exists
        if (e instanceof SessionExistsException) {
            logger.info("Session already exists. Continuing...");
            return true;

            // Controller offline
        } else if (e instanceof ControllerOfflineException) {
            logger.error("innogy SmartHome Controller is offline. {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, e.getMessage());
            dispose();
            scheduleReinitialize();
            return false;

            // Configuration error
        } else if (e instanceof ConfigurationException) {
            logger.error("Configuration error: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            dispose();
            return false;

            // invalid auth code
        } else if (e instanceof InvalidAuthCodeException) {
            logger.error("Error fetching access tokens. Invalid authcode! Please generate a new one.");
            org.eclipse.smarthome.config.core.Configuration configuration = editConfiguration();
            configuration.put(AUTH_CODE, "");
            updateConfiguration(configuration);
            config.setAuthCode("");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid authcode. Please generate a new one!");
            dispose();
            return false;

        } else if (e instanceof InvalidActionTriggeredException) {
            logger.error("Error triggering action: {}", e.getMessage());
            return true;

            // io error
        } else if (e instanceof IOException) {
            logger.error("IO error: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            dispose();
            scheduleReinitialize(3 * 60);
            return false;

            // unexpected API error
        } else if (e instanceof ApiException) {
            logger.error("Unexcepted API error: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            dispose();
            scheduleReinitialize(3 * 60);
            return false;

            // unknown
        } else {
            logger.error("Unknown exception", e);
            e.printStackTrace();
            dispose();
            scheduleReinitialize();
            return false;
        }
    }
}
