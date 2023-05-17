package p2;

import p2.storage.Interval;

import java.util.List;

public interface AllocationStrategy {

    List<Interval> allocate(int size);

    interface Factory {

        AllocationStrategy create(FileSystem disk);

    }

}
