package p2.storage;

public record Interval(int start, int length) {

    public Interval {
        if (start < 0) {
            throw new IllegalArgumentException("start must be non-negative");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
    }

}
