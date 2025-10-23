package com.ruoyi.license.biz.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密钥生成工具
 * 
 * 使用说明：
 * 1. 运行此工具生成RSA密钥对和HKDF盐值（仅在首次部署时运行一次）
 * 2. 将输出的私钥和盐值保存到服务器的application.yml配置文件
 * 3. 将输出的公钥转换为XML格式后配置到客户端CryptoHelper.cs
 * 4. 妥善保管私钥和盐值，不要泄露！
 */
public class KeyGenerator {
    
    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("              许可证服务器 - 密钥初始化工具");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("⚠️  重要提示：");
        System.out.println("   1. 此工具仅在首次部署服务器时运行一次");
        System.out.println("   2. 生成后请妥善保管所有密钥，不要泄露");
        System.out.println("   3. 建议将密钥保存到密码管理器或加密存储");
        System.out.println("   4. 不要将密钥提交到代码仓库");
        System.out.println();
        System.out.println("================================================================================");
        System.out.println();
        
        // ====================================
        // 步骤1：生成RSA密钥对（2048位）
        // ====================================
        System.out.println("步骤 1/3：生成RSA密钥对（2048位）...");
        System.out.println();
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // 导出私钥（PKCS8格式）
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyBytes);
        
        // 导出公钥（X509格式）
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);
        
        System.out.println("✅ RSA密钥对生成成功！");
        System.out.println();
        
        // ====================================
        // 步骤2：生成HKDF盐值（64字节）
        // ====================================
        System.out.println("步骤 2/3：生成HKDF盐值（64字节）...");
        System.out.println();
        
        byte[] salt = new byte[64];
        new SecureRandom().nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        
        System.out.println("✅ HKDF盐值生成成功！");
        System.out.println();
        
        // ====================================
        // 步骤3：输出配置信息
        // ====================================
        System.out.println("步骤 3/3：配置说明");
        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                          服务器端配置");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("请将以下内容添加到服务器端的 application.yml 文件中：");
        System.out.println();
        System.out.println("license:");
        System.out.println("  server:");
        System.out.println("    # RSA私钥（PKCS8格式，Base64编码）");
        System.out.println("    private-key: " + privateKeyBase64);
        System.out.println();
        System.out.println("    # HKDF盐值（64字节，Base64编码）");
        System.out.println("    hkdf-salt: " + saltBase64);
        System.out.println();
        System.out.println("或者使用环境变量（推荐用于生产环境）：");
        System.out.println();
        System.out.println("export LICENSE_SERVER_PRIVATE_KEY=\"" + privateKeyBase64 + "\"");
        System.out.println("export LICENSE_SERVER_SALT=\"" + saltBase64 + "\"");
        System.out.println();
        System.out.println("然后在application.yml中引用：");
        System.out.println("license:");
        System.out.println("  server:");
        System.out.println("    private-key: ${LICENSE_SERVER_PRIVATE_KEY}");
        System.out.println("    hkdf-salt: ${LICENSE_SERVER_SALT}");
        System.out.println();
        
        System.out.println("================================================================================");
        System.out.println("                          客户端配置");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("步骤1：公钥（Base64格式，需要转换）");
        System.out.println("--------------------------------------");
        System.out.println(publicKeyBase64);
        System.out.println();
        System.out.println("步骤2：使用PublicKeyConverter.cs工具转换公钥");
        System.out.println("--------------------------------------");
        System.out.println("1. 复制上面的公钥Base64字符串");
        System.out.println("2. 运行客户端的PublicKeyConverter.exe");
        System.out.println("3. 粘贴公钥Base64字符串，工具会自动转换为XML格式");
        System.out.println("4. 将转换后的XML格式公钥配置到客户端CryptoHelper.cs的SERVER_PUBLIC_KEY常量");
        System.out.println();
        System.out.println("示例（客户端CryptoHelper.cs）：");
        System.out.println("--------------------------------------");
        System.out.println("private const string SERVER_PUBLIC_KEY = @\"");
        System.out.println("<RSAKeyValue>");
        System.out.println("  <Modulus>转换后的模数...</Modulus>");
        System.out.println("  <Exponent>AQAB</Exponent>");
        System.out.println("</RSAKeyValue>\";");
        System.out.println();
        
        System.out.println("================================================================================");
        System.out.println("                          安全提示");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("🔒 私钥和盐值安全：");
        System.out.println("   ✓ 私钥只能存在于服务器端，绝不能泄露");
        System.out.println("   ✓ HKDF盐值同样只能存在于服务器端");
        System.out.println("   ✓ 建议使用环境变量而不是直接写在配置文件中");
        System.out.println("   ✓ 不要将包含私钥的配置文件提交到Git");
        System.out.println();
        System.out.println("🔓 公钥安全：");
        System.out.println("   ✓ 公钥可以公开，会内嵌到客户端程序中");
        System.out.println("   ✓ 公钥用于验证服务器签名，不能用于加密或签名");
        System.out.println();
        System.out.println("💾 备份建议：");
        System.out.println("   ✓ 将私钥和盐值保存到安全的密码管理器");
        System.out.println("   ✓ 创建加密备份文件存储在离线设备");
        System.out.println("   ✓ 如果密钥丢失，所有已激活的客户端都需要重新激活");
        System.out.println();
        
        System.out.println("================================================================================");
        System.out.println("                          下一步操作");
        System.out.println("================================================================================");
        System.out.println();
        System.out.println("1️⃣  复制上面的私钥和盐值到服务器的application.yml");
        System.out.println("2️⃣  使用PublicKeyConverter工具转换公钥为XML格式");
        System.out.println("3️⃣  将XML格式公钥配置到客户端CryptoHelper.cs");
        System.out.println("4️⃣  启动服务器：mvn spring-boot:run");
        System.out.println("5️⃣  测试激活流程");
        System.out.println();
        System.out.println("================================================================================");
        System.out.println("密钥生成完成！请按照上述说明进行配置。");
        System.out.println("================================================================================");
    }
}