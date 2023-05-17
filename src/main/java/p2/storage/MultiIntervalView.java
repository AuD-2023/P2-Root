package p2.storage;

import java.util.stream.Stream;

class MultiIntervalView implements StorageView {

    private final int totalLength;
    private final StorageInterval[] intervals;

    MultiIntervalView(StorageInterval... intervals) {
        totalLength = Stream.of(intervals)
            .map(StorageInterval::interval)
            .mapToInt(Interval::length)
            .sum();
        this.intervals = intervals;
    }

    @Override
    public int length() {
        return totalLength;
    }

    @Override
    public StorageInterval[] getIntervals() {
        return intervals;
    }

    @Override
    public void forEachByte(ByteConsumer consumer) {
        for (StorageInterval tuple : intervals) {
            final int tupleStart = tuple.interval().start();
            final int tupleLength = tuple.interval().length();
            for (int i = 0; i < tupleLength; i++) {
                consumer.accept(
                    tupleStart + i,
                    i,
                    tuple.storage().get(tupleStart + i)
                );
            }
        }
    }

    @Override
    public StorageView plus(StorageView other) {
        final StorageInterval[] otherIntervals = other.getIntervals();
        final StorageInterval[] newIntervals = new StorageInterval[intervals.length + otherIntervals.length];
        System.arraycopy(intervals, 0, newIntervals, 0, intervals.length);
        System.arraycopy(otherIntervals, 0, newIntervals, intervals.length, otherIntervals.length);
        return new MultiIntervalView(newIntervals);
    }
}
