package org.openhab.binding.innogysmarthome.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.openhab.binding.innogysmarthome.handler.InnogyBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class InnogyWebSocket {

    private Logger logger = LoggerFactory.getLogger(InnogyWebSocket.class);
    private final CountDownLatch closeLatch;
    @SuppressWarnings("unused")
    private Session session;
    public boolean isConnected = false;
    private InnogyBridgeHandler bridgeHandler;

    public InnogyWebSocket(InnogyBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        this.closeLatch = new CountDownLatch(1);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connected to innogy WebSocket.");
        logger.debug("innogy Websocket session: {}", session);
        this.session = session;
        isConnected = true;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info("Connection to innogy WebSocket was closed (code: {}). Reason: {}", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
        isConnected = false;
        bridgeHandler.onEventRunnerStopped();
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        logger.warn("Error with innogy WebSocket: {}", cause);
        isConnected = false;
        // bridgeHandler.onEventRunnerStopped(60);
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        logger.debug("innogy WebSocket onMessage() - {}", msg);
        bridgeHandler.onEvent(msg);
    }
}
