package p2.storage;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a view of a {@link Storage} that is limited to a certain interval.
 *
 * <p>
 * Use {@link Storage#createView(StorageInterval)} to create a view of a storage.
 * </p>
 *
 * <p><b>This view cannot be modified through any instance methods of this class.
 * However, modifications to the underlying {@link Storage} will be reflected in this view</b></p>
 */
@ApiStatus.NonExtendable
public interface StorageView {

    StorageView EMPTY = new StorageView() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public byte get(int index) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
        }

        @Override
        public void forEachByte(ByteConsumer consumer) {
        }

        @Override
        public StorageView plus(StorageView other) {
            return other;
        }

        @Override
        public StorageView slice(StorageInterval interval) {
            return this;
        }
    };

    int size();

    byte get(int index);

    void forEachByte(ByteConsumer consumer);

    /**
     * Returns a new view that is the concatenation of this view and the given view.
     *
     * <p><b>Does not modify this view.</b>
     */
    StorageView plus(StorageView other);

    /**
     * Returns a new view of the given interval of this view.
     *
     * <p><b>Does not modify this view.</b>
     */
    StorageView slice(StorageInterval interval);

    default byte[] copyToByteArray() {
        final byte[] result = new byte[size()];
        forEachByte((ignored, readIndex, b) -> result[readIndex] = b);
        return result;
    }

    /**
     * Concatenates the given views into a single view.
     *
     * <p><b>Does not modify any of the given views.</b>
     */
    static StorageView concat(StorageView... views) {
        return null;
    }

    @ApiStatus.OverrideOnly
    interface ByteConsumer {
        void accept(int storageIndex, int readIndex, byte b);
    }
}
