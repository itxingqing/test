using System;
using System.Management;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Win32;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 硬件指纹采集类 - 采集CPU、主板、机器GUID
    /// </summary>
    public class HardwareFingerprint
    {
        private string _cachedFingerprint;

        /// <summary>
        /// 获取硬件指纹（SHA256哈希）
        /// </summary>
        public string GetFingerprint()
        {
            if (!string.IsNullOrEmpty(_cachedFingerprint))
                return _cachedFingerprint;

            try
            {
                string cpuId = GetCpuId();
                string boardSerial = GetBoardSerial();
                string machineGuid = GetMachineGuid();

                // 组合：CPU序列号|主板序列号|机器GUID
                string combined = $"{cpuId}|{boardSerial}|{machineGuid}";
                
                // SHA256哈希
                using (SHA256 sha256 = SHA256.Create())
                {
                    byte[] hashBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(combined));
                    _cachedFingerprint = BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
                }

                return _cachedFingerprint;
            }
            catch (Exception ex)
            {
                throw new Exception("无法采集硬件指纹", ex);
            }
        }

        /// <summary>
        /// 获取CPU序列号
        /// </summary>
        private string GetCpuId()
        {
            try
            {
                using (ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT ProcessorId FROM Win32_Processor"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        string processorId = obj["ProcessorId"]?.ToString();
                        if (!string.IsNullOrWhiteSpace(processorId))
                            return processorId.Trim();
                    }
                }

                // 备用方案：CPU品牌+核心数
                using (ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT Name, NumberOfCores FROM Win32_Processor"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        string name = obj["Name"]?.ToString() ?? "";
                        string cores = obj["NumberOfCores"]?.ToString() ?? "";
                        return $"{name}_{cores}".Trim();
                    }
                }
            }
            catch
            {
                // 忽略异常，返回默认值
            }

            return "UNKNOWN_CPU";
        }

        /// <summary>
        /// 获取主板序列号
        /// </summary>
        private string GetBoardSerial()
        {
            try
            {
                using (ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT SerialNumber FROM Win32_BaseBoard"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        string serial = obj["SerialNumber"]?.ToString();
                        if (!string.IsNullOrWhiteSpace(serial) && serial != "None")
                            return serial.Trim();
                    }
                }
            }
            catch
            {
                // 忽略异常
            }

            return "UNKNOWN_BOARD";
        }

        /// <summary>
        /// 获取Windows机器GUID
        /// </summary>
        private string GetMachineGuid()
        {
            try
            {
                using (RegistryKey key = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Cryptography"))
                {
                    if (key != null)
                    {
                        object guid = key.GetValue("MachineGuid");
                        if (guid != null)
                            return guid.ToString();
                    }
                }
            }
            catch
            {
                // 忽略异常
            }

            return "UNKNOWN_GUID";
        }

        /// <summary>
        /// 获取硬件特征列表（用于容错匹配）
        /// </summary>
        public string[] GetFingerprintComponents()
        {
            return new string[]
            {
                GetCpuId(),
                GetBoardSerial(),
                GetMachineGuid()
            };
        }

        /// <summary>
        /// 检查两个指纹是否匹配（允许1项变化）
        /// </summary>
        public static bool IsMatch(string[] components1, string[] components2)
        {
            if (components1.Length != 3 || components2.Length != 3)
                return false;

            int matchCount = 0;
            for (int i = 0; i < 3; i++)
            {
                if (components1[i] == components2[i])
                    matchCount++;
            }

            // 3项中至少2项匹配
            return matchCount >= 2;
        }
    }
}
