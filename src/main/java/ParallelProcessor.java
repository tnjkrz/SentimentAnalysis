import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ParallelProcessor {

    private final BlockingQueue<String> buffer;
    private final int maxReviews;
    private final ExecutorService executorService;
    private final Categorizer categorizer;
    private final int batchSize;
    private final WebSocketClientHandler webSocketClientHandler;

    public ParallelProcessor(BlockingQueue<String> buffer, int maxReviews, String[] topics) throws InterruptedException, URISyntaxException {
        this.buffer = buffer;
        this.maxReviews = maxReviews;

        // determine the number of processor cores available
        int coreCount = Runtime.getRuntime().availableProcessors();

        // calculate batch size based on the number of available threads and total reviews
        this.batchSize = Math.max(1, (int) Math.ceil((double) maxReviews / coreCount));

        this.executorService = new ThreadPoolExecutor(
                coreCount,                   // core pool size
                coreCount * 2,               // maximum pool size
                10L,                         // time to wait before resizing pool
                TimeUnit.SECONDS,            // time unit for the timeout
                new LinkedBlockingQueue<>(), // the work queue
                new ThreadPoolExecutor.CallerRunsPolicy() // handles rejected tasks
        );

        this.categorizer = new Categorizer(); // prep for later

        // init and connect the WebSocket client
        System.out.println("Initializing WebSocket connection...");
        this.webSocketClientHandler = new WebSocketClientHandler(new URI("wss://prog3.student.famnit.upr.si/sentiment"), buffer, maxReviews);
        this.webSocketClientHandler.connectBlocking();
        System.out.println("WebSocket connection established.");

        // selecting topics
        for (String topic : topics) {
            String subscriptionMessage = "topic:" + topic.trim();
            this.webSocketClientHandler.send(subscriptionMessage);
            System.out.println("Subscribed to topic: " + topic.trim());
        }
    }

    public void process() {
        System.out.println("Parallel data processing in progress...");

        // start a separate thread to handle WebSocket closing after reaching max reviews
        new Thread(() -> {
            while (webSocketClientHandler.isOpen()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            executorService.shutdown();
        }).start();

        // process incoming reviews while WebSocket connection is open OR there are still reviews in the buffer
        while (!executorService.isShutdown() || !buffer.isEmpty()) {
            List<String> batch = new ArrayList<>();

            // gather reviews up to batchSize from the buffer
            for (int i = 0; i < batchSize && !buffer.isEmpty(); i++) {
                String reviewData = buffer.poll();
                if (reviewData != null) {
                    batch.add(reviewData);
                    System.out.println("Thread " + Thread.currentThread().getName() + " added review to batch.");
                }
            }

            if (!batch.isEmpty()) {
                System.out.println("Thread " + Thread.currentThread().getName() + " processing batch of size " + batch.size());
                // create a task for processing the batch of reviews
                ReviewProcessingTask task = new ReviewProcessingTask(batch, categorizer);
                executorService.submit(task); // Submit the Runnable task
            }
        }

        // wait for all tasks to complete
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // retrieve and display categorized results
        Map<String, List<Categorizer.ProductSummary>> categorizedData = categorizer.getCategorizedResults();
        displayCategorizedResults(categorizedData);
    }

    // display categorized results
    private void displayCategorizedResults(Map<String, List<Categorizer.ProductSummary>> categorizedData) {
        System.out.println("\nCategorized Results:");
        for (Map.Entry<String, List<Categorizer.ProductSummary>> categoryEntry : categorizedData.entrySet()) {
            String category = categoryEntry.getKey();
            List<Categorizer.ProductSummary> productList = categoryEntry.getValue();
            System.out.println("----------------------------------------------------");
            System.out.println("Category: " + category);

            for (Categorizer.ProductSummary product : productList) {
                String[] resultArray = product.toResultArray();
                System.out.println("----------------------------------------------------");
                System.out.println("ASIN: " + resultArray[2]);
                System.out.println("Average Score: " + resultArray[1]);
                System.out.println("Average Sentiment: " + resultArray[3]);
                System.out.println("Top Topics: " + resultArray[4]);
                System.out.println("Top Adjectives: " + resultArray[5]);
                System.out.println("Number of Reviews: " + resultArray[6]);
            }
        }
    }

    public static class ReviewProcessingTask implements Runnable {
        private final List<String> reviewDataBatch;
        private final Categorizer categorizer;

        public ReviewProcessingTask(List<String> reviewDataBatch, Categorizer categorizer) {
            this.reviewDataBatch = reviewDataBatch;
            this.categorizer = categorizer;
        }

        @Override
        public void run() {
            DataCleaner cleaner = new DataCleaner(new LinkedBlockingQueue<>());

            // step 1: clean the reviews batch
            System.out.println("Thread " + Thread.currentThread().getName() + " cleaning batch of size " + reviewDataBatch.size());
            List<String[]> cleanedDataBatch = new ArrayList<>();
            for (String reviewData : reviewDataBatch) {
                cleanedDataBatch.add(cleaner.cleanData(reviewData));
            }

            // step 2: perform sentiment analysis
            System.out.println("Thread " + Thread.currentThread().getName() + " performing sentiment analysis on batch of size " + cleanedDataBatch.size());
            SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
            List<String[]> analyzedDataBatch = sentimentAnalyzer.analyzeData(cleanedDataBatch);

            // step 3: categorize
            System.out.println("Thread " + Thread.currentThread().getName() + " categorizing batch of size " + analyzedDataBatch.size());
            categorizer.categorizeReviews(analyzedDataBatch);
        }
    }
}
