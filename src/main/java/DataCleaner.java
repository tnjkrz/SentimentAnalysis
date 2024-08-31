import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataCleaner {

    private final BlockingQueue<String> buffer;

    public DataCleaner(BlockingQueue<String> buffer) {
        this.buffer = buffer;
    }

    // extract and clean data from buffer
    public List<String[]> extractAndCleanData(int numberOfLines) {
        List<String[]> cleanedData = new ArrayList<>();

        // extract the specified num of lines/reviews from buffer
        for (int i = 0; i < numberOfLines; i++) {
            // if buffer is empty, break out of loop
            if (buffer.isEmpty()) {
                System.out.println("Buffer is empty, stopping extraction.");
                break;
            }

            String message = buffer.poll();
            if (message != null) {
                String[] cleaned = cleanData(message);
                cleanedData.add(cleaned);
            }
        }

        return cleanedData;
    }

    // cleaning data using regex
    public String[] cleanData(String data) {
        // remove double backslashes followed by any character, remove quotations
        data = data.replaceAll("\\\\n", " ")   // replace double newlines
                .replaceAll("\\\\", "")        // remove remaining backslashes
                .replaceAll("\"", "");         // remove quotations

        String category = extractCategory(data);
        String overall = extractValue(data, "overall: ([0-9.]+)");
        String asin = extractValue(data, "asin: ([A-Z0-9]+)");
        String reviewText = extractReviewText(data);

        return new String[]{category, overall, asin, reviewText};
    }

    private String extractCategory(String data) {
        // matches first key in JSON object (category)
        Pattern pattern = Pattern.compile("^\\{([^:]+):");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Not found"; // default if pattern not found
        }
    }

    private String extractValue(String data, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Not found"; // default if pattern not found
        }
    }

    private String extractReviewText(String data) {
        // look for "reviewText:" key and extract until start of next key or end of string
        Pattern pattern = Pattern.compile("reviewText: (.*?)(,\\s\\w+:|$)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            // replace any multiple newlines or any other unwanted characters
            return matcher.group(1).replaceAll("\\\\n", " ").replaceAll("\\s+", " ").trim();
        } else {
            return "Not found"; // default if pattern not found
        }
    }
}
