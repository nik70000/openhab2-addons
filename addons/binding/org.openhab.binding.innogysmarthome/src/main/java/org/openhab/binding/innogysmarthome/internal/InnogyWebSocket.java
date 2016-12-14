package org.openhab.binding.innogysmarthome.internal;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.innogysmarthome.InnogyBindingConstants;
import org.openhab.binding.innogysmarthome.handler.InnogyBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class InnogyWebSocket {

    private Logger logger = LoggerFactory.getLogger(InnogyWebSocket.class);
    private final CountDownLatch closeLatch;
    private Session session;
    private InnogyBridgeHandler bridgeHandler;
    private WebSocketClient client;
    private URI webSocketURI;

    public InnogyWebSocket(InnogyBridgeHandler bridgeHandler, URI webSocketURI) {
        this.bridgeHandler = bridgeHandler;
        this.closeLatch = new CountDownLatch(1);
        this.webSocketURI = webSocketURI;
    }

    public synchronized void start() throws Exception {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true); // The magic

        if (client == null || client.isStopped()) {
            client = new WebSocketClient(sslContextFactory);
            client.setMaxIdleTimeout(InnogyBindingConstants.WEBSOCKET_MAX_IDLE_TIMEOUT);
            client.start();
        }

        if (session != null) {
            session.close();
        }

        logger.info("Connecting to innogy WebSocket...");
        session = client.connect(this, webSocketURI).get();
    }

    public synchronized void stop() {
        logger.info("Stopping innogy WebSocket...");
        if (isRunning()) {
            logger.debug("Closing session...");
            session.close();
            session = null;
        }
    }

    public synchronized boolean isRunning() {
        return session != null && session.isOpen();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connected to innogy WebSocket.");
        logger.trace("innogy Websocket session: {}", session);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        this.closeLatch.countDown();
        if (statusCode == StatusCode.NORMAL) {
            logger.info("Connection to innogy WebSocket was closed normally.");
        } else {
            logger.info("Connection to innogy WebSocket was closed (code: {}). Reason: {}", statusCode, reason);
            bridgeHandler.onEventRunnerStopped();
        }
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        logger.debug("innogy WebSocket awaitClose() - {}{}", duration, unit);
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        logger.error("innogy WebSocket onError() - {}", cause.getMessage());
        // bridgeHandler.onEventRunnerStopped(60);
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        logger.debug("innogy WebSocket onMessage() - {}", msg);
        bridgeHandler.onEvent(msg);
    }
}
