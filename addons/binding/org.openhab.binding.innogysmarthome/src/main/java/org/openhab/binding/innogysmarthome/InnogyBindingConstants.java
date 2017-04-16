/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.innogysmarthome;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link InnogySmartHome2Binding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Oliver Kuhl - Initial contribution
 */
public class InnogyBindingConstants {

    public static final String BINDING_ID = "innogysmarthome";

    // Bridge config parameters
    public final static String HOST = "host";
    public final static String CLIENT_ID = "24635748";
    public final static String CLIENT_SECRET = "no secret";

    public final static String AUTH_CODE = "authcode";
    public final static String ACCESS_TOKEN = "accesstoken";
    public final static String REFRESH_TOKEN = "refreshtoken";
    public final static String WEBSOCKET_IDLE_TIMEOUT = "websocketidletimeout";

    public final static long REINITIALIZE_DELAY_SECONDS = 30;
    public final static long REINITIALIZE_DELAY_LONG_SECONDS = 120;

    // API URLs
    public final static String API_VERSION = "1.0";
    public final static String WEBSOCKET_API_URL_EVENTS = "wss://api.services-smarthome.de/API/" + API_VERSION
            + "/events?token={token}";

    // properties
    public final static String PROPERTY_ID = "id";
    public final static String PROPERTY_VENDOR = "Vendor";
    public final static String PROPERTY_VERSION = "Version";
    public final static String PROPERTY_SERIAL_NUMBER = "Serial number";
    public final static String PROPERTY_LOCATION = "Location";
    public final static String PROPERTY_FIRMWARE_VERSION = "Firmware version";
    public final static String PROPERTY_HARDWARE_VERSION = "Hardware version";
    public final static String PROPERTY_SOFTWARE_VERSION = "Software version";
    public final static String PROPERTY_IP_ADDRESS = "IP address";
    public final static String PROPERTY_MAC_ADDRESS = "MAC address";
    public final static String PROPERTY_REGISTRATION_TIME = "Registration Time";
    public final static String PROPERTY_TIME_OF_ACCEPTANCE = "Time of acceptance";
    public final static String PROPERTY_TIME_OF_DISCOVERY = "Time of discovery";

    // List of main device types
    public final static String DEVICE_SHC = "SHC"; // smarthome controller - the bridge
    public final static String DEVICE_PSS = "PSS"; // pluggable smart switch
    public final static String DEVICE_PSSO = "PSSO"; // pluggable smart switch outdoor
    public final static String DEVICE_VARIABLE_ACTUATOR = "VariableActuator";
    public final static String DEVICE_RST = "RST"; // radiator mounted smart thermostat
    public final static String DEVICE_WRT = "WRT"; // wall mounted room thermostat
    public final static String DEVICE_WDS = "WDS"; // window door sensor
    public final static String DEVICE_ISS2 = "ISS2"; // inwall smart switch
    public final static String DEVICE_WSD = "WSD"; // wall mounted smoke detector
    public final static String DEVICE_WSD2 = "WSD2"; // wall mounted smoke detector
    public final static String DEVICE_WMD = "WMD"; // wall mounted motion detector indoor
    public final static String DEVICE_WMDO = "WMDO"; // wall mounted motion detector outdoor
    public final static String DEVICE_WSC2 = "WSC2"; // wall mounted smart controller (2 buttons)
    public final static String DEVICE_BRC8 = "BRC8"; // basic remote controller (8 buttons)
    public final static String DEVICE_ISC2 = "ISC2"; // in wall smart controller (2 buttons)
    public final static String DEVICE_ISD2 = "ISD2"; // in wall smart dimmer (2 buttons)
    public final static String DEVICE_PSD = "PSD"; // pluggable smart dimmer

