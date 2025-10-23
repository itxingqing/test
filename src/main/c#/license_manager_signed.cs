using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Win32;
using Newtonsoft.Json;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 许可证管理器 - 核心类
    /// 集成API签名功能
    /// </summary>
    public class LicenseManager
    {
        private const string SERVER_URL = "https://license.yourcompany.com";
        private const string LICENSE_FILE_PATH = @"%LocalAppData%\YourCompany\.license";
        private const string REGISTRY_KEY = @"SOFTWARE\YourCompany";
        private const string TIMESTAMP_FILE = @"%ProgramData%\YourCompany\.timestamp";
        private const int HEARTBEAT_INTERVAL_HOURS = 2;
        private const int MAX_OFFLINE_HOURS = 48;

        private static readonly HttpClient _httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
        private readonly HardwareFingerprint _hwFingerprint;
        private LicenseToken _currentToken;
        private DateTime _lastHeartbeat;
        private DateTime _lastVerifyTime;

        public LicenseManager()
        {
            _hwFingerprint = new HardwareFingerprint();
            _lastHeartbeat = DateTime.MinValue;
            _lastVerifyTime = LoadLastVerifyTime();
        }

        /// <summary>
        /// 激活许可证(带签名)
        /// </summary>
        public async Task<(bool success, string message)> ActivateAsync(string licenseKey)
        {
            try
            {
                string fingerprint = _hwFingerprint.GetFingerprint();
                string[] components = _hwFingerprint.GetFingerprintComponents();

                // 构建业务参数
                var businessParams = new Dictionary<string, object>
                {
                    ["license_key"] = licenseKey,
                    ["hardware_fingerprint"] = fingerprint,
                    ["hardware_components"] = new List<string>(components),
                    ["client_version"] = "1.0.0"
                };

                // 添加签名字段
                var signedParams = SignatureHelper.AddSignatureFields(businessParams);

                // 发送请求
                string jsonContent = JsonConvert.SerializeObject(signedParams);
                var content = new StringContent(jsonContent, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PostAsync(
                    $"{SERVER_URL}/api/v2/activate", 
                    content
                );
                
                string responseBody = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                {
                    var errorResponse = JsonConvert.DeserializeObject<dynamic>(responseBody);
                    return (false, errorResponse?.message?.ToString() ?? "激活失败");
                }

                ActivationResponse activationResponse = JsonConvert.DeserializeObject<ActivationResponse>(responseBody);
                
                if (!activationResponse.Success)
                    return (false, activationResponse.Message ?? "激活失败");

                // 解密令牌
                byte[] encryptedTokenBytes = Convert.FromBase64String(activationResponse.EncryptedToken);
                
                // 使用硬件指纹派生密钥解密
                byte[] derivedKey = CryptoHelper.DeriveKey(fingerprint, "会话令牌加密", new byte[32]);
                string tokenJson = CryptoHelper.AesGcmDecrypt(encryptedTokenBytes, derivedKey);
                
                LicenseToken token = LicenseToken.FromJson(tokenJson);

                // 验证令牌签名
                if (!token.VerifySignature())
                    return (false, "令牌签名验证失败");

                // 保存令牌
                SaveToken(token);
                _currentToken = token;
                _lastHeartbeat = DateTime.UtcNow;

                return (true, "激活成功");
            }
            catch (HttpRequestException ex)
            {
                return (false, $"网络错误: {ex.Message}");
            }
            catch (Exception ex)
            {
                return (false, $"激活失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 验证许可证(启动时调用)
        /// </summary>
        public async Task<(bool valid, string message)> ValidateLicenseAsync()
        {
            try
            {
                // 步骤1: 加载令牌
                if (_currentToken == null)
                {
                    if (!LoadToken())
                        return (false, "未找到激活信息,请先激活");
                }

                // 步骤2: 本地验证
                var (valid, message) = PerformLocalValidation();
                if (!valid)
                    return (false, message);

                // 步骤3: 检查是否需要心跳
                TimeSpan timeSinceLastHeartbeat = DateTime.UtcNow - _lastHeartbeat;
                if (timeSinceLastHeartbeat.TotalHours >= HEARTBEAT_INTERVAL_HOURS)
                {
                    var (heartbeatSuccess, heartbeatMessage) = await PerformHeartbeatAsync();
                    
                    if (!heartbeatSuccess)
                    {
                        // 心跳失败,检查离线时长
                        if (timeSinceLastHeartbeat.TotalHours > MAX_OFFLINE_HOURS)
                            return (false, "超过离线时限,需要联网验证");
                        
                        // 还在宽限期内,允许继续运行
                        return (true, $"离线模式运行(剩余 {MAX_OFFLINE_HOURS - (int)timeSinceLastHeartbeat.TotalHours} 小时)");
                    }
                }

                return (true, "许可证验证通过");
            }
            catch (Exception ex)
            {
                return (false, $"验证失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 执行本地验证
        /// </summary>
        private (bool valid, string message) PerformLocalValidation()
        {
            // 验证项1: 签名验证
            if (!_currentToken.VerifySignature())
                return (false, "令牌签名验证失败");

            // 验证项2: 硬件指纹匹配
            string currentFingerprint = _hwFingerprint.GetFingerprint();
            string[] currentComponents = _hwFingerprint.GetFingerprintComponents();
            
            if (!HardwareFingerprint.IsMatch(_currentToken.HardwareComponents, currentComponents))
                return (false, "硬件环境已变化,请重新激活");

            // 验证项3: 时间验证
            DateTime now = DateTime.UtcNow;
            
            if (_currentToken.IsNotYetValid())
                return (false, "许可证尚未生效");

            if (_currentToken.IsExpired())
                return (false, "许可证已过期");

            // 验证项4: 防时间回拨
            if (now < _lastVerifyTime)
            {
                return (false, "检测到时间回拨,需要在线验证");
            }

            // 更新最后验证时间
            SaveLastVerifyTime(now);
            _lastVerifyTime = now;

            return (true, "本地验证通过");
        }

        /// <summary>
        /// 执行心跳(带签名)
        /// </summary>
        private async Task<(bool success, string message)> PerformHeartbeatAsync()
        {
            try
            {
                // 构建业务参数
                var businessParams = new Dictionary<string, object>
                {
                    ["token_id"] = _currentToken.TokenId,
                    ["hardware_fingerprint"] = _hwFingerprint.GetFingerprint(),
                    ["hardware_components"] = new List<string>(_hwFingerprint.GetFingerprintComponents())
                };

                // 添加签名字段
                var signedParams = SignatureHelper.AddSignatureFields(businessParams);

                // 发送请求
                string jsonContent = JsonConvert.SerializeObject(signedParams);
                var content = new StringContent(jsonContent, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PostAsync(
                    $"{SERVER_URL}/api/v2/heartbeat", 
                    content
                );
                
                string responseBody = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                    return (false, "心跳请求失败");

                HeartbeatResponse heartbeatResponse = JsonConvert.DeserializeObject<HeartbeatResponse>(responseBody);

                if (!heartbeatResponse.Success)
                    return (false, heartbeatResponse.Message ?? "心跳验证失败");

                // 更新最后心跳时间
                _lastHeartbeat = DateTime.UtcNow;

                // 处理特殊状态
                if (heartbeatResponse.LicenseStatus == "revoked")
                {
                    DeleteToken();
                    return (false, "许可证已被吊销");
                }

                if (heartbeatResponse.ForceReactivate)
                {
                    DeleteToken();
                    return (false, "需要重新激活");
                }

                return (true, "心跳成功");
            }
            catch (HttpRequestException)
            {
                return (false, "网络连接失败");
            }
            catch (Exception ex)
            {
                return (false, $"心跳失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 保存令牌到本地
        /// </summary>
        private void SaveToken(LicenseToken token)
        {
            try
            {
                string tokenJson = token.ToJson();
                
                // 1. 使用硬件指纹派生密钥AES加密
                byte[] derivedKey = CryptoHelper.DeriveKey(_hwFingerprint.GetFingerprint(), "会话令牌加密", new byte[32]);
                byte[] encryptedToken = CryptoHelper.AesGcmEncrypt(tokenJson, derivedKey);

                // 2. DPAPI二次加密
                byte[] protectedData = CryptoHelper.DpapiProtect(encryptedToken);

                // 3. 保存到文件
                string filePath = Environment.ExpandEnvironmentVariables(LICENSE_FILE_PATH);
                Directory.CreateDirectory(Path.GetDirectoryName(filePath));
                File.WriteAllBytes(filePath, protectedData);

                // 设置文件属性为隐藏+系统
                File.SetAttributes(filePath, FileAttributes.Hidden | FileAttributes.System);
            }
            catch (Exception ex)
            {
                throw new Exception("保存令牌失败", ex);
            }
        }

        /// <summary>
        /// 从本地加载令牌
        /// </summary>
        private bool LoadToken()
        {
            try
            {
                string filePath = Environment.ExpandEnvironmentVariables(LICENSE_FILE_PATH);
                
                if (!File.Exists(filePath))
                    return false;

                // 1. 读取文件
                byte[] protectedData = File.ReadAllBytes(filePath);

                // 2. DPAPI解密
                byte[] encryptedToken = CryptoHelper.DpapiUnprotect(protectedData);

                // 3. 硬件指纹派生密钥解密
                byte[] derivedKey = CryptoHelper.DeriveKey(_hwFingerprint.GetFingerprint(), "会话令牌加密", new byte[32]);
                string tokenJson = CryptoHelper.AesGcmDecrypt(encryptedToken, derivedKey);

                // 4. 反序列化
                _currentToken = LicenseToken.FromJson(tokenJson);

                // 5. 加载最后心跳时间(从令牌文件的修改时间估算)
                FileInfo fileInfo = new FileInfo(filePath);
                _lastHeartbeat = fileInfo.LastWriteTimeUtc;

                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 删除令牌
        /// </summary>
        private void DeleteToken()
        {
            try
            {
                string filePath = Environment.ExpandEnvironmentVariables(LICENSE_FILE_PATH);
                if (File.Exists(filePath))
                    File.Delete(filePath);

                _currentToken = null;
            }
            catch
            {
                // 忽略错误
            }
        }

        /// <summary>
        /// 保存最后验证时间
        /// </summary>
        private void SaveLastVerifyTime(DateTime time)
        {
            try
            {
                long timestamp = new DateTimeOffset(time).ToUnixTimeSeconds();
                byte[] timestampBytes = BitConverter.GetBytes(timestamp);
                byte[] key = CryptoHelper.DeriveKey(_hwFingerprint.GetFingerprint(), "时间戳加密", new byte[16]);
                byte[] encrypted = CryptoHelper.AesGcmEncrypt(timestamp.ToString(), key);

                // 保存到注册表
                using (RegistryKey regKey = Registry.LocalMachine.CreateSubKey(REGISTRY_KEY))
                {
                    regKey?.SetValue("LastVerifyTime", Convert.ToBase64String(encrypted));
                }

                // 保存到文件
                string filePath = Environment.ExpandEnvironmentVariables(TIMESTAMP_FILE);
                Directory.CreateDirectory(Path.GetDirectoryName(filePath));
                File.WriteAllBytes(filePath, encrypted);
                File.SetAttributes(filePath, FileAttributes.Hidden | FileAttributes.System);
            }
            catch
            {
                // 忽略错误
            }
        }

        /// <summary>
        /// 加载最后验证时间
        /// </summary>
        private DateTime LoadLastVerifyTime()
        {
            try
            {
                byte[] key = CryptoHelper.DeriveKey(_hwFingerprint.GetFingerprint(), "时间戳加密", new byte[16]);
                DateTime registryTime = DateTime.MinValue;
                DateTime fileTime = DateTime.MinValue;

                // 从注册表读取
                try
                {
                    using (RegistryKey regKey = Registry.LocalMachine.OpenSubKey(REGISTRY_KEY))
                    {
                        object value = regKey?.GetValue("LastVerifyTime");
                        if (value != null)
                        {
                            byte[] encrypted = Convert.FromBase64String(value.ToString());
                            string decrypted = CryptoHelper.AesGcmDecrypt(encrypted, key);
                            long timestamp = long.Parse(decrypted);
                            registryTime = DateTimeOffset.FromUnixTimeSeconds(timestamp).UtcDateTime;
                        }
                    }
                }
                catch { }

                // 从文件读取
                try
                {
                    string filePath = Environment.ExpandEnvironmentVariables(TIMESTAMP_FILE);
                    if (File.Exists(filePath))
                    {
                        byte[] encrypted = File.ReadAllBytes(filePath);
                        string decrypted = CryptoHelper.AesGcmDecrypt(encrypted, key);
                        long timestamp = long.Parse(decrypted);
                        fileTime = DateTimeOffset.FromUnixTimeSeconds(timestamp).UtcDateTime;
                    }
                }
                catch { }

                // 返回较大的时间
                return registryTime > fileTime ? registryTime : fileTime;
            }
            catch
            {
                return DateTime.MinValue;
            }
        }

        /// <summary>
        /// 获取许可证信息
        /// </summary>
        public LicenseToken GetLicenseInfo()
        {
            return _currentToken;
        }
    }
}
