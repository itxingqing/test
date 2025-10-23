# C# 签名功能快速集成指南

## 🚀 5分钟快速开始

### 步骤1: 复制必需文件 (1分钟)

将以下文件复制到你的项目:

```
YourProject/
├── LicenseSystem/
│   ├── SignatureHelper.cs          ✅ 新增 - 签名工具
│   ├── LicenseManager.cs           ✅ 已更新 - 集成签名
│   ├── EncryptedConfig.cs          ✅ 新增 - 加密存储
│   ├── HardwareFingerprint.cs      (已存在)
│   ├── CryptoHelper.cs             (已存在)
│   ├── LicenseToken.cs             (已存在)
│   └── ActivationWindow.cs         (已存在)
└── Tests/
    └── SignatureTestTool.cs        ✅ 新增 - 测试工具
```

### 步骤2: 配置AppId和AppSecret (1分钟)

打开 `SignatureHelper.cs`,修改:

```csharp
// 从服务器管理员获取这两个值
private const string APP_ID = "your_app_id_here";
private const string APP_SECRET = "your_app_secret_here";
```

**如何获取:**
在服务器端数据库查询:
```sql
SELECT app_id, app_secret FROM app_credentials WHERE app_name = 'YourApp';
```

### 步骤3: 配置服务器地址 (30秒)

打开 `LicenseManager.cs`,修改:

```csharp
private const string SERVER_URL = "https://your-server.com";
```

### 步骤4: 测试签名功能 (2分钟)

```csharp
// 运行测试工具
await SignatureTestTool.RunAllTests("https://your-server.com");
```

**期望输出:**
```
✅ 签名验证成功! C#客户端与Java服务器兼容。
```

### 步骤5: 集成到你的应用 (1分钟)

```csharp
// 原有代码无需修改,只需确保使用最新的LicenseManager
var licenseManager = new LicenseManager();

// 激活 - 现在自动包含签名
var (success, message) = await licenseManager.ActivateAsync(licenseKey);

// 验证 - 心跳也自动包含签名
var (valid, validMessage) = await licenseManager.ValidateLicenseAsync();
```

## ✨ 完成!

你的C#客户端现在已经集成了签名功能,可以安全地与Java服务器通信。

---

## 🔍 验证集成是否成功

### 测试1: 签名生成

```csharp
var testParams = new Dictionary<string, object>
{
    ["test_key"] = "test_value",
    ["timestamp"] = SignatureHelper.GetTimestamp(),
    ["nonce"] = SignatureHelper.GenerateNonce()
};

string signature = SignatureHelper.GenerateSignature(testParams);
Console.WriteLine($"签名: {signature}");
// 应该输出: 64个十六进制字符
```

### 测试2: 与服务器通信

```csharp
try
{
    var licenseManager = new LicenseManager();
    var (success, message) = await licenseManager.ActivateAsync("TEST-KEY");
    
    if (!success && message.Contains("签名"))
    {
        Console.WriteLine("❌ 签名配置有问题");
    }
    else
    {
        Console.WriteLine("✅ 签名功能正常");
    }
}
catch (Exception ex)
{
    Console.WriteLine($"错误: {ex.Message}");
}
```

## 🆘 遇到问题?

### 问题: 401 签名验证失败

**检查清单:**
1. APP_ID 是否正确?
2. APP_SECRET 是否正确?
3. 服务器地址是否正确?
4. 系统时间是否同步? (与服务器相差不超过5分钟)

**调试方法:**
```csharp
// 打印签名参数
var params = SignatureHelper.AddSignatureFields(yourBusinessParams);
Console.WriteLine(JsonConvert.SerializeObject(params, Formatting.Indented));

// 检查时间戳
long timestamp = SignatureHelper.GetTimestamp();
Console.WriteLine($"当前时间戳: {timestamp}");
Console.WriteLine($"UTC时间: {DateTimeOffset.UtcNow}");
```

### 问题: 连接超时

**可能原因:**
- 服务器未启动
- 防火墙阻止连接
- 网络不通

**测试连接:**
```bash
# 测试服务器是否可访问
curl https://your-server.com/api/v2/health

# 期望输出: OK
```

### 问题: Nonce已使用

**原因:** 请求被重复发送

**解决:** 确保每次请求生成新的Nonce
```csharp
// ✅ 正确
for (int i = 0; i < 3; i++)
{
    var params = SignatureHelper.AddSignatureFields(businessParams);
    // 每次循环 params 中的 nonce 都是新的
}

// ❌ 错误
var params = SignatureHelper.AddSignatureFields(businessParams);
for (int i = 0; i < 3; i++)
{
    // nonce 相同,第二次会失败
}
```

## 📞 获取帮助

1. **运行完整测试:**
   ```bash
   dotnet run --project SignatureTestTool.csproj https://your-server.com
   ```

2. **查看详细文档:**
   - [C# 签名功能集成说明.md](./C#签名功能集成说明.md)
   - [API签名安全方案.md](../doc/API签名安全方案.md)

3. **联系技术支持** 时提供:
   - 测试工具的完整输出
   - 服务器响应内容
   - APP_ID (不要提供APP_SECRET!)

---

## 🎉 下一步

### 生产环境部署

1. **使用加密存储 AppSecret:**
   ```csharp
   // 运行一次
   EncryptedConfig.GenerateEncryptedCredentials(
       "your_app_id",
       "your_app_secret"
   );
   
   // 复制输出到 SignatureHelper.cs
   private static readonly string APP_ID = 
       EncryptedConfig.DecryptString("加密后的AppId");
   ```

2. **使用 .NET Reactor 混淆:**
   ```
   - 启用 NecroBit (重点保护签名方法)
   - 启用 String Encryption
   - 启用 Anti-Tampering
   ```

3. **部署检查:**
   - [ ] APP_SECRET 已加密
   - [ ] 使用 HTTPS 连接
   - [ ] 程序集已混淆
   - [ ] 服务器 Redis 正常运行
   - [ ] 在生产环境测试激活和心跳

### 监控和维护

1. **日志记录:**
   ```csharp
   // 在 LicenseManager 中添加日志
   log.Info($"激活请求: licenseKey={licenseKey.Substring(0, 4)}...");
   log.Error($"签名验证失败: {errorMessage}");
   ```

2. **定期轮换 AppSecret:**
   - 每年更换一次
   - 保持1个月过渡期
   - 通知用户更新客户端

3. **监控异常模式:**
   - 大量签名失败 → 可能被攻击
   - 相同Nonce重复 → 可能有重放攻击
   - 时间戳异常 → 客户端时间不同步
