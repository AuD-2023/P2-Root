package p2.storage;

/**
 * A view of a single interval in a storage.
 */
class SingleIntervalView implements StorageView {

    /**
     * The interval that this view is limited to.
     */
    private final Interval interval;

    /**
     * An array that holds the single interval.
     */
    private final Interval[] intervals = new Interval[1];

    /**
     * The underlying storage.
     */
    private final Storage storage;

    /**
     * Creates a new {@link SingleIntervalView} instance.
     *
     * @param storage the underlying storage.
     * @param interval the interval that this view is limited to.
     */
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

    @Override
    public byte[] getData() {
        byte[] data = new byte[length()];
        storage.read(interval.start(), data, 0, interval.length());
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
