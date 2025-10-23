# C# 客户端签名功能集成说明

## 📋 概述

本文档说明如何在C#客户端中集成API签名功能，实现与Java服务器端的安全通信。

## 🎯 核心功能

1. **HMAC-SHA256签名**: 防止请求被篡改
2. **时间戳验证**: 防止重放攻击(±5分钟有效期)
3. **Nonce去重**: 确保每个请求唯一
4. **参数规范化**: 与Java端完全兼容的参数序列化

## 📁 文件列表

### 必需文件
- `SignatureHelper.cs` - 签名生成和验证工具类
- `LicenseManager.cs` - 许可证管理器(已集成签名)
- `EncryptedConfig.cs` - AppSecret加密存储(可选,推荐)

### 测试文件
- `SignatureTestTool.cs` - 签名功能测试工具

### 依赖文件(已存在)
- `HardwareFingerprint.cs` - 硬件指纹采集
- `CryptoHelper.cs` - 加密解密工具
- `LicenseToken.cs` - 令牌模型

## ⚙️ 配置步骤

### 1. 配置AppId和AppSecret

在`SignatureHelper.cs`中配置(与服务器端保持一致):

```csharp
// 开发环境 - 直接硬编码
private const string APP_ID = "app_test_001";
private const string APP_SECRET = "secret_test_001_change_me_in_production";

// 生产环境 - 使用加密存储(推荐)
private static readonly string APP_ID = 
    EncryptedConfig.DecryptString("加密后的AppId");
private static readonly string APP_SECRET = 
    EncryptedConfig.DecryptString("加密后的AppSecret");
```

### 2. 配置服务器地址

在`LicenseManager.cs`中配置:

```csharp
private const string SERVER_URL = "https://license.yourcompany.com";
```

### 3. 生成加密的凭证(生产环境)

```csharp
// 运行一次生成加密的AppId和AppSecret
EncryptedConfig.GenerateEncryptedCredentials(
    "app_prod_001", 
    "your_production_secret_key_here"
);

// 复制输出结果到SignatureHelper.cs
```

## 🔧 使用方法

### 方法1: 自动添加签名(推荐)

```csharp
// 构建业务参数
var businessParams = new Dictionary<string, object>
{
    ["license_key"] = licenseKey,
    ["hardware_fingerprint"] = fingerprint,
    ["hardware_components"] = new List<string> { "CPU", "Board", "GUID" },
    ["client_version"] = "1.0.0"
};

// 自动添加 app_id, timestamp, nonce, sign
var signedParams = SignatureHelper.AddSignatureFields(businessParams);

// 发送HTTP请求
string json = JsonConvert.SerializeObject(signedParams);
var content = new StringContent(json, Encoding.UTF8, "application/json");
await httpClient.PostAsync(url, content);
```

### 方法2: 手动控制

```csharp
var parameters = new Dictionary<string, object>
{
    ["license_key"] = "XXXX-XXXX-XXXX-XXXX",
    ["hardware_fingerprint"] = "abc123",
    ["app_id"] = SignatureHelper.GetAppId(),
    ["timestamp"] = SignatureHelper.GetTimestamp(),
    ["nonce"] = SignatureHelper.GenerateNonce()
};

// 生成签名
string signature = SignatureHelper.GenerateSignature(parameters);
parameters["sign"] = signature;
```

## 🧪 测试验证

### 1. 运行测试工具

```csharp
// 方式1: 命令行
dotnet run --project SignatureTestTool.csproj http://localhost:8800

// 方式2: 代码调用
await SignatureTestTool.RunAllTests("http://localhost:8800");
```

### 2. 测试输出示例

```
╔════════════════════════════════════════════╗
║     C# 签名功能测试套件                    ║
╚════════════════════════════════════════════╝

=== 测试签名生成 ===
测试1 - 简单参数:
签名: a1b2c3d4e5f6...

=== 测试与服务器的签名兼容性 ===
响应状态码: 200 OK
✅ 签名验证成功! C#客户端与Java服务器兼容。
```

## 🔍 常见问题排查

### 问题1: 签名验证失败 (401错误)

**可能原因:**
1. APP_ID 或 APP_SECRET 与服务器端不一致
2. 参数序列化格式不正确
3. 时间戳超出有效期(±5分钟)

**解决方法:**
```csharp
// 检查配置
Console.WriteLine($"Client APP_ID: {SignatureHelper.GetAppId()}");
Console.WriteLine($"Client Timestamp: {SignatureHelper.GetTimestamp()}");

// 验证时间戳
bool valid = SignatureHelper.ValidateTimestamp(timestamp);
Console.WriteLine($"Timestamp valid: {valid}");

// 对比签名
var params = new Dictionary<string, object> { /* 测试参数 */ };
string sign = SignatureHelper.GenerateSignature(params);
Console.WriteLine($"Generated signature: {sign}");
```

### 问题2: List参数序列化不一致

**C#端正确格式:**
```csharp
// hardware_components会被序列化为: ["CPU","Board","GUID"]
var params = new Dictionary<string, object>
{
    ["hardware_components"] = new List<string> { "CPU", "Board", "GUID" }
};
```

**验证序列化结果:**
```csharp
string json = JsonConvert.SerializeObject(params);
Console.WriteLine(json);
// 输出: {"hardware_components":["CPU","Board","GUID"]}
```

### 问题3: Nonce重复使用

**现象:** 第二次相同请求失败

**原因:** Nonce被Redis记录,5分钟内不能重复

