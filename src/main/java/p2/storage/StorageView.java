package p2.storage;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a view of a {@link Storage} that is limited to a certain interval.
 *
 * <p>
 * Use {@link Storage#createView(Interval...)} to create a view of a storage.
 * </p>
 *
 * <p><b>This view cannot be modified through any instance methods of this class.
 * However, modifications to the underlying {@link Storage} will be reflected in this view</b></p>
 */
@ApiStatus.NonExtendable
public interface StorageView {

    int length();

    /**
     * <b>Immutable. Do not modify this array.</b>
     */
    Interval[] getIntervals();

    /**
     * Returns a new view that is the concatenation of this view and the given view.
     *
     * <p><b>Does not modify this view.</b>
     */
    StorageView plus(StorageView other);

    Storage getStorage();

    byte[] getData();

    /**
     * Concatenates the given views into a single view.
     *
     * <p><b>Does not modify any of the given views.</b></p>
     */
    static StorageView concat(StorageView... views) {
        return switch (views.length) {
            case 0 -> throw new IllegalArgumentException("Cannot concatenate zero views");
            case 1 -> views[0];
            default -> {
                // assert that all views are from the same storage
                if (Stream.of(views).map(StorageView::getStorage)
                    .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())))
                    .size() != 1) {
                    throw new IllegalArgumentException("Cannot concatenate views from different storages");
                }
                final Interval[] intervals = Stream.of(views)
                    .flatMap(view -> Stream.of(view.getIntervals())).toArray(Interval[]::new);
                yield new MultiIntervalView(views[0].getStorage(), intervals);
            }
        };
    }

    @ApiStatus.OverrideOnly
    interface ByteConsumer {
        /**
         * Accepts a byte from a storage view.
         *
         * @param storageIndex  The index in the {@link Storage} space
         * @param intervalIndex The index in the {@link Interval} space
         * @param b             The byte
         */
        void accept(int storageIndex, int intervalIndex, byte b);
    }
}
