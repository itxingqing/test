using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace LicenseSystem.Client
{
    /// <summary>
    /// 签名测试工具
    /// 用于测试和调试签名功能
    /// </summary>
    public class SignatureTestTool
    {
        /// <summary>
        /// 测试签名生成
        /// </summary>
        public static void TestSignatureGeneration()
        {
            Console.WriteLine("=== 测试签名生成 ===\n");
            
            // 测试用例1: 简单参数
            var simpleParams = new Dictionary<string, object>
            {
                ["license_key"] = "TEST-1234-5678-ABCD",
                ["hardware_fingerprint"] = "abc123def456",
                ["timestamp"] = 1697533200L,
                ["nonce"] = "test_nonce_12345"
            };
            
            string sign1 = SignatureHelper.GenerateSignature(simpleParams);
            Console.WriteLine("测试1 - 简单参数:");
            Console.WriteLine($"参数: {JsonConvert.SerializeObject(simpleParams)}");
            Console.WriteLine($"签名: {sign1}\n");
            
            // 测试用例2: 包含List参数
            var listParams = new Dictionary<string, object>
            {
                ["license_key"] = "TEST-1234-5678-ABCD",
                ["hardware_fingerprint"] = "abc123def456",
                ["hardware_components"] = new List<string> { "CPU-ID-123", "BOARD-456", "GUID-789" },
                ["timestamp"] = 1697533200L,
                ["nonce"] = "test_nonce_12345"
            };
            
            string sign2 = SignatureHelper.GenerateSignature(listParams);
            Console.WriteLine("测试2 - 包含List参数:");
            Console.WriteLine($"参数: {JsonConvert.SerializeObject(listParams)}");
            Console.WriteLine($"签名: {sign2}\n");
            
            // 测试用例3: 完整的激活请求
            var activationParams = SignatureHelper.AddSignatureFields(new Dictionary<string, object>
            {
                ["license_key"] = "TEST-1234-5678-ABCD",
                ["hardware_fingerprint"] = "abc123def456",
                ["hardware_components"] = new List<string> { "CPU", "Board", "GUID" },
                ["client_version"] = "1.0.0"
            });
            
            Console.WriteLine("测试3 - 完整的激活请求(自动添加签名字段):");
            Console.WriteLine(JsonConvert.SerializeObject(activationParams, Formatting.Indented));
            Console.WriteLine();
        }
        
        /// <summary>
        /// 测试与服务器的签名兼容性
        /// </summary>
        public static async Task TestServerCompatibility(string serverUrl)
        {
            Console.WriteLine($"=== 测试与服务器的签名兼容性 ===");
            Console.WriteLine($"服务器地址: {serverUrl}\n");
            
            try
            {
                // 构建测试请求
                var testParams = SignatureHelper.AddSignatureFields(new Dictionary<string, object>
                {
                    ["license_key"] = "TEST-KEY-FOR-SIGNATURE-VALIDATION",
                    ["hardware_fingerprint"] = "test_fingerprint_abc123",
                    ["hardware_components"] = new List<string> { "TEST-CPU", "TEST-BOARD", "TEST-GUID" },
                    ["client_version"] = "1.0.0"
                });
                
                Console.WriteLine("发送的请求参数:");
                Console.WriteLine(JsonConvert.SerializeObject(testParams, Formatting.Indented));
                Console.WriteLine();
                
                // 发送HTTP请求
                using (var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(10) })
                {
                    string jsonContent = JsonConvert.SerializeObject(testParams);
                    var content = new StringContent(jsonContent, Encoding.UTF8, "application/json");
                    
                    HttpResponseMessage response = await httpClient.PostAsync(
                        $"{serverUrl}/api/v2/activate", 
                        content
                    );
                    
                    string responseBody = await response.Content.ReadAsStringAsync();
                    
                    Console.WriteLine($"响应状态码: {(int)response.StatusCode} {response.StatusCode}");
                    Console.WriteLine($"响应内容:\n{responseBody}\n");
                    
                    if (response.IsSuccessStatusCode)
                    {
                        Console.WriteLine("✅ 签名验证成功! C#客户端与Java服务器兼容。");
                    }
                    else if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                    {
                        Console.WriteLine("❌ 签名验证失败! 请检查:");
                        Console.WriteLine("   1. APP_ID 和 APP_SECRET 是否与服务器端一致");
                        Console.WriteLine("   2. 参数序列化格式是否正确(特别是List类型)");
                        Console.WriteLine("   3. 时间戳是否在有效期内(±5分钟)");
                    }
                    else
                    {
                        Console.WriteLine($"⚠ 其他错误: {response.StatusCode}");
                    }
                }
            }
            catch (HttpRequestException ex)
            {
                Console.WriteLine($"❌ 网络错误: {ex.Message}");
                Console.WriteLine("   请确保服务器正在运行且可访问");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"❌ 未知错误: {ex.Message}");
                Console.WriteLine($"   堆栈跟踪: {ex.StackTrace}");
            }
        }
        
        /// <summary>
        /// 对比C#和预期的签名结果
        /// </summary>
        public static void CompareSignatures()
        {
            Console.WriteLine("=== 签名算法验证 ===\n");
            
            // 已知的测试用例(可以与Java服务器端对比)
            var testCase = new Dictionary<string, object>
            {
                ["app_id"] = "app_test_001",
                ["hardware_fingerprint"] = "test123",
                ["license_key"] = "XXXX-XXXX-XXXX-XXXX",
                ["nonce"] = "abc123",
                ["timestamp"] = 1697533200L
            };
            
            Console.WriteLine("测试参数:");
            Console.WriteLine(JsonConvert.SerializeObject(testCase, Formatting.Indented));
            
            string signature = SignatureHelper.GenerateSignature(testCase);
            Console.WriteLine($"\nC#生成的签名: {signature}");
            Console.WriteLine("\n提示: 使用相同参数在Java端生成签名,比对是否一致");
        }
        
        /// <summary>
        /// 测试时间戳验证
        /// </summary>
        public static void TestTimestampValidation()
        {
            Console.WriteLine("=== 测试时间戳验证 ===\n");
            
            long currentTimestamp = SignatureHelper.GetTimestamp();
            Console.WriteLine($"当前时间戳: {currentTimestamp}");
            Console.WriteLine($"UTC时间: {DateTimeOffset.UtcNow}");
            
            // 测试有效时间戳
            bool valid1 = SignatureHelper.ValidateTimestamp(currentTimestamp);
            Console.WriteLine($"\n当前时间戳验证: {(valid1 ? "✅ 通过" : "❌ 失败")}");
            
            // 测试过期时间戳(6分钟前)
            long expiredTimestamp = currentTimestamp - 360;
            bool valid2 = SignatureHelper.ValidateTimestamp(expiredTimestamp);
            Console.WriteLine($"6分钟前的时间戳验证: {(valid2 ? "✅ 通过" : "❌ 失败(预期)")}");
            
            // 测试边界时间戳(4分钟前)
            long boundaryTimestamp = currentTimestamp - 240;
            bool valid3 = SignatureHelper.ValidateTimestamp(boundaryTimestamp);
            Console.WriteLine($"4分钟前的时间戳验证: {(valid3 ? "✅ 通过(预期)" : "❌ 失败")}");
        }
        
        /// <summary>
        /// 测试Nonce生成
        /// </summary>
        public static void TestNonceGeneration()
        {
            Console.WriteLine("=== 测试Nonce生成 ===\n");
            
            Console.WriteLine("生成10个Nonce,验证唯一性:");
            var nonces = new HashSet<string>();
            
            for (int i = 0; i < 10; i++)
            {
                string nonce = SignatureHelper.GenerateNonce();
                bool isUnique = nonces.Add(nonce);
                Console.WriteLine($"{i + 1}. {nonce} - {(isUnique ? "✅ 唯一" : "❌ 重复")}");
            }
            
            Console.WriteLine($"\n所有Nonce唯一性: {(nonces.Count == 10 ? "✅ 通过" : "❌ 失败")}");
        }
        
        /// <summary>
        /// 主测试入口
        /// </summary>
        public static async Task RunAllTests(string serverUrl = null)
        {
            Console.WriteLine("╔════════════════════════════════════════════╗");
            Console.WriteLine("║     C# 签名功能测试套件                    ║");
            Console.WriteLine("╚════════════════════════════════════════════╝\n");
            
            // 测试1: 签名生成
            TestSignatureGeneration();
            Console.WriteLine(new string('-', 50) + "\n");
            
            // 测试2: 签名算法验证
            CompareSignatures();
            Console.WriteLine(new string('-', 50) + "\n");
            
            // 测试3: 时间戳验证
            TestTimestampValidation();
            Console.WriteLine(new string('-', 50) + "\n");
            
            // 测试4: Nonce生成
            TestNonceGeneration();
            Console.WriteLine(new string('-', 50) + "\n");
            
            // 测试5: 服务器兼容性(如果提供了服务器URL)
            if (!string.IsNullOrEmpty(serverUrl))
            {
                await TestServerCompatibility(serverUrl);
                Console.WriteLine(new string('-', 50) + "\n");
            }
            
            Console.WriteLine("✅ 所有测试完成!");
        }
    }
    
    /// <summary>
    /// 控制台测试程序入口
    /// </summary>
    public class Program
    {
        public static async Task Main(string[] args)
        {
            try
            {
                // 默认服务器地址(可通过命令行参数覆盖)
                string serverUrl = args.Length > 0 ? args[0] : "http://localhost:8800";
                
                await SignatureTestTool.RunAllTests(serverUrl);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n❌ 测试失败: {ex.Message}");
                Console.WriteLine($"堆栈跟踪:\n{ex.StackTrace}");
            }
            
            Console.WriteLine("\n按任意键退出...");
            Console.ReadKey();
        }
    }
}