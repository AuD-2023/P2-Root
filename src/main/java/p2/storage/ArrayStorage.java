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
            case 0 -> StorageView.EMPTY;
            case 1 -> new SingleIntervalView(intervals[0], this);
            default -> {
                final StorageInterval[] storageIntervals = new StorageInterval[intervals.length];
                for (int i = 0; i < intervals.length; i++) {
                    storageIntervals[i] = new StorageInterval(intervals[i], this);
                }
                yield new MultiIntervalView(storageIntervals);
            }
        };
    }

    @Override
    public void write(int start, byte[] data, int length) {
        System.arraycopy(data, 0, this.data, start, length);
    }

    @Override
    public byte get(int index) {
        return data[index];
    }

    @Override
    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }
}
