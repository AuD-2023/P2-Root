package p2.storage;

import java.util.stream.Stream;

/**
 * A view of multiple intervals in a storage.
 */
class MultiIntervalView implements StorageView {

    /**
     * The underlying storage.
     */
    private final Storage storage;

    /**
     * The total length of all intervals combined.
     */
    private final int totalLength;

    /**
     * The intervals that this view is limited to.
     */
    private final Interval[] intervals;

    /**
     * Creates a new {@link MultiIntervalView} instance.
     *
     * @param storage the underlying storage.
     * @param intervals the intervals that this view is limited to.
     */
    MultiIntervalView(Storage storage, Interval... intervals) {
        this.storage = storage;
        totalLength = Stream.of(intervals)
            .mapToInt(Interval::length)
            .sum();
        this.intervals = intervals;
    }

    @Override
    public int length() {
        return totalLength;
    }

    @Override
    public Interval[] getIntervals() {
        return intervals;
    }

    @Override
    public byte[] getData() {
        byte[] data = new byte[length()];
        int pos = 0;
        for (final Interval interval : intervals) {
            storage.read(interval.start(), data, pos, interval.length());
            pos += interval.length();
        }
        return data;
    }

    @Override
    public StorageView plus(StorageView other) {
        final Interval[] otherIntervals = other.getIntervals();
        final Interval[] newIntervals = new Interval[intervals.length + otherIntervals.length];
        System.arraycopy(intervals, 0, newIntervals, 0, intervals.length);
        System.arraycopy(otherIntervals, 0, newIntervals, intervals.length, otherIntervals.length);
        return new MultiIntervalView(storage, newIntervals);
    }

    @Override
    public Storage getStorage() {
        return storage;
    }
}
