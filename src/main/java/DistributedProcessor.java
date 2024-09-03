import mpi.MPI;
import mpi.MPIException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DistributedProcessor {

    private final BlockingQueue<String> buffer;
    private final int maxReviews;

    public DistributedProcessor(BlockingQueue<String> buffer, int maxReviews) {
        this.buffer = buffer;
        this.maxReviews = maxReviews;
    }

    public void masterProcess() throws MPIException {
        int size = MPI.COMM_WORLD.Size();
        int reviewsPerWorker = maxReviews / (size - 1);
        int remainder = maxReviews % (size - 1);
        int currentWorker = 1;
        int totalProcessed = 0;

        System.out.println("Master process started. Total workers: " + (size - 1));

        while (totalProcessed < maxReviews) {
            List<String> batch = new ArrayList<>();
            int targetBatchSize = reviewsPerWorker + (remainder > 0 ? 1 : 0);
            remainder = Math.max(0, remainder - 1);

            // gather reviews up to targetBatchSize from the buffer
            while (batch.size() < targetBatchSize && totalProcessed < maxReviews) {
                String review = buffer.poll();
                if (review != null) {
                    batch.add(review);
                    totalProcessed++;
                }
            }

            if (!batch.isEmpty()) {
                System.out.println("Master sending batch of size " + batch.size() + " to worker " + currentWorker);

                // convert batch to a string w/ a delimiter then send to current worker
                String reviews = String.join("##DELIM##", batch);
                MPI.COMM_WORLD.Send(new Object[]{reviews}, 0, 1, MPI.OBJECT, currentWorker, 0);

                // move to the next worker
                currentWorker++;
                if (currentWorker >= size) {
                    break; // stop after assigning last worker
                }
            }
        }

        System.out.println("Master has finished distributing work to all workers.");

        // prep to collect all analyzed reviews
        List<String[]> allAnalyzedData = new ArrayList<>();

        // receive sentiment analyzed data from workers
        for (int i = 1; i < size; i++) {
            Object[] receivedObject = new Object[1];
            System.out.println("Master waiting to receive data from worker " + i);
            MPI.COMM_WORLD.Recv(receivedObject, 0, 1, MPI.OBJECT, i, 0);
            System.out.println("Master received sentiment-analyzed data from worker " + i);

            // deserialize analyzed data
            String analyzedData = (String) receivedObject[0];
            String[] analyzedDataArray = analyzedData.split("##REVIEW_DELIM##");

            // convert each string back to the array form (simulating the sentiment analyzer output format)
            for (String analyzedReview : analyzedDataArray) {
                String[] reviewArray = analyzedReview.split(" ");
                allAnalyzedData.add(reviewArray);
            }
        }

        // categorize analyzed reviews
        Categorizer categorizer = new Categorizer();
        categorizer.categorizeReviews(allAnalyzedData);

        // display the categorized results
        System.out.println("Categorized Results:");
        for (Map.Entry<String, List<Categorizer.ProductSummary>> entry : categorizer.getCategorizedResults().entrySet()) {
            System.out.println("----------------------------------------------------");
            System.out.println("Category: " + entry.getKey());

            for (Categorizer.ProductSummary summary : entry.getValue()) {
                String[] resultArray = summary.toResultArray();
                System.out.println("----------------------------------------------------");
                System.out.println("ASIN: " + resultArray[2]);
                System.out.println("Average Score: " + resultArray[1]);
                System.out.println("Average Sentiment: " + resultArray[3]);
                System.out.println("Top Topics: " + resultArray[4]);
                System.out.println("Top Adjectives: " + resultArray[5]);
                System.out.println("Number of Reviews: " + resultArray[6]);
            }
        }

        System.out.println("Master processing and printing finished.");
    }

    public void workerProcess() throws MPIException {
        int rank = MPI.COMM_WORLD.Rank();
        System.out.println("Worker " + rank + " process started.");

        // wait to receive reviews from the master
        Object[] receivedObject = new Object[1];
        MPI.COMM_WORLD.Recv(receivedObject, 0, 1, MPI.OBJECT, 0, 0);
        String receivedString = (String) receivedObject[0];

        if (receivedString != null && !receivedString.isEmpty()) {
            System.out.println("Worker " + rank + " received reviews from master.");

            // deserialize received string back to an array
            String[] reviews = receivedString.split("##DELIM##");
            System.out.println("Worker " + rank + " is processing " + reviews.length + " reviews.");

            // process received reviews (cleaning and sentiment analysis)
            DataCleaner cleaner = new DataCleaner(buffer);
            List<String[]> cleanedData = new ArrayList<>();
            for (String review : reviews) {
                String[] cleanedReview = cleaner.cleanData(review);
                cleanedData.add(cleanedReview);
            }

            // pass the cleanedData list to the SentimentAnalyzer
            SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
            List<String[]> analyzedData = sentimentAnalyzer.analyzeData(cleanedData);

            // serialize analyzed reviews back to a string
            List<String> analyzedStringList = new ArrayList<>();
            for (String[] analyzedReviewArray : analyzedData) {
                analyzedStringList.add(String.join(" ", analyzedReviewArray)); // join array elements into a single string
            }
            String analyzedString = String.join("##REVIEW_DELIM##", analyzedStringList); // join all reviews w/ delimiter

            System.out.println("Worker " + rank + " sending sentiment analyzed data back to master.");

            // send analyzed reviews back to master node
            MPI.COMM_WORLD.Send(new Object[]{analyzedString}, 0, 1, MPI.OBJECT, 0, 0);
        }
    }
}
