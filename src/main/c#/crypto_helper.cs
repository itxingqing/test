using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 加密解密工具类
    /// </summary>
    public static class CryptoHelper
    {
        // 服务器公钥（硬编码，用于验证签名）
        private const string SERVER_PUBLIC_KEY = @"
<RSAKeyValue>
  <Modulus>your_modulus_here</Modulus>
  <Exponent>AQAB</Exponent>
</RSAKeyValue>";

        /// <summary>
        /// HKDF密钥派生（基于硬件指纹）
        /// </summary>
        public static byte[] DeriveKey(string hardwareFingerprint, string info, byte[] salt)
        {
            // 输入密钥材料 = SHA256(硬件指纹)
            byte[] ikm;
            using (SHA256 sha256 = SHA256.Create())
            {
                ikm = sha256.ComputeHash(Encoding.UTF8.GetBytes(hardwareFingerprint));
            }

            // HKDF
            return HKDF(ikm, salt, Encoding.UTF8.GetBytes(info), 32);
        }

        /// <summary>
        /// HKDF实现（基于HMAC-SHA256）
        /// </summary>
        private static byte[] HKDF(byte[] ikm, byte[] salt, byte[] info, int outputLength)
        {
            // Extract
            byte[] prk;
            using (var hmac = new HMACSHA256(salt ?? new byte[32]))
            {
                prk = hmac.ComputeHash(ikm);
            }

            // Expand
            int iterations = (int)Math.Ceiling((double)outputLength / 32);
            byte[] result = new byte[outputLength];
            byte[] t = new byte[0];
            
            using (var hmac = new HMACSHA256(prk))
            {
                for (int i = 1; i <= iterations; i++)
                {
                    byte[] input = new byte[t.Length + info.Length + 1];
                    Buffer.BlockCopy(t, 0, input, 0, t.Length);
                    Buffer.BlockCopy(info, 0, input, t.Length, info.Length);
                    input[input.Length - 1] = (byte)i;

                    t = hmac.ComputeHash(input);
                    int copyLength = Math.Min(32, outputLength - (i - 1) * 32);
                    Buffer.BlockCopy(t, 0, result, (i - 1) * 32, copyLength);
                }
            }

            return result;
        }

        /// <summary>
        /// AES-256-GCM加密
        /// </summary>
        public static byte[] AesGcmEncrypt(string plainText, byte[] key)
        {
            byte[] plainBytes = Encoding.UTF8.GetBytes(plainText);
            byte[] nonce = new byte[12]; // GCM推荐12字节
            byte[] tag = new byte[16];
            byte[] cipherBytes = new byte[plainBytes.Length];

            using (RandomNumberGenerator rng = RandomNumberGenerator.Create())
            {
                rng.GetBytes(nonce);
            }

            using (var aesGcm = new AesGcm(key))
            {
                aesGcm.Encrypt(nonce, plainBytes, cipherBytes, tag);
            }

            // 组合：nonce(12) + tag(16) + ciphertext
            byte[] result = new byte[nonce.Length + tag.Length + cipherBytes.Length];
            Buffer.BlockCopy(nonce, 0, result, 0, nonce.Length);
            Buffer.BlockCopy(tag, 0, result, nonce.Length, tag.Length);
            Buffer.BlockCopy(cipherBytes, 0, result, nonce.Length + tag.Length, cipherBytes.Length);

            return result;
        }

        /// <summary>
        /// AES-256-GCM解密
        /// </summary>
        public static string AesGcmDecrypt(byte[] encryptedData, byte[] key)
        {
            if (encryptedData.Length < 28) // 12 + 16
                throw new ArgumentException("加密数据格式错误");

            byte[] nonce = new byte[12];
            byte[] tag = new byte[16];
            byte[] cipherBytes = new byte[encryptedData.Length - 28];
            byte[] plainBytes = new byte[cipherBytes.Length];

            Buffer.BlockCopy(encryptedData, 0, nonce, 0, 12);
            Buffer.BlockCopy(encryptedData, 12, tag, 0, 16);
            Buffer.BlockCopy(encryptedData, 28, cipherBytes, 0, cipherBytes.Length);

            using (var aesGcm = new AesGcm(key))
            {
                aesGcm.Decrypt(nonce, cipherBytes, tag, plainBytes);
            }

            return Encoding.UTF8.GetString(plainBytes);
        }

        /// <summary>
        /// 使用Windows DPAPI加密
        /// </summary>
        public static byte[] DpapiProtect(byte[] data)
        {
            return ProtectedData.Protect(data, null, DataProtectionScope.LocalMachine);
        }

        /// <summary>
        /// 使用Windows DPAPI解密
        /// </summary>
        public static byte[] DpapiUnprotect(byte[] encryptedData)
        {
            return ProtectedData.Unprotect(encryptedData, null, DataProtectionScope.LocalMachine);
        }

        /// <summary>
        /// 验证RSA签名
        /// </summary>
        public static bool VerifySignature(string data, string signatureBase64)
        {
            try
            {
                using (RSA rsa = RSA.Create())
                {
                    rsa.FromXmlString(SERVER_PUBLIC_KEY);
                    
                    byte[] dataBytes = Encoding.UTF8.GetBytes(data);
                    byte[] signatureBytes = Convert.FromBase64String(signatureBase64);

                    return rsa.VerifyData(dataBytes, signatureBytes, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
                }
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 时间常量比较（防止时序攻击）
        /// </summary>
        public static bool ConstantTimeEquals(byte[] a, byte[] b)
        {
            if (a.Length != b.Length)
                return false;

            int result = 0;
            for (int i = 0; i < a.Length; i++)
            {
                result |= a[i] ^ b[i];
            }

            return result == 0;
        }

        /// <summary>
        /// 计算SHA256哈希
        /// </summary>
        public static string ComputeSha256(string input)
        {
            using (SHA256 sha256 = SHA256.Create())
            {
                byte[] hashBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(input));
                return BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
            }
        }
    }
}
