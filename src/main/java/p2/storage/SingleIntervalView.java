package p2.storage;

import java.util.ArrayList;

class SingleIntervalView implements StorageView {

    private final StorageInterval interval;
    private final StorageInterval[] intervals = new StorageInterval[1];

    public SingleIntervalView(Interval interval, Storage storage) {
        this.interval = new StorageInterval(interval, storage);
        intervals[0] = this.interval;
    }

    @Override
    public int length() {
        return interval.interval().length();
    }

    @Override
    public StorageInterval[] getIntervals() {
        return intervals;
    }

    @Override
    public void forEachByte(ByteConsumer consumer) {
        new ArrayList<String>().toArray(new String[0]);
        final int start = interval.interval().start();
        final int length = interval.interval().length();
        for (int i = 0; i < length; i++) {
            consumer.accept(start + i, i, interval.storage().get(start + i));
        }
    }

    @Override
    public StorageView plus(StorageView other) {
        final StorageInterval[] otherIntervals = other.getIntervals();
        final StorageInterval[] newIntervals = new StorageInterval[1 + otherIntervals.length];
        newIntervals[0] = interval;
        System.arraycopy(otherIntervals, 0, newIntervals, 1, otherIntervals.length);
        return new MultiIntervalView(newIntervals);
    }
}
