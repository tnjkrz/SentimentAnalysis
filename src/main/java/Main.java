import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Main {
    public static void main(String[] args) {
        System.out.println("""
                ----------------------------------------------------
                Welcome to the Sentiment Analyzer Program!
                Please select the mode of operation (1,2,3):
                1 - Sequential
                2 - Parallel
                3 - Distributive
                ----------------------------------------------------""");

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();

        System.out.println("Enter the number of reviews to process: ");
        int maxReviews = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Please enter the topics you wish to subscribe to, separated by commas (movies,electronics,music,toys,pet-supplies,automotive,sport):");
        String topicsInput = scanner.nextLine();
        String[] topics = topicsInput.split(",");

        // initialization of buffer and beginning population of queue
        BlockingQueue<String> buffer = WebSocketClientHandler.connectAndReceiveMessages(maxReviews, topics);

        switch (choice) {
            case 1:
                SequentialProcessor sequentialProcessor = new SequentialProcessor(buffer, maxReviews);
                sequentialProcessor.process();
                break;
            case 2:
                // parallel processing here
                break;
            case 3:
                // distributive processing here
                break;
            default:
                System.out.println("Invalid choice. Please restart the program.");
                System.exit(0);
        }
    }
}
