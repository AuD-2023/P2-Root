package p2;

import java.util.List;

public interface AllocationStrategy {

    List<DiskSegment> allocate(int size);

    interface Factory {

        AllocationStrategy create(Disk disk);

    }

}
