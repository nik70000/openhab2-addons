package org.openhab.binding.innogysmarthome.internal.manager;

import org.openhab.binding.innogysmarthome.internal.listener.ConnectionListener;

import com.google.api.client.auth.oauth2.CredentialRefreshListener;

import in.ollie.innogysmarthome.Configuration;
import in.ollie.innogysmarthome.InnogyClient;

public class ConnectionManager {

    private Configuration config;
    private InnogyClient client;
    private ConnectionListener connListener = null;
    private Boolean lostConnectionState = false;

    public ConnectionManager(Configuration config, CredentialRefreshListener refreshListener,
            ConnectionListener connectionListener) {
        this.connListener = connectionListener;
        init(config, refreshListener);
    }

    private void init(Configuration config, CredentialRefreshListener refreshListener) {
        this.config = config;
        client = new InnogyClient(config);
        client.setCredentialRefreshListener(refreshListener);
    }

    public synchronized boolean checkConnection() {
        int code = this.client.checkConnection();
        return true;
    }

    private void onNotAuthenticated() {

    }

    /**
     * This method is called whenever the connection to the innogy SmartHome service is lost.
     *
     * @param reason
     */
    private void onConnectionLost(String reason) {
        if (connListener != null) {
            connListener.onConnectionStateChange(ConnectionListener.CONNECTION_LOST, reason);
        }
    }

    /**
     * This method is called whenever the connection to the innogy SmartHome service is resumed.
     */
    private void onConnectionResumed() {
        if (connListener != null) {
            connListener.onConnectionStateChange(ConnectionListener.CONNECTION_RESUMED);
        }
    }

    /**
     * Registers a connection listener.
     *
     * @param listener
     */
    public void registerConnectionListener(ConnectionListener listener) {
        this.connListener = listener;
    }

    /**
     * Unregisters a connection listener.
     */
    public void unregisterConnectionListener() {
        this.connListener = null;
    }
}