**解决:** 每次请求必须生成新的Nonce
```csharp
// ✅ 正确 - 每次生成新的
string nonce1 = SignatureHelper.GenerateNonce();
string nonce2 = SignatureHelper.GenerateNonce();

// ❌ 错误 - 重复使用
string nonce = "fixed_nonce";
```

### 问题4: 时间不同步

**现象:** 时间戳验证总是失败

**检查方法:**
```csharp
long clientTimestamp = SignatureHelper.GetTimestamp();
Console.WriteLine($"Client time: {DateTimeOffset.UtcNow}");
Console.WriteLine($"Client timestamp: {clientTimestamp}");

// 与服务器时间对比
// 服务器会返回 server_time 字段
```

**解决:** 同步系统时间或在服务器响应中获取时间偏移

## 🔐 安全建议

### 1. AppSecret保护

```csharp
// ❌ 不推荐 - 明文存储
private const string APP_SECRET = "my_secret_key";

// ✅ 推荐 - 加密存储
private static readonly string APP_SECRET = 
    EncryptedConfig.DecryptString("encrypted_secret");

// ✅ 最佳实践 - 混淆 + 加密
// 使用 .NET Reactor 混淆整个程序集
// 特别保护 EncryptedConfig 和 SignatureHelper 类
```

### 2. 使用 .NET Reactor 保护

**保护配置:**
```
1. NecroBit: 将签名相关方法转为native代码
   - SignatureHelper.GenerateSignature
   - SignatureHelper.AddSignatureFields
   - EncryptedConfig.DecryptString

2. String Encryption: 加密所有字符串常量
   - APP_ID
   - APP_SECRET (加密后的)
   - 服务器地址

3. Anti-Tampering: 防止修改程序集

4. Control Flow Obfuscation: 混淆控制流
```

### 3. 定期轮换AppSecret

```csharp
// 服务器端生成新的AppSecret
// 发布新版本客户端
// 旧AppSecret设置过渡期(如1个月)
```

## 📊 性能指标

| 操作 | 耗时 |
|-----|------|
| 生成签名 | < 1ms |
| 生成Nonce | < 0.1ms |
| 获取时间戳 | < 0.1ms |
| 完整签名流程 | < 2ms |

## 🔄 与Java服务器端的对应关系

| C#客户端 | Java服务器端 | 说明 |
|---------|-------------|------|
| `SignatureHelper.cs` | `SignatureUtil.java` | 签名算法实现 |
| `SignatureHelper.AddSignatureFields()` | `SignatureInterceptor.preHandle()` | 签名字段添加/验证 |
| `APP_ID / APP_SECRET` | `app_credentials表` | 凭证存储 |
| Nonce生成 | `NonceService.java` | Nonce管理 |

## 📝 完整的激活流程示例

```csharp
public async Task<bool> ActivateWithSignature(string licenseKey)
{
    // 1. 采集硬件信息
    var hwFingerprint = new HardwareFingerprint();
    string fingerprint = hwFingerprint.GetFingerprint();
    string[] components = hwFingerprint.GetFingerprintComponents();
    
    // 2. 构建业务参数
    var businessParams = new Dictionary<string, object>
    {
        ["license_key"] = licenseKey,
        ["hardware_fingerprint"] = fingerprint,
        ["hardware_components"] = new List<string>(components),
        ["client_version"] = "1.0.0"
    };
    
    // 3. 添加签名字段 (自动添加 app_id, timestamp, nonce, sign)
    var signedParams = SignatureHelper.AddSignatureFields(businessParams);
    
    // 4. 发送HTTP请求
    string json = JsonConvert.SerializeObject(signedParams);
    var content = new StringContent(json, Encoding.UTF8, "application/json");
    
    using (var httpClient = new HttpClient())
    {
        var response = await httpClient.PostAsync(
            "https://license.yourcompany.com/api/v2/activate",
            content
        );
        
        string responseBody = await response.Content.ReadAsStringAsync();
        
        if (!response.IsSuccessStatusCode)
        {
            Console.WriteLine($"激活失败: {responseBody}");
            return false;
        }
        
        // 5. 处理响应...
        return true;
    }
}
```

## 🆘 技术支持

遇到问题时,请提供以下信息:

1. **测试输出:**
   ```bash
   dotnet run --project SignatureTestTool.csproj
   ```

2. **请求示例:**
   ```csharp
   var params = SignatureHelper.AddSignatureFields(yourParams);
   Console.WriteLine(JsonConvert.SerializeObject(params, Formatting.Indented));
   ```

3. **服务器响应:**
   ```
   HTTP状态码: 401
   响应内容: {"success":false,"message":"签名验证失败"}
   ```

4. **环境信息:**
   - .NET版本
   - 操作系统
   - 服务器地址
   - APP_ID (不要提供APP_SECRET!)

## ✅ 检查清单

部署前确认:

- [ ] APP_ID 和 APP_SECRET 与服务器端一致
- [ ] 服务器地址正确配置
- [ ] 生产环境使用加密存储AppSecret
- [ ] 使用 .NET Reactor 混淆保护
- [ ] 运行测试工具验证签名兼容性
- [ ] 时间同步正常 (±5分钟内)
- [ ] Redis正常运行(服务器端)
- [ ] 防火墙允许客户端访问服务器

## 📚 相关文档

- [API签名安全方案.md](../doc/API签名安全方案.md) - 完整的签名机制说明
- [C#软件授权方案-MVP.pdf](../doc/C#软件授权方案-MVP.pdf) - 整体架构设计
- Java服务器端代码: `SignatureInterceptor.java`, `SignatureUtil.java`
