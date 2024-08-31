import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketClientHandler extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final BlockingQueue<String> buffer;
    private boolean isFirstMessage = true; // to track first message
    private final int maxReviews;
    private final AtomicInteger reviewCount = new AtomicInteger(0);

    public WebSocketClientHandler(URI serverUri, BlockingQueue<String> buffer, int maxReviews) {
        super(serverUri);
        this.buffer = buffer;
        this.maxReviews = maxReviews;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to the WebSocket server and began collecting reviews.");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        //logger.info("Connection closed with exit code: {}. Reason: {}", code, reason);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("An error occurred: {}", ex.getMessage(), ex);
    }

    @Override
    public void onMessage(String message) {
        try {
            if (isFirstMessage) {
                isFirstMessage = false;
            } else {
                buffer.put(message);
                int currentCount = reviewCount.incrementAndGet();
                System.out.println("Review " + reviewCount + " received.");
                if (currentCount >= maxReviews) {
                    System.out.println("Max reviews reached, closing connection.");
                    this.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error adding message to buffer: {}", e.getMessage(), e);
        }
    }

    public static BlockingQueue<String> connectAndReceiveMessages(int maxReviews, String[] topics) {
        BlockingQueue<String> buffer = new LinkedBlockingQueue<>();

        try {
            URI uri = new URI("wss://prog3.student.famnit.upr.si/sentiment");

            WebSocketClientHandler client = new WebSocketClientHandler(uri, buffer, maxReviews);
            client.connectBlocking();

            // selecting topics
            for (String topic : topics) {
                String subscriptionMessage = "topic:" + topic.trim();
                client.send(subscriptionMessage);
                //logger.info("Subscribed to topic: {}", topic.trim());
            }

            while (client.isOpen()) {
                Thread.sleep(100);
            }

        } catch (URISyntaxException | InterruptedException e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
        }

        return buffer;
    }
}
