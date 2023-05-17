package p2.storage;

import org.jetbrains.annotations.ApiStatus;

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

    StorageView EMPTY = new StorageView() {
        @Override
        public int length() {
            return 0;
        }

        @Override
        public StorageInterval[] getIntervals() {
            return new StorageInterval[0];
        }

        @Override
        public void forEachByte(ByteConsumer consumer) {
        }

        @Override
        public StorageView plus(StorageView other) {
            return other;
        }
    };

    int length();

    /**
     * <b>Immutable. Do not modify this array.</b>
     */
    StorageInterval[] getIntervals();

    void forEachByte(ByteConsumer consumer);

    /**
     * Returns a new view that is the concatenation of this view and the given view.
     *
     * <p><b>Does not modify this view.</b>
     */
    StorageView plus(StorageView other);

    default byte[] copyToByteArray() {
        final byte[] result = new byte[length()];
        forEachByte((ignored, readIndex, b) -> result[readIndex] = b);
        return result;
    }

    /**
     * Concatenates the given views into a single view.
     *
     * <p><b>Does not modify any of the given views.</b>
     */
    static StorageView concat(StorageView... views) {
        return switch (views.length) {
            case 0 -> EmptyStorageView.INSTANCE;
            case 1 -> views[0];
            default -> {
                final StorageInterval[] intervals = Stream.of(views)
                    .flatMap(view -> Stream.of(view.getIntervals())).toArray(StorageInterval[]::new);
                yield new MultiIntervalView(intervals);
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
