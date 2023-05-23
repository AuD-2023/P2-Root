package p2.storage;

/**
 * Interface for encoding and decoding data of a given type to and from bytes.
 *
 * @param <T> the type of data to encode and decode.
 */
public interface DataEncoder<T> {

    /**
     * Returns an instance of the encoder.
     *
     * @return an instance of the encoder.
     */
    DataEncoder<T> getInstance();

    /**
     * Encodes the given data to bytes.
     *
     * @param data the data to encode.
     * @return the encoded data as a byte array.
     */
    byte[] encode(T data);

    /**
     * Decodes the given bytes to data.
     * If the data is not created by this encoder, the result is undefined.
     * Otherwise, the result is the data that was encoded to the given bytes.
     *
     * @param data the bytes to decode.
     * @return the decoded data.
     */
    T decode(byte[] data);

    /**
     * Decodes the data stored in the given {@link StorageView}.
     *
     * @param view the {@link StorageView} to decode.
     * @return the decoded data.
     * @see #decode(byte[])
     */
    default T decode(StorageView view) {
        return decode(view.getData());
    }
}
