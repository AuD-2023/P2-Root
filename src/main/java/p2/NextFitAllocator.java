package p2;

import p2.storage.FileSystem;
import p2.storage.Interval;

import java.util.LinkedList;
import java.util.List;

public class NextFitAllocator implements AllocationStrategy {

    private final boolean[] used;

    private int lastFit = -1;

    public NextFitAllocator(FileSystem fileSystem) {
        used = new boolean[fileSystem.getSize()];
    }

    @Override
    public List<Interval> allocate(int size) {

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

                if (end - start + 1 == 2) { //TODO remove; just for testing
                    break;
                }

                end++;
            }

            intervals.add(new Interval(start, end - start + 1));

        }

        return intervals;
    }

}
