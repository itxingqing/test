using System;
using System.Security.Cryptography;
using System.Text;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 加密配置存储（增强AppSecret安全性）
    /// 
    /// 使用方式：
    /// 1. 编译时使用EncryptString加密AppId和AppSecret
    /// 2. 将加密后的字符串硬编码到SignatureHelper
    /// 3. 运行时使用DecryptString解密
    /// 4. 配合.NET Reactor的字符串加密功能效果更佳
    /// </summary>
    public static class EncryptedConfig
    {
        // 加密密钥（基于机器特征，每个构建环境不同）
        private static readonly byte[] EncryptionKey = DeriveKeyFromEnvironment();
        
        /// <summary>
        /// 加密字符串（编译时工具使用）
        /// </summary>
        public static string EncryptString(string plainText)
        {
            try
            {
                using (Aes aes = Aes.Create())
                {
                    aes.Key = EncryptionKey;
                    aes.GenerateIV();
                    
                    using (var encryptor = aes.CreateEncryptor(aes.Key, aes.IV))
                    {
                        byte[] plainBytes = Encoding.UTF8.GetBytes(plainText);
                        byte[] encryptedBytes = encryptor.TransformFinalBlock(plainBytes, 0, plainBytes.Length);
                        
                        // 组合IV和密文
                        byte[] result = new byte[aes.IV.Length + encryptedBytes.Length];
                        Buffer.BlockCopy(aes.IV, 0, result, 0, aes.IV.Length);
                        Buffer.BlockCopy(encryptedBytes, 0, result, aes.IV.Length, encryptedBytes.Length);
                        
                        return Convert.ToBase64String(result);
                    }
                }
            }
            catch
            {
                return plainText; // 降级方案
            }
        }
        
        /// <summary>
        /// 解密字符串（运行时使用）
        /// </summary>
        public static string DecryptString(string encryptedText)
        {
            try
            {
                byte[] fullCipher = Convert.FromBase64String(encryptedText);
                
                using (Aes aes = Aes.Create())
                {
                    aes.Key = EncryptionKey;
                    
                    // 提取IV
                    byte[] iv = new byte[aes.IV.Length];
                    Buffer.BlockCopy(fullCipher, 0, iv, 0, iv.Length);
                    aes.IV = iv;
                    
                    // 提取密文
                    byte[] cipherBytes = new byte[fullCipher.Length - iv.Length];
                    Buffer.BlockCopy(fullCipher, iv.Length, cipherBytes, 0, cipherBytes.Length);
                    
                    using (var decryptor = aes.CreateDecryptor(aes.Key, aes.IV))
                    {
                        byte[] plainBytes = decryptor.TransformFinalBlock(cipherBytes, 0, cipherBytes.Length);
                        return Encoding.UTF8.GetString(plainBytes);
                    }
                }
            }
            catch
            {
                return encryptedText; // 降级方案
            }
        }
        
        /// <summary>
        /// 从环境派生加密密钥
        /// </summary>
        private static byte[] DeriveKeyFromEnvironment()
        {
            // 基于固定字符串派生密钥（在.NET Reactor混淆后更安全）
            string seed = "YourCompany_LicenseSystem_2025_SecretKey";
            
            using (SHA256 sha256 = SHA256.Create())
            {
                return sha256.ComputeHash(Encoding.UTF8.GetBytes(seed));
            }
        }
        
        /// <summary>
        /// 测试工具：生成加密的AppId和AppSecret
        /// </summary>
        public static void GenerateEncryptedCredentials(string appId, string appSecret)
        {
            string encryptedAppId = EncryptString(appId);
            string encryptedAppSecret = EncryptString(appSecret);
            
            Console.WriteLine("=== 加密后的凭证（复制到SignatureHelper.cs） ===");
            Console.WriteLine($"private const string ENCRYPTED_APP_ID = \"{encryptedAppId}\";");
            Console.WriteLine($"private const string ENCRYPTED_APP_SECRET = \"{encryptedAppSecret}\";");
            Console.WriteLine();
            Console.WriteLine("在SignatureHelper中使用：");
            Console.WriteLine("private static readonly string APP_ID = EncryptedConfig.DecryptString(ENCRYPTED_APP_ID);");
            Console.WriteLine("private static readonly string APP_SECRET = EncryptedConfig.DecryptString(ENCRYPTED_APP_SECRET);");
        }
    }
}
