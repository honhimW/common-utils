package io.github.honhimw;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author hon_him
 * @since 2022-06-08
 */
@SuppressWarnings("unused")
public class RsaUtils {

    public static final String MD5_WITH_RSA = "MD5WithRSA";

    /**
     * TODO NONE/SHA1/SHA384/SHA512...
     */
    public static final String SHA256_WITH_RSA = "SHA256WithRSA";


    /**
     * 使用公钥加密
     */
    public static String encryptWithPublicKey(String content, String publicKey, Charset charset)
        throws GeneralSecurityException {
        Key key = getPublicKeyFromX509(publicKey);
        return encryptWithKey(content, key, charset);
    }

    /**
     * 使用私钥加密
     */
    public static String encryptWithPrivateKey(String content, String privateKey, Charset charset)
        throws GeneralSecurityException {
        PrivateKey key = getPrivateKeyFromPKCS8(privateKey);
        return encryptWithKey(content, key, charset);
    }

    /**
     * 使用密钥加密
     */
    public static String encryptWithKey(String content, Key key, Charset charset)
        throws GeneralSecurityException {
        if (content == null) {
            return null;
        }
        // 对数据加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        if (charset == null) {
            cipher.update(content.getBytes());
        } else {
            cipher.update(content.getBytes(charset));
        }

        byte[] signed = cipher.doFinal();
        return Base64.encodeBase64String(signed);
    }

    /**
     * 使用公钥解密
     */
    public static String decryptWithPublicKey(String content, String publicKey, Charset charset)
        throws GeneralSecurityException {
        Key key = getPublicKeyFromX509(publicKey);
        return decryptWithKey(content, key, charset);
    }

    /**
     * 使用私钥解密
     */
    public static String decryptWithPrivateKey(String content, String privateKey, Charset charset)
        throws GeneralSecurityException {
        Key key = getPrivateKeyFromPKCS8(privateKey);
        return decryptWithKey(content, key, charset);
    }

    /**
     * 使用密钥解密
     */
    public static String decryptWithKey(String content, Key key, Charset charset)
        throws GeneralSecurityException {
        if (content == null) {
            return null;
        }
        try {
            String result = null;
            // 对数据解密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] data = Base64.decodeBase64(content);
            cipher.update(data);
            byte[] signed = cipher.doFinal();
            if (charset != null) {
                result = new String(signed, charset);
            } else {
                result = new String(signed);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static PrivateKey getPrivateKeyFromPKCS8(String privateKey)
        throws GeneralSecurityException {
        if (privateKey == null) {
            return null;
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] encodedKey = Base64.decodeBase64(privateKey);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
    }

    public static PublicKey getPublicKeyFromX509(String publicKey) throws GeneralSecurityException {
        if (publicKey == null) {
            return null;
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] encodedKey = Base64.decodeBase64(publicKey);

        return keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
    }

    /**
     * RSA签名
     *
     * @param type 签名方式,{@link #MD5_WITH_RSA} {@link #SHA256_WITH_RSA}
     * @param content 待签名的字符串
     * @param privateKey rsa私钥字符串
     * @param charset 字符编码
     * @return 签名结果
     */
    public static String sign(String type, String content, String privateKey, Charset charset) {
        if (content == null) {
            return null;
        }
        try {
            PrivateKey priKey = getPrivateKeyFromPKCS8(privateKey);

            Signature signature = Signature.getInstance(type);
            signature.initSign(priKey);
            if (charset == null) {
                signature.update(content.getBytes());
            } else {
                signature.update(content.getBytes(charset));
            }

            byte[] signed = signature.sign();
            return Base64.encodeBase64String(signed);
        } catch (Exception e) {
            e.printStackTrace(); // 签名方式或密钥格式错误才会抛异常, 请在正式上线前排除这类错误
            return null;
        }

    }
}
