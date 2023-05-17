package p2.storage;

class EmptyStorageView implements StorageView {

    static EmptyStorageView INSTANCE = new EmptyStorageView();

    private EmptyStorageView() {
        throw new AssertionError("This class should not be instantiated");
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public void forEachByte(final ByteConsumer consumer) {
    }

    @Override
    public StorageView plus(final StorageView other) {
        return other;
    }
}
