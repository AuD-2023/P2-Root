package p2.storage;

/**
 * Represents a storage that can be read from and written to.
 * This storage is byte-addressable, meaning that each byte has its own address.
 */
public interface Storage {

    /**
     * Creates an unmodifiable view on the data stored at the given intervals.
     * The view is unmodifiable in the sense that it cannot be used to write to the storage.
     * The intervals will be concatenated in the order they are given.
     *
     * @param intervals The intervals that will be used to create the view.
     * @return The created view.
     */
    StorageView createView(Interval... intervals);

    /**
     * Writes the given data to the storage at the given position.
     *
     * @param storagePos The position at which the data will be written.
     * @param source The data that will be written.
     * @param sourcePos The position in the source array at which the data starts.
     * @param length The length of the data that will be written.
     * @throws SegmentationFault If the data is written outside the storage.
     */
    void write(int storagePos, byte[] source, int sourcePos, int length) throws SegmentationFault;

    /**
     * Reads data from the storage at the given position and stores it in the given array.
     *
     * @param storagePos The position at which the data will be read.
     * @param dest The array to which the data will be written.
     * @param destPos The position in the destination array at which the data will start.
     * @param length The length of the data that will be read.
     * @throws SegmentationFault If the read data is outside the storage.
     */
    void read(int storagePos, byte[] dest, int destPos, int length) throws SegmentationFault;

    /**
     * Returns the size of the storage.
     *
     * @return The size of the storage.
     */
    int getSize();
}
