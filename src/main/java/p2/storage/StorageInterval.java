package p2.storage;

public record StorageInterval(int start, int length) {

    public StorageInterval {
        if (start < 0) {
            throw new IllegalArgumentException("start must be non-negative");
        }
    }

    public boolean contains(int index) {
        return index >= start && index < (start + length);
    }
}
