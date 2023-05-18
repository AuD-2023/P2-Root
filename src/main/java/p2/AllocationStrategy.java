package p2;

import p2.storage.FileSystem;
import p2.storage.Interval;

import java.util.List;

public interface AllocationStrategy {

    AllocationStrategy.Factory NEXT_FIT = NextFitAllocator::new;

    List<Interval> allocate(int size);

    interface Factory {

        AllocationStrategy create(FileSystem disk);

    }

}
