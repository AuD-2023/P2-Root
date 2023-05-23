package p2.storage;

/**
 * An interval with a start and a length.
 * The start must be non-negative and the length must be positive.
 *
 * @param start the start.
 * @param length the length.
 */
public record Interval(int start, int length) {

    /**
     * Creates a new interval with the given start and length.
     *
     * @param start the start.
     * @param length the length.
     */
    public Interval {
        if (start < 0) {
            throw new IllegalArgumentException("start must be non-negative");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
    }

}