    public final static Set<String> SUPPORTED_DEVICES = ImmutableSet.of(DEVICE_SHC, DEVICE_PSS, DEVICE_PSSO,
            DEVICE_VARIABLE_ACTUATOR, DEVICE_RST, DEVICE_WRT, DEVICE_WDS, DEVICE_ISS2, DEVICE_WSD, DEVICE_WSD2,
            DEVICE_WMD, DEVICE_WMDO, DEVICE_WSC2, DEVICE_BRC8, DEVICE_ISC2, DEVICE_ISD2, DEVICE_PSD);

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public final static ThingTypeUID THING_TYPE_PSS = new ThingTypeUID(BINDING_ID, DEVICE_PSS);
    public final static ThingTypeUID THING_TYPE_PSSO = new ThingTypeUID(BINDING_ID, DEVICE_PSSO);
    public final static ThingTypeUID THING_TYPE_VARIABLE_ACTUATOR = new ThingTypeUID(BINDING_ID,
            DEVICE_VARIABLE_ACTUATOR);
    public final static ThingTypeUID THING_TYPE_RST = new ThingTypeUID(BINDING_ID, DEVICE_RST);
    public final static ThingTypeUID THING_TYPE_WRT = new ThingTypeUID(BINDING_ID, DEVICE_WRT);
    public final static ThingTypeUID THING_TYPE_WDS = new ThingTypeUID(BINDING_ID, DEVICE_WDS);
    public final static ThingTypeUID THING_TYPE_ISS2 = new ThingTypeUID(BINDING_ID, DEVICE_ISS2);
    public final static ThingTypeUID THING_TYPE_WSD = new ThingTypeUID(BINDING_ID, DEVICE_WSD);
    public final static ThingTypeUID THING_TYPE_WSD2 = new ThingTypeUID(BINDING_ID, DEVICE_WSD2);
    public final static ThingTypeUID THING_TYPE_WMD = new ThingTypeUID(BINDING_ID, DEVICE_WMD);
    public final static ThingTypeUID THING_TYPE_WMDO = new ThingTypeUID(BINDING_ID, DEVICE_WMDO);
    public final static ThingTypeUID THING_TYPE_WSC2 = new ThingTypeUID(BINDING_ID, DEVICE_WSC2);
    public final static ThingTypeUID THING_TYPE_BRC8 = new ThingTypeUID(BINDING_ID, DEVICE_BRC8);
    public final static ThingTypeUID THING_TYPE_ISC2 = new ThingTypeUID(BINDING_ID, DEVICE_ISC2);
    public final static ThingTypeUID THING_TYPE_ISD2 = new ThingTypeUID(BINDING_ID, DEVICE_ISD2);
    public final static ThingTypeUID THING_TYPE_PSD = new ThingTypeUID(BINDING_ID, DEVICE_PSD);

    public final static Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES = ImmutableSet.of(THING_TYPE_PSS,
            THING_TYPE_PSSO, THING_TYPE_VARIABLE_ACTUATOR, THING_TYPE_RST, THING_TYPE_WRT, THING_TYPE_WDS,
            THING_TYPE_ISS2, THING_TYPE_WSD, THING_TYPE_WSD2, THING_TYPE_WMD, THING_TYPE_WMDO, THING_TYPE_WSC2,
            THING_TYPE_BRC8, THING_TYPE_ISC2, THING_TYPE_ISD2, THING_TYPE_PSD);

    // List of all Channel ids
    public final static String CHANNEL_SWITCH = "switch";
    public final static String CHANNEL_SET_TEMPERATURE = "set_temperature";
    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_HUMIDITY = "humidity";
    public final static String CHANNEL_CONTACT = "contact";
    public final static String CHANNEL_SMOKE = "smoke";
    public final static String CHANNEL_ALARM = "alarm";
    public final static String CHANNEL_MOTION_COUNT = "motion_count";
    public final static String CHANNEL_LUMINANCE = "luminance";
    public final static String CHANNEL_OPERATION_MODE = "operation_mode";
    public final static String CHANNEL_FROST_WARNING = "frost_warning";
    public final static String CHANNEL_MOLD_WARNING = "mold_warning";
    public final static String CHANNEL_WINDOW_REDUCTION_ACTIVE = "window_reduction_active";
    public final static String CHANNEL_KEY1_COUNT = "key1_count";
    public final static String CHANNEL_KEY2_COUNT = "key2_count";
    public final static String CHANNEL_KEY3_COUNT = "key3_count";
    public final static String CHANNEL_KEY4_COUNT = "key4_count";
    public final static String CHANNEL_KEY5_COUNT = "key5_count";
    public final static String CHANNEL_KEY6_COUNT = "key6_count";
    public final static String CHANNEL_KEY7_COUNT = "key7_count";
    public final static String CHANNEL_KEY8_COUNT = "key8_count";
    public final static String CHANNEL_DIMMER = "dimmer";

}
