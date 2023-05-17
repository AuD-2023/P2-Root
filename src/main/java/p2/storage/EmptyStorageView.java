package p2.storage;

public class EmptyStorageView implements StorageView {

    private final Storage storage;
    private final Interval[] intervals = new Interval[0];
    private final byte[] data = new byte[0];

    public EmptyStorageView(Storage storage) {
        this.storage = storage;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Interval[] getIntervals() {
        return intervals;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public StorageView plus(StorageView other) {
        return other;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }
}
