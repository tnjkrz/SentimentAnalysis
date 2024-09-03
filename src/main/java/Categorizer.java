import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Categorizer {

    // map to store asin as key and product summary data as the value
    private final Map<String, ProductSummary> asinToProductMap = new ConcurrentHashMap<>();

    public void categorizeReviews(List<String[]> analyzedData) {
        System.out.println("Starting categorization process...");

        // loop through each review in analyzed data
        for (String[] review : analyzedData) {
            String asin = review[2];                            // asin
            String category = review[0];                        // category
            String overallScore = review[1];                    // overall (score)
            int sentiment = Integer.parseInt(review[3]);        // sentiment score (1-5)

            String[] topics;
            if (review[4].equals("NO_TOPICS")) {
                topics = new String[0];
            } else {
                topics = review[4].split(",\\s*"); // split by comma and optional spaces
            }

            String[] adjectives;
            if (review[5].equals("NO_ADJECTIVES")) {
                adjectives = new String[0];
            } else {
                adjectives = review[5].split(",\\s*"); // split by comma and optional spaces
            }

            // get ProductSummary for this asin, create a new one if it does not exist
            ProductSummary productReview = asinToProductMap.get(asin);
            if (productReview == null) {
                productReview = new ProductSummary(category, asin);
                asinToProductMap.put(asin, productReview);
                System.out.println("Created new ProductSummary for ASIN: " + asin);
            }

            // add review data to ProductSummary
            productReview.addReview(overallScore, sentiment, topics, adjectives);
            System.out.println("Added review data to ProductSummary for ASIN: " + asin);
        }

        System.out.println("Finished categorization process.");
    }

    public Map<String, List<ProductSummary>> getCategorizedResults() {
        System.out.println("Compiling categorized results...");

        // map to store categorized results by category
        Map<String, List<ProductSummary>> categorizedResults = new LinkedHashMap<>();

        // loop through each ProductSummary and categorize them
        for (ProductSummary product : asinToProductMap.values()) {
            List<ProductSummary> productList = categorizedResults.get(product.getCategory());
            if (productList == null) {
                productList = new ArrayList<>();
                categorizedResults.put(product.getCategory(), productList);
            }
            productList.add(product);
            //System.out.println("Categorized ProductSummary under category: " + product.getCategory());
            //System.out.println("ProductSummary Details: " + product);
        }

        System.out.println("Categorized results compiled.");
        return categorizedResults;
    }

    public static class ProductSummary {
        private final String category;
        private final String asin;
        private int totalReviews = 0;
        private double totalOverallScore = 0.0;
        private double totalSentimentScore = 0.0;
        private final Map<String, Integer> topicCounts = new HashMap<>();
        private final Map<String, Integer> adjectiveCounts = new HashMap<>();

        public ProductSummary(String category, String asin) {
            this.category = category;
            this.asin = asin;
        }

        public synchronized void addReview(String overallScore, int sentiment, String[] topics, String[] adjectives) {
            // inc reviews num and add scores to total
            totalReviews++;
            totalOverallScore += Double.parseDouble(overallScore);
            totalSentimentScore += sentiment;

            // count each topic
            for (String topic : topics) {
                topic = topic.trim();
                if (!topic.isEmpty()) {
                    int count = topicCounts.getOrDefault(topic, 0);
                    topicCounts.put(topic, count + 1);
                }
            }

            // count each adjective
            for (String adjective : adjectives) {
                adjective = adjective.trim();
                if (!adjective.isEmpty()) {
                    int count = adjectiveCounts.getOrDefault(adjective, 0);
                    adjectiveCounts.put(adjective, count + 1);
                }
            }
        }

        public String getCategory() {
            return category;
        }

        public String getAsin() {
            return asin;
        }

        public double getAverageOverallScore() {
            if (totalReviews == 0) {
                return 0;
            } else {
                return totalOverallScore / totalReviews;
            }
        }

        public double getAverageSentimentScore() {
            if (totalReviews == 0) {
                return 0;
            } else {
                return totalSentimentScore / totalReviews;
            }
        }

        public List<String> getTopTopics(int limit) {
            // convert map entries (topic, count) into a list
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(topicCounts.entrySet());

            // sort list in descending order based on count of each topic
            sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            // init list to store the top topics
            List<String> topTopics = new ArrayList<>();

            // loop through sorted entries and add top topics to the list (add up 'limit' num or fewer)
            for (int i = 0; i < Math.min(limit, sortedEntries.size()); i++) {
                topTopics.add(sortedEntries.get(i).getKey());
            }

            return topTopics;
        }

        public List<String> getTopAdjectives(int limit) {
            // convert map entries (adjective, count) into a list
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(adjectiveCounts.entrySet());

            // sort list in descending order based on count of each adjective
            sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            // init list to store the top adjectives
            List<String> topAdjectives = new ArrayList<>();

            // loop through sorted entries and add top adjectives to the list (add up 'limit' num or fewer)
            for (int i = 0; i < Math.min(limit, sortedEntries.size()); i++) {
                topAdjectives.add(sortedEntries.get(i).getKey());
            }

            return topAdjectives;
        }

        public String[] toResultArray() {
            // convert top topics and adjectives to strings separated by commas
            String topTopicsStr = String.join(", ", getTopTopics(10));
            String topAdjectivesStr = String.join(", ", getTopAdjectives(10));

            // return all data as an arr
            return new String[]{
                    category,
                    String.format("%.2f", getAverageOverallScore()),
                    asin,
                    String.format("%.2f", getAverageSentimentScore()),
                    topTopicsStr,
                    topAdjectivesStr,
                    String.valueOf(totalReviews)
            };
        }
    }
}
