import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketClientHandler extends WebSocketClient {

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
        //System.out.println("Connection closed with exit code " + code + " and reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("An error occurred: " + ex.getMessage());
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
            System.err.println("Error adding message to buffer: " + e.getMessage());
        }
    }

    public static void connectAndReceiveMessages(int maxReviews, String[] topics, BlockingQueue<String> buffer) {
        try {
            URI uri = new URI("wss://prog3.student.famnit.upr.si/sentiment");

            WebSocketClientHandler client = new WebSocketClientHandler(uri, buffer, maxReviews);
            client.connectBlocking();

            // selecting topics
            for (String topic : topics) {
                String subscriptionMessage = "topic:" + topic.trim();
                client.send(subscriptionMessage);
                System.out.println("Subscribed to topic: " + topic.trim());
            }

            // keep connection open (caused errors otherwise)
            while (client.isOpen()) {
                Thread.sleep(100);
            }

        } catch (URISyntaxException | InterruptedException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
