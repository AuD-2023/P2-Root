package p2.storage;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

class MultiIntervalView implements StorageView {

    private final Storage storage;
    private final int totalLength;
    private final Interval[] intervals;

    private byte @Nullable [] data;

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

    private void calculateData() {
        data = new byte[length()];
        int pos = 0;
        for (final Interval interval : intervals) {
            storage.read(interval.start(), data, pos, interval.length());
            pos += interval.length();
        }
    }

    @Override
    public byte[] getData() {
        if (data == null) {
            calculateData();
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
