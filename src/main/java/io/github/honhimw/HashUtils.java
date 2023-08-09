package io.github.honhimw;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hon_him
 * @since 2022-11-17
 */
@SuppressWarnings("unused")
public class HashUtils {

    public static final String MD2 = "MD2";
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";
    public static final String SHA224 = "SHA224";
    public static final String SHA256 = "SHA256";
    public static final String SHA384 = "SHA384";
    public static final String SHA512 = "SHA512";

    private static final char[] HEX_DIGITS_LOWER_CAST =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final char[] HEX_DIGITS_UPPER_CAST =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final byte[] EMPTY_BYTE_ARR = new byte[0];

    private final Map<String, MessageDigest> messageDigests;

    private final Map<String, byte[]> results;

    private final AtomicBoolean finish = new AtomicBoolean(false);

    private final char[] charset = HEX_DIGITS_LOWER_CAST;

    private HashUtils(Set<String> algorithms) {
        try {
            messageDigests = new HashMap<>(algorithms.size());
            results = new HashMap<>(algorithms.size());
            for (String algorithm : algorithms) {
                messageDigests.put(algorithm, MessageDigest.getInstance(algorithm));
                results.put(algorithm, EMPTY_BYTE_ARR);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static HashUtils newInstance(String... algorithm) {
        return new HashUtils(Set.copyOf(List.of(algorithm)));
    }

    public static HashUtils md5() {
        return newInstance(MD5);
    }

    public static HashUtils sha1() {
        return newInstance(SHA1);
    }

    public static HashUtils sha256() {
        return newInstance(SHA256);
    }

    public static HashUtils sha512() {
        return newInstance(SHA512);
    }

    public HashUtils update(byte[] bytes) {
        if (!messageDigests.isEmpty()) {
            messageDigests.values().forEach(messageDigest -> messageDigest.update(bytes));
        }
        return this;
    }

    public byte[] digest() {
        return digest(messageDigests.keySet().stream().findFirst().orElse(null));
    }

    public byte[] digest(String algorithm) {
        if (!finish.get()) {
            doDigest();
        }
        return results.getOrDefault(algorithm, EMPTY_BYTE_ARR);
    }

    private synchronized void doDigest() {
        if (!finish.get()) {
            if (!messageDigests.isEmpty()) {
                messageDigests.forEach((alg, messageDigest) -> {
                    byte[] digest = messageDigest.digest();
                    results.put(alg, digest);
                });
            }
            finish.compareAndSet(false, true);
        }
    }

    public String toHexString() {
        byte[] digest = this.digest();
        return toHexString(digest, charset);
    }

    public String toHexString(String algorithm) {
        byte[] digest = this.digest(algorithm);
        return toHexString(digest, charset);
    }

    public static String toHexString(byte[] digest) {
        return toHexString(digest, HEX_DIGITS_LOWER_CAST);
    }

    public static String toHexString(byte[] digest, char[] charset) {
        int length = digest.length;
        char[] chars = new char[length << 1];
        for (int i = 0, j = 0; i < length; i++) {
            chars[j++] = charset[(0xF0 & digest[i]) >>> 4];
            chars[j++] = charset[0x0F & digest[i]];
        }
        return new String(chars);
    }

}
