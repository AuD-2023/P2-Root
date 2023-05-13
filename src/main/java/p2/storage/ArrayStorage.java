package p2.storage;

import p2.AllocationStrategy;

class ArrayStorage implements Storage {

    final byte[] data;
    AllocationStrategy allocationStrategy;

    public ArrayStorage(int size, AllocationStrategy allocationStrategy) {
        data = new byte[size];
        this.allocationStrategy = allocationStrategy;
    }

    @Override
    public StorageView createView(StorageInterval interval) {
        return new SingleIntervalView(this, interval);
    }

    @Override
    public void write(int start, byte[] data, int length) {
        System.arraycopy(data, 0, this.data, start, length);
    }

    @Override
    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }
}
