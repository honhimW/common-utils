package io.github.honhimw;

import java.security.MessageDigest;

/**
 * @author hon_him
 * @since 2022-06-06
 */
@SuppressWarnings("unused")
public class MD5Utils {

    private static final char[] HEX_DIGITS_LOWER_CAST =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final char[] HEX_DIGITS_UPPER_CAST =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String getMD5(String str) {
        return md5(str, HEX_DIGITS_LOWER_CAST);
    }

    public static String getMD5UpperCast(String str) {
        return md5(str, HEX_DIGITS_UPPER_CAST);
    }
    private static String md5(String str, char[] cast) {
        try {
            byte[] bytes = str.getBytes();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            byte[] digest = messageDigest.digest();
            char[] chars = new char[32];
            int k = 0;
            for (byte b : digest) {
                chars[k++] = cast[b >>> 4 & 0xf];
                chars[k++] = cast[b & 0xf];
            }
            return new String(chars);
        } catch (Exception ignored) {
            return "";
        }
    }

}
