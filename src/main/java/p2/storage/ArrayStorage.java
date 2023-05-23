package p2.storage;

/**
 * A storage that stores data in a byte array.
 */
class ArrayStorage implements Storage {

    /**
     * The byte array that stores the data.
     */
    private final byte[] data;

    /**
     * Creates a new storage with the given size.
     *
     * @param size the size of the storage.
     */
    public ArrayStorage(int size) {
        data = new byte[size];
    }

    @Override
    public StorageView createView(Interval... intervals) {
        return switch (intervals.length) {
            case 0 -> new EmptyStorageView(this);
            case 1 -> new SingleIntervalView(this, intervals[0]);
            default -> new MultiIntervalView(this, intervals);
        };
    }

    @Override
    public void write(int storagePos, byte[] source, int sourcePos, int length) throws SegmentationFault {
        checkAccess(storagePos, length);

        System.arraycopy(source, sourcePos, data, storagePos, length);
    }

    @Override
    public void read(int storageStart, byte[] dest, int destPos, int length) throws SegmentationFault {
        checkAccess(storageStart, length);

        System.arraycopy(this.data, storageStart, dest, destPos, length);
    }

    @Override
    public int getSize() {
        return data.length;
    }

    private void checkAccess(int start, int length) throws SegmentationFault {
        if (start < 0) {
            throw new SegmentationFault(getSize(), start);
        }
        if (start + length > getSize()) {
            throw new SegmentationFault(getSize(), start + length);
        }
    }
}
