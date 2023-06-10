package p2.storage;

import java.nio.charset.StandardCharsets;

/**
 * Encodes and decodes strings to and from bytes.
 */
public class StringEncoder implements DataEncoder<String> {

    /**
     * The singleton instance of the class.
     */
    public static final StringEncoder INSTANCE = new StringEncoder();

    /**
     * Creates a new string encoder.
     * This constructor is private to prevent instantiation.
     * To get an instance of the class, use {@link #getInstance()}.
     */
    private StringEncoder() {
    }

    @Override
    public DataEncoder<String> getInstance() {
        return INSTANCE;
    }

    @Override
    public byte[] encode(String data) {

        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String decode(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }
}
