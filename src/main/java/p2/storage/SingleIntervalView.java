package p2.storage;

class SingleIntervalView implements StorageView {

    private final ArrayStorage baseStorage;
    final Interval interval;

    public SingleIntervalView(ArrayStorage baseStorage, Interval interval) {
        this.baseStorage = baseStorage;
        this.interval = interval;
    }

    @Override
    public int length() {
        return baseStorage.data.length;
    }

    @Override
    public byte get(int index) {
        if (index < 0 || index >= interval.length()) {
            throw new SegmentationFault(this, index);
        }
        return baseStorage.data[index];
    }

    @Override
    public void forEachByte(ByteConsumer consumer) {
        for (int i = 0; i < baseStorage.data.length; i++) {
            consumer.accept(i, i, baseStorage.data[i]);
        }
    }

    @Override
    public StorageView plus(StorageView other) {
        if (other instanceof MultiIntervalView otherTree) {
            // flatten resulting tree by adding other's children instead of the tree itself
            StorageInterval[] intervals = new StorageInterval[1 + otherTree.getChildren().length];
            intervals[0] = StorageInterval.ofEntire(this);
            System.arraycopy(otherTree.getChildren(), 0, intervals, 1, otherTree.getChildren().length);
            return new MultiIntervalView(intervals);
        }

        // create a new node with two children: this and other
        return new MultiIntervalView(new StorageInterval[]{
            StorageInterval.ofEntire(this),
            StorageInterval.ofEntire(other)
        });
    }

    @Override
    public StorageView slice(Interval interval) {
        return new SingleIntervalView(baseStorage, interval);
    }

    @Override
    public byte[] copyToByteArray() {
        return baseStorage.data.clone();
    }

}
