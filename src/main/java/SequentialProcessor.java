import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class SequentialProcessor {

    private final BlockingQueue<String> buffer;
    private final int maxReviews;

    public SequentialProcessor(BlockingQueue<String> buffer, int maxReviews) {
        this.buffer = buffer;
        this.maxReviews = maxReviews;
    }

    public void process() {
        System.out.println("Data processing in progress...");

        // step 1: clean data
        DataCleaner cleaner = new DataCleaner(buffer);
        List<String[]> cleanedData = cleaner.extractAndCleanData(maxReviews);
        System.out.println("Cleaning complete...");

        // step 2: perform sentiment analysis
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
        List<String[]> analyzedData = sentimentAnalyzer.analyzeData(cleanedData);
        System.out.println("Sentiment analysis complete...");

        // step 3: categorize data
        Categorizer categorizer = new Categorizer();
        categorizer.categorizeReviews(analyzedData);
        System.out.println("Categorization complete...");

        // step 4: retrieve and display data
        Map<String, List<Categorizer.ProductSummary>> categorizedData = categorizer.getCategorizedResults();
        displayCategorizedResults(categorizedData);
    }

    // displays the categorized results
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
}
