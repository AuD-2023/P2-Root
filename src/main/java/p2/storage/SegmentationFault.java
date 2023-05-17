package p2.storage;

public class SegmentationFault extends RuntimeException {

    SegmentationFault(StorageView view, int location) {
        super("Could not read byte at location " + location + " in view of length " + view.length());
    }
}
