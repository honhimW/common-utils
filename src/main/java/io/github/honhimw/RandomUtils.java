package io.github.honhimw;

import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * {@link RandomStringUtils}
 *
 * @author hon_him
 * @since 2022-06-14
 */
@SuppressWarnings("unused")
public class RandomUtils {

    /**
     * 不要修改数组成员
     */
    public static final char[] NUMBER_LETTER_CHARS;
    public static final char[] NUMBER_CHARS;
    public static final char[] LOWER_LETTER_CHARS;
    public static final char[] UPPER_LETTER_CHARS;
    public static final char[] LETTER_CHARS;
    public static final char[] SYMBOL_CHARS;

    private static final int count;

    private static final Map<Character, boolean[]> HEXA_DECIMAL_CHARS_MAP = new HashMap<>();

    private static volatile int DEFAULT_LENGTH = 10;

    public static void setDefaultLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("长度不能小于1");
        }
        DEFAULT_LENGTH = length;
    }

    static {
        NUMBER_LETTER_CHARS = new char[]{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z'
        };

        NUMBER_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        LOWER_LETTER_CHARS = new char[]{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        };

        UPPER_LETTER_CHARS = new char[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
        };

        LETTER_CHARS = new char[]{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
        };

        SYMBOL_CHARS = new char[]{'!', '@', '#', '$', '%', '^', '&', '-', '_', '=', '+', '\'', '"', ';', ':'};

        count = NUMBER_LETTER_CHARS.length;
        boolean[] _0 = new boolean[]{false, false, false, false};
        boolean[] _1 = new boolean[]{false, false, false, true};
        boolean[] _2 = new boolean[]{false, false, true, false};
        boolean[] _3 = new boolean[]{false, false, true, true};
        boolean[] _4 = new boolean[]{false, true, false, false};
        boolean[] _5 = new boolean[]{false, true, false, true};
        boolean[] _6 = new boolean[]{false, true, true, false};
        boolean[] _7 = new boolean[]{false, true, true, true};
        boolean[] _8 = new boolean[]{true, false, false, false};
        boolean[] _9 = new boolean[]{true, false, false, true};
        boolean[] _a = new boolean[]{true, false, true, false};
        boolean[] _b = new boolean[]{true, false, true, true};
        boolean[] _c = new boolean[]{true, true, false, false};
        boolean[] _d = new boolean[]{true, true, false, true};
        boolean[] _e = new boolean[]{true, true, true, false};
        boolean[] _f = new boolean[]{true, true, true, true};
        HEXA_DECIMAL_CHARS_MAP.put('0', _0);
        HEXA_DECIMAL_CHARS_MAP.put('1', _1);
        HEXA_DECIMAL_CHARS_MAP.put('2', _2);
        HEXA_DECIMAL_CHARS_MAP.put('3', _3);
        HEXA_DECIMAL_CHARS_MAP.put('4', _4);
        HEXA_DECIMAL_CHARS_MAP.put('5', _5);
        HEXA_DECIMAL_CHARS_MAP.put('6', _6);
        HEXA_DECIMAL_CHARS_MAP.put('7', _7);
        HEXA_DECIMAL_CHARS_MAP.put('8', _8);
        HEXA_DECIMAL_CHARS_MAP.put('9', _9);
        HEXA_DECIMAL_CHARS_MAP.put('a', _a);
        HEXA_DECIMAL_CHARS_MAP.put('b', _b);
        HEXA_DECIMAL_CHARS_MAP.put('c', _c);
        HEXA_DECIMAL_CHARS_MAP.put('d', _d);
        HEXA_DECIMAL_CHARS_MAP.put('e', _e);
        HEXA_DECIMAL_CHARS_MAP.put('f', _f);
    }

    /**
     * 默认生成长度10位
     */
    public static String randomId() {
        return randomId(DEFAULT_LENGTH);
    }

    /**
     * 不限长度, {@link ThreadLocalRandom}
     *
     * @param length 长度
     */
    public static String randomId(int length) {
        return randomId(length, NUMBER_LETTER_CHARS);
    }

    public static String randomId(int length, char[] chars) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] cs = new char[length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = chars[random.nextInt(chars.length)];
        }
        return new String(cs);
    }

    /**
     * 默认生成长度10位大小写
     */
    public static String shortUUID() {
        char[] uuidChar = new char[10];
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 10; i++) {
            String str = uuid.substring(i * 3, i * 3 + 3);
            int x = Integer.parseInt(str, 16);
            uuidChar[i] = NUMBER_LETTER_CHARS[x % count];
        }
        return String.valueOf(uuidChar);
    }

    /**
     * {@link UUID}随机生成再转换生成长度[1,21]
     *
     * @param length 长度
     */
    public static String fixedUUID(int length) {
        if (length < 1 || length > 21) {
            throw new IllegalArgumentException("UUID长度范围[1,21]");
        }
        char[] bits = new char[6];
        String uuid = UUID.randomUUID().toString();
        char[] cs = uuid.toCharArray();
        int site = 0;
        char[] css = new char[length];
        int cursor = 0;
        for (char c : cs) {
            boolean[] booleans = HEXA_DECIMAL_CHARS_MAP.get(c);
            if (booleans == null) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                bits[site++] = booleans[i] ? '1' : '0';
                if (site == 6) {
                    int j = Integer.parseInt(String.valueOf(bits), 2);
                    j = j >= count ? j - count : j;
                    css[cursor++] = NUMBER_LETTER_CHARS[j];
                    site = 0;
                }
            }
            if (cursor == length) {
                break;
            }
        }
        return String.valueOf(css);
    }

    private static final Random random;

    static {
        SecureRandom secureRandom = new SecureRandom();
        byte[] seed = new byte[8];
        secureRandom.nextBytes(seed);
        random = new Random(new BigInteger(seed).longValue());
    }

    public static UUID uuid() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        long mostSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (randomBytes[i] & 0xff);
        }

        long leastSigBits = 0;
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xff);
        }
        return new UUID(mostSigBits, leastSigBits);
    }

    public static RandomGenerator random() {
        return ThreadLocalRandom.current();
    }

}
