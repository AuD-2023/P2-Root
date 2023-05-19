package p2.storage;

import java.nio.charset.StandardCharsets;

public class StringDecoder implements DataDecoder<String> {

    @Override
    public String decode(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }
}
