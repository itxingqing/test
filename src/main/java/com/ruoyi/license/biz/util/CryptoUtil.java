package com.ruoyi.license.biz.util;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密工具类
 */
@Component
public class CryptoUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_NONCE_LENGTH = 12;

    // 服务器端HKDF盐值（从配置文件注入）
    private static byte[] serverSalt;

    @Value("${license.server.hkdf-salt}")
    private String hkdfSaltBase64;

    @PostConstruct
    public void init() {
        // 从配置文件读取并解码HKDF盐值
        try {
            serverSalt = Base64.getDecoder().decode(hkdfSaltBase64);
            if (serverSalt.length != 64) {
                throw new IllegalArgumentException("HKDF盐值长度必须为64字节");
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化HKDF盐值失败: " + e.getMessage(), e);
        }
    }

    /**
     * HKDF密钥派生
     */
    public static byte[] deriveKey(String hardwareFingerprint, String info) throws Exception {
        if (serverSalt == null) {
            throw new IllegalStateException("HKDF盐值未初始化");
        }

        // 输入密钥材料 = SHA256(硬件指纹)
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] ikm = sha256.digest(hardwareFingerprint.getBytes(StandardCharsets.UTF_8));

        // 使用Bouncy Castle的HKDF
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm, serverSalt, info.getBytes(StandardCharsets.UTF_8)));

        byte[] derivedKey = new byte[32]; // 256 bits
        hkdf.generateBytes(derivedKey, 0, 32);

        return derivedKey;
    }

    /**
     * AES-GCM加密
     */
    public static byte[] aesGcmEncrypt(String plainText, byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, "AES");

        // 生成随机nonce
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        // 加密
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = cipher.doFinal(plainBytes);

        // 组合：nonce + ciphertext (tag已包含在cipherBytes中)
        ByteBuffer byteBuffer = ByteBuffer.allocate(nonce.length + cipherBytes.length);
        byteBuffer.put(nonce);
        byteBuffer.put(cipherBytes);

        return byteBuffer.array();
    }

    /**
     * AES-GCM解密
     */
    public static String aesGcmDecrypt(byte[] encryptedData, byte[] key) throws Exception {
        if (encryptedData.length < GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("加密数据格式错误");
        }

        // 分离nonce和密文
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        byteBuffer.get(nonce);
        byte[] cipherBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherBytes);

        // 解密
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * RSA签名
     */
    public static String rsaSign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * RSA验证签名
     */
    public static boolean rsaVerify(String data, String signatureBase64, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }

    /**
     * 从Base64字符串加载私钥
     */
    public static PrivateKey loadPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 从Base64字符串加载公钥
     */
    public static PublicKey loadPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 计算SHA256哈希
     */
    public static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 生成随机HKDF盐值（64字节）- 仅在初始化时使用一次
     */
    public static String generateHkdfSalt() {
        byte[] salt = new byte[64];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}