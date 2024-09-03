import mpi.MPI;
import mpi.MPIException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DistributedManager {

    private final BlockingQueue<String> buffer;
    private final int maxReviews;
    private final String[] topics = {"movies"}; // change the desired topics HERE

    public DistributedManager(BlockingQueue<String> buffer, int maxReviews) {
        this.buffer = buffer;
        this.maxReviews = maxReviews;
    }

    public void start(String[] args) throws MPIException {
        // initialize MPI
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        DistributedProcessor processor = new DistributedProcessor(buffer, maxReviews);

        if (rank == 0) {
            // master node
            System.out.println("Master started with rank " + rank);


            // start timer
            long startTime = System.currentTimeMillis();

            // start WebSocket in a separate thread
            new Thread(() -> {
                WebSocketClientHandler.connectAndReceiveMessages(maxReviews, topics, buffer);
                System.out.println("WebSocket connection closed.");
            }).start();

            // start processing the reviews as they come in
            processor.masterProcess();


            // end timing, calculate the final time
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("----------------------------------------------------");
            System.out.println("Processing time: " + (duration / 1000.0) + " seconds");
            System.out.println("Average number of reviews per second: " + String.format("%.3f", (double) maxReviews / (duration / 1000.0)));
        } else {
            // worker node
            System.out.println("Worker started with rank " + rank);
            processor.workerProcess();
        }

        // finalize MPI
        MPI.Finalize();
    }

    public static void main(String[] args) throws MPIException {
        BlockingQueue<String> buffer = new LinkedBlockingQueue<>(); // shared buffer for reviews
        DistributedManager manager = new DistributedManager(buffer, 1000); // change the num of reviews HERE
        manager.start(args);
    }
}
