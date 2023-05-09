package p2;

import p2.storage.StorageInterval;

import java.util.List;

public interface AllocationStrategy {

    List<StorageInterval> allocate(int size);

    interface Factory {

        AllocationStrategy create(FileSystem disk);

    }

}
