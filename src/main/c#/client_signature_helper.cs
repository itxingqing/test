using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 客户端签名工具类
    /// </summary>
    public static class SignatureHelper
    {
        // AppId和AppSecret（生产环境应该使用混淆和加密）
        // 建议使用.NET Reactor的字符串加密功能
        private const string APP_ID = "app_test_001";
        private const string APP_SECRET = "secret_test_001_change_me_in_production";
        
        /// <summary>
        /// 生成请求签名
        /// </summary>
        /// <param name="parameters">请求参数（不包含sign字段）</param>
        /// <returns>签名字符串</returns>
        public static string GenerateSignature(Dictionary<string, object> parameters)
        {
            // 1. 移除sign字段（如果存在）
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
        /// 格式：key1=value1&key2=value2&key3=value3
        /// </summary>
        private static string SortAndJoinParams(Dictionary<string, object> parameters)
        {
            // 过滤null值和空字符串
            var filteredParams = parameters
                .Where(p => p.Value != null && !string.IsNullOrEmpty(p.Value.ToString()))
                .OrderBy(p => p.Key)
                .ToList();
            
            // 拼接字符串
            var sb = new StringBuilder();
            foreach (var param in filteredParams)
            {
                if (sb.Length > 0)
                    sb.Append("&");
                
                sb.Append(param.Key).Append("=").Append(param.Value);
            }
            
            return sb.ToString();
        }
        
        /// <summary>
        /// 生成随机nonce（32字符）
        /// </summary>
        public static string GenerateNonce()
        {
            return Guid.NewGuid().ToString("N"); // 32个十六进制字符
        }
        
        /// <summary>
        /// 获取当前Unix时间戳（秒）
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
        /// </summary>
        /// <param name="parameters">原始参数</param>
        /// <returns>添加签名后的参数</returns>
        public static Dictionary<string, object> AddSignatureFields(Dictionary<string, object> parameters)
        {
            var signedParams = new Dictionary<string, object>(parameters);
            
            // 添加签名必要字段
            signedParams["app_id"] = APP_ID;
            signedParams["timestamp"] = GetTimestamp();
            signedParams["nonce"] = GenerateNonce();
            
            // 生成签名
            signedParams["sign"] = GenerateSignature(signedParams);
            
            return signedParams;
        }
    }
}
