package p2;

import java.util.Iterator;
import java.util.List;

public interface DiskData<T extends DiskData<T>> {

    List<Integer> encode();

    T decode(Iterator<Integer> encodedData);

}
