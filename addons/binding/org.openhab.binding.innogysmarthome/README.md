# innogy SmartHome binding

The binding integrates the [innogy SmartHome](http://innogy.com/smarthome) System into openHAB. It uses the official API as provided by innogy as cloud service. As all status updates and commands have to go through the API, a permanent internet connection is required. Currently there is no API for a direct communication with the innogy SmartHome Controller (SHC).

**Note:** All was tested with innogy SmartHome firmware version XXX

## Supported things

### Bridge
The innogy SmartHome Controller (SHC) is the bridge, that provides the central communication with the devices. Without the SHC, you cannot communicate with the devices.

### Devices
The following table shows all supported and tested devices and their channels. The channels are described in detail in the next chapter.

| Device | Description | Supported channels |
| ------ | ----------- | ------------------ |
| BRC8 | Basic Remote Controller | key1_count ... key8_count, battery_low |
| ISC2 | In Wall Smart Controller | key1_count, key2_count |
| ISD2 | In Wall Smart Dimmer | key1_count, key2_count, dimmer |
| ISR2 | In Wall Smart Rollershutter | key1_count, key2_count, rollershutter |
| ISS2 | In Wall Smart Switch | key1_count, key2_count, switch |
| PSD | Pluggable Smart Dimmer | dimmer |
| PSS | Pluggable Smart Switch, indoor | switch |
| PSSO | Pluggable Smart Switch, outdoor | switch |
| RST | Radiator Mounted Smart Thermostat | set_temperature, temperature, frost_warning, humidity, mold_warning, operation_mode, window_reduction_active, battery_low |
| | VariableActuator | switch |
| WDS | Window Door Sensor | contact, battery_low |
| WMD| Wall Mounted Motion Detector, indoor | motion_count, luminance, battery_low |
| WMDO | Wall Mounted Motion Detector, outdoor | motion_count, luminance, battery_low |
| WRT | Wall Mounted Room Thermostat | set_temperature, temperature, humidity, battery_low |
| WSC2 | Wall Mounted Smart Controller | key1_count, key2_count, battery_low |
| WSD | Wall Mounted Smoke Detector, old version | smoke, alarm, battery_low |
| WSD2 | Wall Mounted Smoke Detector, new version | smoke, alarm, battery_low |

## Channels

| Channel Type ID | Item Type    | Description  | Available on thing |
| --------------- | ------------ | ------------ | ------------------ |
| alarm | Switch | Switches the alarm (ON/OFF) | WSD, WSD2 | 
| battery_low | Switch | Indicates, if the battery is low (ON/OFF) | BRC8, RST, WDS, WMD, WMD0, WRT, WSC2, WSD, WSD2 |
| contact | Contact | Indicates the contact state (OPEN/CLOSED) | WDS |
| dimmer | Dimmer | Allows to dimm a light device | ISD2, PSD |
| frost_warning | Switch | active, if the measured temperature is too low (ON/OFF) | RST |
| humidity | Number | The actual humidity in percents | RST, WRT |
| key1_count | Number | number of key pushes for key 1, increased with each push | BRC8, ISC2, ISD2, ISR2, ISS2, WSC2 |
| key2_count | Number | number of key pushes for key 2, increased with each push | BRC8, ISC2, ISD2, ISR2, ISS2, WSC2 |
| key3_count | Number | number of key pushes for key 3, increased with each push | BRC8 |
| key4_count | Number | number of key pushes for key 4, increased with each push | BRC8 |
| key5_count | Number | number of key pushes for key 5, increased with each push | BRC8 |
| key6_count | Number | number of key pushes for key 6, increased with each push | BRC8 |
| key7_count | Number | number of key pushes for key 7, increased with each push | BRC8 |
| key8_count | Number | number of key pushes for key 8, increased with each push | BRC8 |
| luminance | Number | Indicates the measured luminance in percents | WMD, WMD0 |
| mold_warning | Switch | active, if the measured humidity is too low (ON/OFF) | RST | 
| motion_count | Number | Number of detected motions, increases with each detected motion | WMD, WMDO |
| operation_mode | String | the mode of a thermostat (auto/manual) | RST | 
| rollershutter | Rollershutter | Controlls a roller shutter | ISR2 |
| set_temperature | Number | Sets the target temperature in °C | RST, WRT |
| smoke | Switch | Indicates, if smoke was detected (ON/OFF) | WSD, WSD2 |
| switch | Switch | A switch to turn the device or variable on/off (ON/OFF) | ISS2, PSS, PSSO, VariableActuator |
| temperature | Number | Holds the actual temperature in °C | RST, WRT |
| window_reduction_active | Switch | indicates if a linked window is open and temperature reduced (ON/OFF)  | RST |


