import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        System.out.println("----------------------------------------------------");
        System.out.println("Welcome to the Sentiment Analyzer Program!");
        System.out.println("Please select the mode of operation (1,2,3):");
        System.out.println("1 - Sequential");
        System.out.println("2 - Parallel");
        System.out.println("3 - Distributive (must be run through mpjrun.sh)");
        System.out.println("----------------------------------------------------");

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();

        System.out.println("Enter the number of reviews to process: ");
        int maxReviews = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Please enter the topics you wish to subscribe to, separated by commas (movies,electronics,music,toys,pet-supplies,automotive,sport):");
        String topicsInput = scanner.nextLine();
        String[] topics = topicsInput.split(",");

        // start timing here
        long startTime = System.currentTimeMillis();

        switch (choice) {
            case 1:
                BlockingQueue<String> sequentialBuffer = new LinkedBlockingQueue<>();
                WebSocketClientHandler.connectAndReceiveMessages(maxReviews, topics, sequentialBuffer);
                SequentialProcessor sequentialProcessor = new SequentialProcessor(sequentialBuffer, maxReviews);
                sequentialProcessor.process();
                break;
            case 2:
                BlockingQueue<String> parallelBuffer = new LinkedBlockingQueue<>();
                ParallelProcessor parallelProcessor = new ParallelProcessor(parallelBuffer, maxReviews, topics);
                parallelProcessor.process();
                break;
            case 3:
                System.out.println("Please use the mpjrun.sh command in terminal.");
                break;
            default:
                System.out.println("Invalid choice. Please restart the program.");
                System.exit(0);
        }

        // end timing, calculate the final time
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("----------------------------------------------------");
        System.out.println("Processing time: " + (duration / 1000.0) + " seconds");
        System.out.println("Average number of reviews per second: " + String.format("%.3f", (double) maxReviews / (duration / 1000.0)));
    }
}