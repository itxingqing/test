using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 客户端签名工具类
    /// 实现HMAC-SHA256签名,与Java服务器端兼容
    /// </summary>
    public static class SignatureHelper
    {
        // ==================== 配置部分 ====================
        // 生产环境应该使用加密存储
        // 建议配合.NET Reactor的字符串加密功能和EncryptedConfig类
        
        private const string APP_ID = "app_test_001";
        private const string APP_SECRET = "secret_test_001_change_me_in_production";
        
        // 使用加密存储的方式(推荐):
        // private static readonly string APP_ID = EncryptedConfig.DecryptString("加密后的AppId");
        // private static readonly string APP_SECRET = EncryptedConfig.DecryptString("加密后的AppSecret");
        
        /// <summary>
        /// 生成请求签名 (HMAC-SHA256)
        /// </summary>
        /// <param name="parameters">请求参数(不包含sign字段)</param>
        /// <returns>签名字符串(小写十六进制)</returns>
        public static string GenerateSignature(Dictionary<string, object> parameters)
        {
            // 1. 移除sign字段(如果存在)
            var paramsToSign = new Dictionary<string, object>(parameters);
            paramsToSign.Remove("sign");
            
            // 2. 参数排序并拼接
            string sortedParams = SortAndJoinParams(paramsToSign);
            
            // 3. HMAC-SHA256签名
            using (HMACSHA256 hmac = new HMACSHA256(Encoding.UTF8.GetBytes(APP_SECRET)))
            {
                byte[] hashBytes = hmac.ComputeHash(Encoding.UTF8.GetBytes(sortedParams));
                return BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
            }
        }
        
        /// <summary>
        /// 参数排序并拼接
        /// 格式: key1=value1&key2=value2&key3=value3
        /// 规则: 
        /// 1. 按参数名ASCII码升序排序
        /// 2. 过滤null值和空字符串
        /// 3. List类型序列化为JSON数组格式
        /// </summary>
        private static string SortAndJoinParams(Dictionary<string, object> parameters)
        {
            // 过滤null值和空字符串
            var filteredParams = parameters
                .Where(p => p.Value != null && !string.IsNullOrEmpty(p.Value.ToString()))
                .OrderBy(p => p.Key) // ASCII码升序排序
                .ToList();
            
            // 拼接字符串
            var sb = new StringBuilder();
            foreach (var param in filteredParams)
            {
                if (sb.Length > 0)
                    sb.Append("&");
                
                string valueStr = SerializeValue(param.Value);
                sb.Append(param.Key).Append("=").Append(valueStr);
            }
            
            return sb.ToString();
        }
        
        /// <summary>
        /// 序列化参数值
        /// List类型转为JSON数组格式,与Java端保持一致
        /// </summary>
        private static string SerializeValue(object value)
        {
            if (value is List<string> list)
            {
                // 将List<string>序列化为JSON数组格式: ["item1","item2","item3"]
                var items = list.Select(item => $"\"{item}\"");
                return $"[{string.Join(",", items)}]";
            }
            
            return value.ToString();
        }
        
        /// <summary>
        /// 生成随机nonce(32字符)
        /// 使用GUID确保唯一性
        /// </summary>
        public static string GenerateNonce()
        {
            return Guid.NewGuid().ToString("N"); // 32个十六进制字符,无连字符
        }
        
        /// <summary>
        /// 获取当前Unix时间戳(秒)
        /// </summary>
        public static long GetTimestamp()
        {
            return DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        }
        
        /// <summary>
        /// 获取AppId
        /// </summary>
        public static string GetAppId()
        {
            return APP_ID;
        }
        
        /// <summary>
        /// 为请求参数添加签名相关字段
        /// 这是最常用的方法,自动添加app_id, timestamp, nonce, sign
        /// </summary>
        /// <param name="parameters">原始业务参数</param>
        /// <returns>添加签名后的完整参数</returns>
        public static Dictionary<string, object> AddSignatureFields(Dictionary<string, object> parameters)
        {
            var signedParams = new Dictionary<string, object>(parameters);
            
            // 添加签名必要字段
            signedParams["app_id"] = APP_ID;
            signedParams["timestamp"] = GetTimestamp();
            signedParams["nonce"] = GenerateNonce();
            
            // 生成签名(不包含sign字段本身)
            signedParams["sign"] = GenerateSignature(signedParams);
            
            return signedParams;
        }
        
        /// <summary>
        /// 验证时间戳是否在有效期内(±5分钟)
        /// 用于客户端自检
        /// </summary>
        public static bool ValidateTimestamp(long timestamp)
        {
            long now = GetTimestamp();
            long diff = Math.Abs(now - timestamp);
            return diff <= 300; // 5分钟 = 300秒
        }
    }
}
