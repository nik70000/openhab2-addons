# innogy SmartHome binding

The binding integrates the [innogy SmartHome](http://innogy.com/smarthome) System into openHAB. It uses the official API as provided by innogy as cloud service. As all status updates and commands have to go through the API, a permanent internet connection is required. Currently there is no API for a direct communication with the innogy SmartHome Controller (SHC).

## Supported things

* SmartHome Controller (SHC): this is the bridge, that provides the central communication with the devices
* Basic Remote Controller (BRC8)
* In Wall Smart Controller (ISC2)
* In Wall Smart Dimmer (ISD2)
* In Wall Smart Switch (ISS2)
* In Wall Smart Rollershutter (ISR2)
* Pluggable Smart Dimmer (PSD)
* Pluggable Smart Switch, indoor and outdoor (PSS/PSSO)
* Pluggable Smart Switch Outdoor (PSSO)
* Radiator Mounted Smart Thermostat (RST)
* VariableActuator
* Wall Mounted Motion Detector, indoor and outdoor (WMD/WMDO)
* Wall Mounted Room Thermostat (WRT)
* Wall Mounted Smart Controller (WSC2)
* Wall Mounted Smoke Detector, old and new version (WSD/WSD2)
* Window Door Sensor (WDS)

## Channels

| Channel Type ID | Item Type    | Description  | Available on thing |
|-------------|--------|-----------------------------|------------------------------------|
| mode | String | This channel indicates the mode of a thermostat (AUTOMATIC/MANUAL/BOOST/VACATION). | thermostat, thermostatplus, wallthermostat |
| battery_low | Switch | This channel indicates if the device battery is low. (ON/OFF) | thermostat, thermostatplus, wallthermostat, ecoswitch, shuttercontact |
| set_temp | Number | This channel indicates the sets temperature (in °C) of a thermostat. | thermostat, thermostatplus, wallthermostat |
| actual_temp | Number | This channel indicates the measured temperature (in °C) of a thermostat (see below for more details). | thermostat, thermostatplus, wallthermostat |
| valve | Number | This channel indicates the valve opening in %. Note this is an advaned setting, normally not visible. | thermostat, thermostatplus, wallthermostat |
| locked | Contact | This channel indocates if the thermostat is locked for adjustments (OPEN/CLOSED). Note this is an advanced setting, normally not visible. | thermostat, thermostatplus, wallthermostat |
| contact_state | Contact | This channel indicates the contact state for a shutterswitch (OPEN/CLOSED). | shuttercontact |
| free_mem | Number | This channel indicates the free available memory on the cube to hold send commands. Note this is an advanced setting, normally not visible. | bridge |
| duty_cycle | Number | This channel indicates the duty cycle (due to regulatory compliance reasons the cube is allowed only to send for a limited time. Duty cycle indicates how much of the available time is consumed) Note this is an advanced setting, normally not visible. | bridge |
