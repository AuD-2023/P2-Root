package p2.storage;

import java.nio.charset.StandardCharsets;

public class StringDecoder implements DataDecoder<String> {

    @Override
    public String decode(StorageView data) {
        return new String(data.getData(), StandardCharsets.UTF_8);
    }
}
