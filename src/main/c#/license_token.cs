using System;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 许可证令牌模型
    /// </summary>
    public class LicenseToken
    {
        [JsonProperty("token_id")]
        public string TokenId { get; set; }

        [JsonProperty("license_key")]
        public string LicenseKey { get; set; }

        [JsonProperty("hardware_fingerprint")]
        public string HardwareFingerprint { get; set; }

        [JsonProperty("hardware_components")]
        public string[] HardwareComponents { get; set; }

        [JsonProperty("issued_at")]
        public DateTime IssuedAt { get; set; }

        [JsonProperty("expires_at")]
        public DateTime ExpiresAt { get; set; }

        [JsonProperty("features")]
        public List<string> Features { get; set; }

        [JsonProperty("max_offline_hours")]
        public int MaxOfflineHours { get; set; }

        [JsonProperty("signature")]
        public string Signature { get; set; }

        /// <summary>
        /// 验证令牌签名
        /// </summary>
        public bool VerifySignature()
        {
            string dataToSign = $"{TokenId}{LicenseKey}{HardwareFingerprint}" +
                                $"{IssuedAt:O}{ExpiresAt:O}{string.Join(",", Features)}";
            
            return CryptoHelper.VerifySignature(dataToSign, Signature);
        }

        /// <summary>
        /// 检查是否过期
        /// </summary>
        public bool IsExpired()
        {
            return DateTime.UtcNow > ExpiresAt;
        }

        /// <summary>
        /// 检查是否尚未生效
        /// </summary>
        public bool IsNotYetValid()
        {
            return DateTime.UtcNow < IssuedAt;
        }

        /// <summary>
        /// 序列化为JSON
        /// </summary>
        public string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.None);
        }

        /// <summary>
        /// 从JSON反序列化
        /// </summary>
        public static LicenseToken FromJson(string json)
        {
            return JsonConvert.DeserializeObject<LicenseToken>(json);
        }
    }

    /// <summary>
    /// 激活响应
    /// </summary>
    public class ActivationResponse
    {
        [JsonProperty("success")]
        public bool Success { get; set; }

        [JsonProperty("encrypted_token")]
        public string EncryptedToken { get; set; }

        [JsonProperty("expires_at")]
        public DateTime ExpiresAt { get; set; }

        [JsonProperty("message")]
        public string Message { get; set; }
    }

    /// <summary>
    /// 心跳响应
    /// </summary>
    public class HeartbeatResponse
    {
        [JsonProperty("success")]
        public bool Success { get; set; }

        [JsonProperty("server_time")]
        public DateTime ServerTime { get; set; }

        [JsonProperty("license_status")]
        public string LicenseStatus { get; set; }

        [JsonProperty("force_reactivate")]
        public bool ForceReactivate { get; set; }

        [JsonProperty("message")]
        public string Message { get; set; }
    }
}
