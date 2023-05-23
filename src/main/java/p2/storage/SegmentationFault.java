package p2.storage;

/**
 * Indicates that the requested byte could not be accessed because it is not in the storage.
 * This happens if the accessed location is negative or greater than or equal to the storage size.
 */
public class SegmentationFault extends RuntimeException {

    /**
     * Creates a new segmentation fault exception.
     *
     * @param storageSize the size of the storage.
     * @param location the location that was accessed.
     */
    SegmentationFault(int storageSize, int location) {
        super("Could no access byte at location %d in a storage of length %d".formatted(location, storageSize));
    }
}
