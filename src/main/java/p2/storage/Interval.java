package p2.storage;

public record Interval(int start, int length) {

    public Interval {
        if (start < 0) {
            throw new IllegalArgumentException("start must be non-negative");
        }
    }

    public boolean contains(int index) {
        return index >= start && index < (start + length);
    }
}
