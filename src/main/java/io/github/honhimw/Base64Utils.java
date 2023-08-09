package io.github.honhimw;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author hon_him
 * @since 2022-06-09
 */
@SuppressWarnings("unused")
public class Base64Utils {

    public static String toBASE64(String string) {
        return toBASE64(string, UTF_8);
    }

    public static String toBASE64(String string, Charset charset) {
        return toBASE64(string.getBytes(charset));
    }

    public static String fromBASE64(String base64String) {
        return fromBASE64(base64String, UTF_8);
    }

    public static String fromBASE64(String base64String, Charset charset) {
        return new String(Base64.decodeBase64(base64String), charset);
    }

    public static String toBASE64(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public static byte[] decode(String base64String) {
        return Base64.decodeBase64(base64String);
    }

}
