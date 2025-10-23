# C# 软件授权系统 - 完整实现

## 项目概述

这是一个基于硬件指纹绑定的软件授权系统，包含C#客户端和Spring Boot服务器端。

### 核心特性

- ✅ 硬件指纹绑定（CPU、主板、机器GUID）
- ✅ HKDF密钥派生（无客户端密钥泄漏）
- ✅ AES-256-GCM加密 + Windows DPAPI双重保护
- ✅ RSA-2048签名验证
- ✅ 在线心跳验证（2小时间隔）
- ✅ 离线宽限期（48小时）
- ✅ 防时间回拨
- ✅ 防重放攻击

---

## 快速开始

### 一、服务器端部署

#### 1. 环境要求

- Java 11+
- MySQL 8.0+
- Maven 3.6+

#### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE license_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入表结构
mysql -u root -p license_db < database-schema.sql
```

#### 3. 生成密钥对

```bash
# 运行密钥生成工具
cd server
mvn compile
mvn exec:java -Dexec.mainClass="com.yourcompany.license.util.KeyGenerator"
```

保存输出的：
- **私钥**（配置到 `application.yml`）
- **公钥**（配置到客户端 `CryptoHelper.cs`）
- **HKDF盐值**（配置到 `application.yml`）

#### 4. 配置 application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/license_db?useSSL=false
    username: root
    password: your_password

license:
  server:
    private-key: <从KeyGenerator获取的私钥>
    hkdf-salt: <从KeyGenerator获取的盐值>
```

#### 5. 启动服务器

```bash
mvn spring-boot:run
```

服务器将在 `http://localhost:8080` 启动

---

### 二、客户端集成

#### 1. 环境要求

- .NET Framework 4.7.2+ 或 .NET 6+
- Windows 操作系统

#### 2. 安装NuGet包

```bash
Install-Package Newtonsoft.Json
Install-Package System.Security.Cryptography.Algorithms
```

#### 3. 配置服务器公钥

编辑 `CryptoHelper.cs`，将 `SERVER_PUBLIC_KEY` 替换为实际公钥（XML格式）：

```csharp
private const string SERVER_PUBLIC_KEY = @"
<RSAKeyValue>
  <Modulus>你的公钥模数</Modulus>
  <Exponent>AQAB</Exponent>
</RSAKeyValue>";
```

> **注意**：需要将Base64格式的公钥转换为XML格式。可以使用在线工具或编写转换程序。

#### 4. 配置服务器地址

编辑 `LicenseManager.cs`：

```csharp
private const string SERVER_URL = "https://license.yourcompany.com";
```

#### 5. 在程序启动时验证许可证

```csharp
// Program.cs 或 App.xaml.cs
using LicenseSystem.Client;

var licenseManager = new LicenseManager();
var (valid, message) = await licenseManager.ValidateLicenseAsync();

if (!valid)
{
    // 显示激活窗口
    var activationWindow = new ActivationWindow();
    if (activationWindow.ShowDialog() != true)
    {
        // 用户取消激活，退出程序
        Application.Current.Shutdown();
        return;
    }
    
    // 重新验证
    (valid, message) = await licenseManager.ValidateLicenseAsync();
    if (!valid)
    {
        MessageBox.Show("激活失败，程序将退出");
        Application.Current.Shutdown();
        return;
    }
}

// 许可证有效，继续启动程序
```

---

## API接口文档

### 1. 激活许可证

**请求**

```http
POST /api/v2/activate
Content-Type: application/json

{
  "license_key": "XXXX-XXXX-XXXX-XXXX",
  "hardware_fingerprint": "sha256哈希值",
  "hardware_components": ["CPU", "Board", "GUID"],
  "timestamp": 1697533200,
  "client_version": "1.0.0"
}
```

**响应**

```json
{
  "success": true,
  "encrypted_token": "Base64加密令牌",
  "expires_at": "2026-10-17T10:00:00",
  "message": "激活成功"
}
```

### 2. 心跳验证

