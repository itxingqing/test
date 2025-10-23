package com.ruoyi.license.biz.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 许可证密钥生成工具
 */
public class LicenseKeyGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成许可证密钥（格式：XXXX-XXXX-XXXX-XXXX）
     */
    public static String generateLicenseKey() {
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                key.append("-");
            }
            for (int j = 0; j < 4; j++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                key.append(CHARACTERS.charAt(index));
            }
        }

        return key.toString();
    }

    /**
     * 生成UUID格式的许可证密钥
     */
    public static String generateUuidLicenseKey() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * 生成指定长度的许可证密钥
     */
    public static String generateLicenseKey(int segments, int charsPerSegment) {
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < segments; i++) {
            if (i > 0) {
                key.append("-");
            }
            for (int j = 0; j < charsPerSegment; j++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                key.append(CHARACTERS.charAt(index));
            }
        }

        return key.toString();
    }

    /**
     * 验证许可证密钥格式
     */
    public static boolean isValidFormat(String licenseKey) {
        if (licenseKey == null || licenseKey.isEmpty()) {
            return false;
        }

        // 格式：XXXX-XXXX-XXXX-XXXX
        String pattern = "^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$";
        return licenseKey.matches(pattern);
    }

    /**
     * 测试生成
     */
    public static void main(String[] args) {
        System.out.println("=== 许可证密钥生成 ===\n");

        // 生成10个示例密钥
        for (int i = 0; i < 10; i++) {
            String key = generateLicenseKey();
            System.out.println((i + 1) + ". " + key);
        }

        System.out.println("\nUUID格式密钥：");
        System.out.println(generateUuidLicenseKey());

        System.out.println("\n自定义格式（5段，每段6字符）：");
        System.out.println(generateLicenseKey(5, 6));
    }
}