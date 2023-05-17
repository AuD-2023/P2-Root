package p2.storage;

import org.jetbrains.annotations.Nullable;

class SingleIntervalView implements StorageView {

    private final Interval interval;
    private final Interval[] intervals = new Interval[1];
    private final Storage storage;
    private byte @Nullable [] data;

    public SingleIntervalView(Storage storage, Interval interval) {
        this.storage = storage;
        this.interval = interval;
        intervals[0] = this.interval;
    }

    @Override
    public int length() {
        return interval.length();
    }

    @Override
    public Interval[] getIntervals() {
        return intervals;
    }

    private void calculateData() {
        data = new byte[length()];
        storage.read(interval.start(), data, 0, interval.length());
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
        final Interval[] newIntervals = new Interval[1 + otherIntervals.length];
        newIntervals[0] = interval;
        System.arraycopy(otherIntervals, 0, newIntervals, 1, otherIntervals.length);
        return new MultiIntervalView(storage, newIntervals);
    }

    @Override
    public Storage getStorage() {
        return storage;
    }
}
