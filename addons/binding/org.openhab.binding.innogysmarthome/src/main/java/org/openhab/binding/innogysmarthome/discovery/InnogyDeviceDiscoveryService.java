package org.openhab.binding.innogysmarthome.discovery;

import static org.openhab.binding.innogysmarthome.InnogyBindingConstants.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.innogysmarthome.handler.DeviceStatusListener;
import org.openhab.binding.innogysmarthome.handler.InnogyBridgeHandler;
import org.openhab.binding.innogysmarthome.handler.InnogyDeviceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ollie.innogysmarthome.entity.device.Device;
import in.ollie.innogysmarthome.entity.event.Event;

public class InnogyDeviceDiscoveryService extends AbstractDiscoveryService implements DeviceStatusListener {

    private static int SEARCH_TIME = 60;
    private final static Logger logger = LoggerFactory.getLogger(InnogyDeviceDiscoveryService.class);
    private InnogyBridgeHandler bridgeHandler;

    public InnogyDeviceDiscoveryService(InnogyBridgeHandler bridgeHandler) {
        super(SEARCH_TIME);
        this.bridgeHandler = bridgeHandler;
    }

    /**
     * Activates the {@link InnogyDeviceDiscoveryService} by registering it as {@link DeviceStatusListener} on the
     * {@link InnogyBridgeHandler}.
     */
    public void activate() {
        bridgeHandler.registerDeviceStatusListener(this);
    }

    /**
     * Deactivates the {@link InnogyDeviceDiscoveryService} by unregistering it as {@link DeviceStatusListener} on the
     * {@link InnogyBridgeHandler}. Older discovery results will be removed.
     *
     * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#deactivate()
     */
    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
        bridgeHandler.unregisterDeviceStatusListener(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#getSupportedThingTypes()
     */
    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return InnogyDeviceHandler.SUPPORTED_THING_TYPES;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#startScan()
     */
    @Override
    protected void startScan() {
        logger.debug("SCAN for new innogy devices started...");

        List<Device> devices = bridgeHandler.loadDevices();
        if (devices != null) {
            for (Device d : devices) {
                onDeviceAdded(d);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#stopScan()
     */
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    public void onDeviceStateChanged(Device device) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeviceStateChanged(Device device, Event event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeviceRemoved(Device device) {
        ThingUID thingUID = getThingUID(device);

        if (thingUID != null) {
            thingRemoved(thingUID);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.innogysmarthome2.handler.DeviceStatusListener#onDeviceAdded(in.ollie.innogysmarthome.
     * InnogyClient, in.ollie.innogysmarthome.entity.device.Device)
     */
    @Override
    public void onDeviceAdded(Device device) {
        ThingUID thingUID = getThingUID(device);
        ThingTypeUID thingTypeUID = getThingTypeUID(device);
        if (thingUID != null && thingTypeUID != null) {

            ThingUID bridgeUID = bridgeHandler.getThing().getUID();

            String name = device.getName();
            if (name.isEmpty()) {
                name = device.getSerialnumber();
            }

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PROPERTY_ID, device.getId());

            String label;
            if (device.hasLocation()) {
                label = device.getType() + ": " + name + " (" + device.getLocation().getName() + ")";
            } else {
                label = device.getType() + ": " + name;
            }

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeUID).withLabel(label).build();

            thingDiscovered(discoveryResult);
        } else {
            logger.debug("Discovered unsupported device of type '{}' and name '{}' with id {}", device.getType(),
                    device.getName(), device.getId());
        }
    }

    /**
     * Returns the {@link ThingUID} for the given {@link Device} or null, if the device type is not available.
     *
     * @param device
     * @return
     */
    private ThingUID getThingUID(Device device) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = getThingTypeUID(device);

        if (thingTypeUID != null && getSupportedThingTypes().contains(thingTypeUID)) {
            return new ThingUID(thingTypeUID, bridgeUID, device.getId());
        } else {
            return null;
        }
    }

    /**
     * Returns a {@link ThingTypeUID} for the given {@link Device} or null, if the device type is not available.
     *
     * @param device
     * @return
     */
    private ThingTypeUID getThingTypeUID(Device device) {
        String thingTypeId = device.getType();
        return thingTypeId != null ? new ThingTypeUID(BINDING_ID, thingTypeId) : null;
    }
}
