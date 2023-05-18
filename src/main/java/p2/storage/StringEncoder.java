package p2.storage;

import java.nio.charset.StandardCharsets;

public class StringEncoder implements DataEncoder<String> {

    @Override
    public byte[] encode(String data) {

        return data.getBytes(StandardCharsets.UTF_8);
    }
}