**请求**

```http
POST /api/v2/heartbeat
Content-Type: application/json

{
  "token_id": "uuid",
  "hardware_fingerprint": "sha256哈希值",
  "hardware_components": ["CPU", "Board", "GUID"],
  "timestamp": 1697533200
}
```

**响应**

```json
{
  "success": true,
  "server_time": "2025-10-17T12:00:00",
  "license_status": "active",
  "force_reactivate": false,
  "message": "验证成功"
}
```

### 3. 管理接口（后台使用）

#### 创建许可证

```http
POST /api/admin/licenses
Content-Type: application/json

{
  "customer_email": "user@example.com",
  "product_name": "YourProduct",
  "max_activations": 2,
  "expires_at": "2026-12-31T23:59:59",
  "features": ["feature1", "feature2"]
}
```

#### 查询许可证列表

```http
GET /api/admin/licenses?page=1&size=20&customerEmail=user@example.com
```

#### 吊销许可证

```http
POST /api/admin/licenses/{licenseKey}/revoke
```

---

## 代码混淆保护

### 使用 .NET Reactor

1. 下载并安装 [.NET Reactor](https://www.eziriz.com/)

2. 配置混淆选项：
    - ✅ NecroBit（关键方法转native）
    - ✅ Strong Name Removal Protection
    - ✅ Anti-Debug（基础级别）
    - ✅ Anti-Tampering

3. 标记关键方法：

```csharp
[Obfuscation(Feature = "necrobit")]
public string GetFingerprint() { ... }

[Obfuscation(Feature = "necrobit")]
private (bool valid, string message) PerformLocalValidation() { ... }
```

4. 构建保护版本：

```bash
# 使用.NET Reactor命令行工具
dotnet_reactor.exe -file YourApp.exe -necrobit -anti_debug
```

---

## 测试检查清单

### 服务器端测试

- [ ] 数据库连接正常
- [ ] 许可证创建成功
- [ ] 激活API正常响应
- [ ] 心跳API正常响应
- [ ] 吊销许可证生效

### 客户端测试

- [ ] 硬件指纹采集成功
- [ ] 首次激活成功
- [ ] 重复激活（同一机器）
- [ ] 更换1项硬件仍可用
- [ ] 更换2项硬件需重新激活
- [ ] 复制令牌文件到其他机器失败
- [ ] 修改系统时间被检测
- [ ] 离线运行48小时正常
- [ ] 离线运行超过48小时被拒绝
- [ ] 网络中断后恢复正常
- [ ] 许可证过期被拒绝

---

## 常见问题

### Q1: 如何将Base64公钥转换为XML格式？

```csharp
// 使用以下工具方法
public static string ConvertPublicKeyToXml(string base64PublicKey)
{
    byte[] keyBytes = Convert.FromBase64String(base64PublicKey);
    using (RSA rsa = RSA.Create())
    {
        rsa.ImportSubjectPublicKeyInfo(keyBytes, out _);
        return rsa.ToXmlString(false);
    }
}
```

### Q2: 用户更换硬件后无法使用怎么办？

系统允许3项硬件特征中的1项变化。如果用户更换了2项或以上，需要重新激活。管理员可以在后台删除旧的激活记录。

### Q3: 如何增加激活次数？

在管理后台修改许可证的 `max_activations` 字段。

### Q4: 客户端报"令牌签名验证失败"？

检查：
1. 服务器公钥是否正确配置
2. 公钥格式是否为XML格式
3. 客户端和服务器的时钟是否同步

---

## 安全建议

1. **HTTPS必须启用**：生产环境必须使用HTTPS保护通信
2. **私钥保管**：服务器私钥和HKDF盐值务必妥善保管
3. **速率限制**：配置Nginx或使用Redis实现API速率限制
4. **日志监控**：定期检查异常激活模式
5. **定期更新**：定期更新混淆配置和签名密钥

---

## 许可证

MIT License

---

## 技术支持

如有问题，请提交Issue或联系技术支持。