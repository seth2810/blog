private final ExecutorService threadPool;
private final String[] knownWords;
private final int blockSize;

public ThreadPoolDistance(String[] words, int block) {
    threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    knownWords = words;
    blockSize = block;
}

public DistancePair bestMatch(String target) {

    // build a list of tasks for matching to ranges of known words
    List<DistanceTask> tasks = new ArrayList<DistanceTask>();

    int size = 0;
    for (int base = 0; base < knownWords.length; base += size) {
        size = Math.min(blockSize, knownWords.length - base);
        tasks.add(new DistanceTask(target, base, size));
    }
    DistancePair best;
    try {

        // pass the list of tasks to the executor, getting back list of futures
        List<Future<DistancePair>> results = threadPool.invokeAll(tasks);

        // find the best result, waiting for each future to complete
        best = DistancePair.WORST_CASE;
        for (Future<DistancePair> future: results) {
            DistancePair result = future.get();
            best = DistancePair.best(best, result);
        }

    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } catch (ExecutionException e) {
        throw new RuntimeException(e);
    }
    return best;
}

/**
 * Shortest distance task implementation using Callable.
 */
public class DistanceTask implements Callable<DistancePair>
{
    private final String targetText;
    private final int startOffset;
    private final int compareCount;

    public DistanceTask(String target, int offset, int count) {
        targetText = target;
        startOffset = offset;
        compareCount = count;
    }

    private int editDistance(String word, int[] v0, int[] v1) {
        ...
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public DistancePair call() throws Exception {

        // directly compare distances for comparison words in range
        int[] v0 = new int[targetText.length() + 1];
        int[] v1 = new int[targetText.length() + 1];
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        boolean single = false;
        for (int i = 0; i < compareCount; i++) {
            int distance = editDistance(knownWords[i + startOffset], v0, v1);
            if (bestDistance > distance) {
                bestDistance = distance;
                bestIndex = i + startOffset;
                single = true;
            } else if (bestDistance == distance) {
                single = false;
            }
        }
        return single ? new DistancePair(bestDistance, knownWords[bestIndex]) :
            	new DistancePair(bestDistance);
    }
}