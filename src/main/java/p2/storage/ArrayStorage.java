package p2.storage;

class ArrayStorage implements Storage {

    final byte[] data;

    public ArrayStorage(int size) {
        data = new byte[size];
    }

    @Override
    public StorageView createView(StorageInterval interval) {
        return new SingleIntervalView(this, interval);
    }

    @Override
    public void write(int start, byte[] data) {
        System.arraycopy(data, 0, this.data, start, data.length);
    }
}
