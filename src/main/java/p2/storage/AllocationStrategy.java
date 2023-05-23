package p2.storage;

import java.util.List;

/**
 * Describes an allocation strategy that can be used by a storage. It is responsible for finding
 * an interval or multiple intervals that can be used to store data of a given length.
 */
public interface AllocationStrategy {

    /**
     * A {@link Factory} that creates a {@link NextFitAllocator}.
     */
    AllocationStrategy.Factory NEXT_FIT = NextFitAllocator::new;

    /**
     * Finds an interval or multiple intervals that can be used to store data of the given length.
     * The intervals are not marked as used.
     * The intervals will not be in any particular order, but they will not overlap.
     * The size of the intervals will be at most the maximum interval size set by {@link #setMaxIntervalSize(int)}.
     * Initially the maximum interval size is {@link Integer#MAX_VALUE}.
     *
     * @param size The length of the data to be stored.
     * @return The interval(s) that can be used to store the data.
     * @throws NoDiskSpaceException If there is not enough space to store the data.
     */
    List<Interval> allocate(int size) throws NoDiskSpaceException;

    /**
     * Sets the maximum size of an interval that will be allocated.
     *
     * @param size the new maximum size.
     */
    void setMaxIntervalSize(int size);

    /**
     * A factory for creating new {@linkplain  AllocationStrategy allocation strategies}.
     */
    interface Factory {

        /**
         * Creates a new {@linkplain  AllocationStrategy allocation strategy}.
         *
         * @param used The array that will be used to mark intervals as used.
         * @return The new {@linkplain  AllocationStrategy allocation strategy}.
         */
        AllocationStrategy create(boolean[] used);

    }

}
