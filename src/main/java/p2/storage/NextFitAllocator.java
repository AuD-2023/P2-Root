package p2.storage;

import java.util.LinkedList;
import java.util.List;

/**
 * An {@linkplain AllocationStrategy allocation strategy} that works by iterating over the array and starting at the
 * last interval that was allocated. It then returns the next possible intervals.
 *
 */
public class NextFitAllocator implements AllocationStrategy {

    /**
     * The array that is used to mark intervals as used.
     */
    private final boolean[] used;

    /**
     * The index of the last interval that was allocated.
     */
    private int lastFit = -1;

    /**
     * The maximum size of an interval that will be allocated.
     */
    private int maxIntervalSize = Integer.MAX_VALUE;

    /**
     * Creates a new {@link NextFitAllocator} instance.
     *
     * @param used The array that will be used to mark intervals as used.
     */
    public NextFitAllocator(boolean[] used) {
        this.used = used;
    }

    @Override
    public void setMaxIntervalSize(int size) {
        maxIntervalSize = size;
    }

    @Override
    public List<Interval> allocate(int size) throws NoDiskSpaceException {

        int i = lastFit + 1;
        int totalSize = 0;

        List<Interval> intervals = new LinkedList<>();

        outer: while (true) {

            while (used[i]) {
                i++;
                if (i == used.length) {
                    i = 0;
                }
                if (i == lastFit) {
                    throw new NoDiskSpaceException();
                }
            }

            int start = i;
            int end = i;

            while (!used[end]) {
                used[end] = true;
                totalSize++;


                if (end == used.length - 1 || totalSize == size) {
                    lastFit = end;
                    intervals.add(new Interval(start, end - start + 1));
                    break outer;
                }

                if (end - start + 1 == maxIntervalSize) {
                    break;
                }

                end++;
            }

            intervals.add(new Interval(start, end - start + 1));

        }

        return intervals;
    }

}
