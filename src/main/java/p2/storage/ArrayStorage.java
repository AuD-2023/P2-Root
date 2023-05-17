package p2.storage;

import p2.AllocationStrategy;

class ArrayStorage implements Storage {

    private final byte[] data;
    private final AllocationStrategy allocationStrategy;

    public ArrayStorage(int size, AllocationStrategy allocationStrategy) {
        data = new byte[size];
        this.allocationStrategy = allocationStrategy;
    }

    @Override
    public StorageView createView(Interval... intervals) {
        return switch (intervals.length) {
            case 0 -> new EmptyStorageView(this);
            case 1 -> new SingleIntervalView(this, intervals[0]);
            default -> new MultiIntervalView(this, intervals);
        };
    }

    @Override
    public void write(int storagePos, byte[] source, int sourcePos, int length) {
        System.arraycopy(source, sourcePos, data, storagePos, length);
    }

    @Override
    public void read(int storageStart, byte[] dest, int destPos, int length) {
        System.arraycopy(this.data, storageStart, dest, destPos, length);
    }

    @Override
    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }
}
