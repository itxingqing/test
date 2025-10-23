# API签名安全方案

## 一、方案概述

本方案为激活和心跳接口增加了 **appId + appSecret + timestamp + nonce + sign** 的签名验证机制，实现以下安全目标：

### 🎯 安全目标

1. **防重放攻击**：timestamp（±5分钟）+ nonce（Redis去重）
2. **防篡改**：HMAC-SHA256签名，任何参数修改都会导致签名失败
3. **身份验证**：appId/appSecret验证客户端合法性
4. **防暴力破解**：失败次数限制（可扩展）

---

## 二、签名流程

### 客户端签名流程

```
1. 构建请求参数
   ├─ license_key: XXXX-XXXX-XXXX-XXXX
   ├─ hardware_fingerprint: sha256哈希
   └─ hardware_components: [CPU, Board, GUID]

2. 添加签名字段
   ├─ app_id: app_test_001
   ├─ timestamp: 1697533200
   └─ nonce: 生成32字符随机数

3. 参数排序并拼接
   app_id=app_test_001&hardware_fingerprint=...&license_key=...&nonce=...&timestamp=1697533200

4. HMAC-SHA256签名
   sign = HMAC-SHA256(appSecret, 排序后的参数)

5. 添加sign字段发送请求
```

### 服务器验证流程

```
1. 提取请求参数
   ├─ 读取POST body
   └─ 解析JSON

2. 基础验证
   ├─ 检查必要字段（app_id, timestamp, nonce, sign）
   ├─ 验证timestamp（±5分钟）
   └─ 验证nonce（Redis去重，防重放）

3. 签名验证
   ├─ 查询appId对应的appSecret
   ├─ 移除sign字段
   ├─ 参数排序拼接
   ├─ HMAC-SHA256计算签名
   └─ 时间常量比较（防时序攻击）

4. 返回结果
   ├─ 验证通过：继续业务逻辑
   └─ 验证失败：返回401错误
```

---

## 三、关键实现

### 1. 签名算法

**排序规则：**
- 按参数名ASCII码升序排序
- 过滤null值和空字符串
- 格式：`key1=value1&key2=value2&key3=value3`

**签名计算：**
```java
String sortedParams = "app_id=xxx&hardware_fingerprint=xxx&...";
String sign = HMAC-SHA256(appSecret, sortedParams);
```

### 2. 防重放机制

**Timestamp验证：**
```java
long now = System.currentTimeMillis() / 1000;
long diff = Math.abs(now - timestamp);
boolean valid = diff <= 300; // 5分钟
```

**Nonce验证（Redis）：**
```java
// Redis SETNX（SET if Not eXists）
Boolean success = redisTemplate.opsForValue().setIfAbsent(
    "license:nonce:" + nonce,
    timestamp,
    5,
    TimeUnit.MINUTES
);
// success=true表示nonce未使用过
```

### 3. 时间常量比较（防时序攻击）

```java
// 错误做法（易受时序攻击）
return providedSign.equals(calculatedSign);

// 正确做法（时间常量比较）
int result = 0;
for (int i = 0; i < a.length(); i++) {
    result |= a.charAt(i) ^ b.charAt(i);
}
return result == 0;
```

---

## 四、安全性分析

### 4.1 攻击场景与防御

| 攻击类型 | 攻击方式 | 防御措施 | 效果 |
|---------|---------|---------|------|
| **重放攻击** | 截获请求后重放 | timestamp + nonce | ✅ 有效 |
| **参数篡改** | 修改请求参数 | HMAC-SHA256签名 | ✅ 有效 |
| **中间人攻击** | 截获并修改请求 | HTTPS + 签名 | ✅ 有效 |
| **暴力破解** | 大量尝试激活 | 频率限制（可扩展） | ⚠️ 基础 |
| **AppSecret泄露** | 反编译提取 | 混淆+加密存储 | ⚠️ 延缓 |
| **时序攻击** | 通过响应时间猜测 | 时间常量比较 | ✅ 有效 |

### 4.2 安全等级

```
基础防护（当前方案）：
✅ 防重放攻击
✅ 防参数篡改
✅ 基础身份验证
⚠️ AppSecret可被提取（需配合混淆）

增强防护（可选扩展）：
✅ IP白名单
✅ 请求频率限制
✅ 异常行为检测
✅ AppSecret动态更新
```

---

## 五、部署配置

### 5.1 生成AppId和AppSecret

```bash
# 使用UUID生成AppId
app_id=$(uuidgen)

# 使用随机字符串生成AppSecret（64字符）
app_secret=$(openssl rand -hex 32)

# 插入数据库
INSERT INTO app_credentials (app_id, app_secret, app_name, product_version)
VALUES ('$app_id', '$app_secret', 'YourApp', 'v1.0');
```

### 5.2 服务器配置

**application.yml:**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

**拦截器配置（已自动）：**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureInterceptor)
                .addPathPatterns("/api/v2/activate", "/api/v2/heartbeat");
    }
}
```

### 5.3 客户端配置

**SignatureHelper.cs:**
```csharp
// 方式1：直接硬编码（简单但不安全）
private const string APP_ID = "app_test_001";
private const string APP_SECRET = "secret_test_001";

// 方式2：加密存储（推荐）
private static readonly string APP_ID = 
    EncryptedConfig.DecryptString("加密后的AppId");
private static readonly string APP_SECRET = 
    EncryptedConfig.DecryptString("加密后的AppSecret");
```

---

## 六、测试验证

### 6.1 正常请求测试

```bash
# 手动构造签名请求
curl -X POST http://localhost:8080/api/v2/activate \
  -H "Content-Type: application/json" \
  -d '{
    "license_key": "XXXX-XXXX-XXXX-XXXX",
    "hardware_fingerprint": "abc123...",
    "hardware_components": ["CPU", "Board", "GUID"],
    "app_id": "app_test_001",
    "timestamp": 1697533200,
    "nonce": "a1b2c3d4e5f6...",
    "sign": "计算得到的签名"
  }'
```

### 6.2 攻击测试

**测试1：重放攻击**
```bash
# 发送相同请求两次
# 预期：第一次成功，第二次失败（nonce已使用）
```

**测试2：参数篡改**
```bash
# 修改license_key但不重新签名
# 预期：签名验证失败
```

**测试3：过期请求**
```bash
# timestamp设置为10分钟前
# 预期：请求已过期
```

### 6.3 性能测试

**签名计算耗时：**
- HMAC-SHA256：< 1ms
- Redis查询：< 5ms
- 总验证耗时：< 10ms

---

## 七、最佳实践

### 7.1 AppSecret管理

**生产环境：**
1. ✅ 使用强随机数生成（至少64字符）
2. ✅ 定期轮换（建议每年）
3. ✅ 加密存储在数据库
4. ✅ 使用环境变量而非配置文件
5. ❌ 不要提交到Git

**客户端：**
1. ✅ 使用.NET Reactor字符串加密
2. ✅ 使用EncryptedConfig加密存储
3. ✅ 运行时解密使用
4. ⚠️ 接受AppSecret可能被提取的风险
5. ✅ 配合其他安全措施（硬件绑定、在线验证）

### 7.2 Nonce管理

**Redis配置（推荐）：**
- 过期时间：5分钟
- 数据结构：String（SETNX）
- 命名空间：`license:nonce:{nonce}`

**本地缓存（仅单机）：**
- 适用场景：开发环境、单机部署
- 限制：不支持分布式
- 定期清理：避免内存泄漏

### 7.3 错误处理

**客户端：**
```csharp
try {
    var result = await ActivateAsync(licenseKey);
} catch (Exception ex) {
    // 不要暴露详细错误信息
    MessageBox.Show("激活失败，请检查网络连接");
}
```

**服务器：**
```java
// 统一返回格式
{
    "success": false,
    "message": "签名验证失败"  // 不要暴露详细原因
}
```

---

## 八、扩展方案（可选）

### 8.1 IP白名单

```java
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(...) {
        String ip = getClientIp(request);
        if (!isWhitelisted(ip)) {
            return sendError(response, 403, "IP未授权");
        }
        return true;
    }
}
```

### 8.2 请求频率限制

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public boolean preHandle(...) {
        String key = "rate_limit:" + appId + ":" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
        
        if (count > 100) {
            return sendError(response, 429, "请求过于频繁");
        }
        return true;
    }
}
```

### 8.3 异常行为检测

```java
// 检测异常模式
- 短时间内大量失败请求
- 同一IP多个不同的硬件指纹
- 异常的激活时间分布
- 地理位置异常（可选）
```

---

## 九、常见问题

### Q1: AppSecret泄露怎么办？

**影响：**
- 攻击者可以伪造签名
- 可以激活任意许可证（如果有许可证密钥）

**应对：**
1. 立即更换AppSecret（数据库更新）
2. 发布新版本客户端
3. 吊销可疑的激活记录
4. 分析攻击来源

**预防：**
- 使用多层混淆保护
- 定期轮换AppSecret
- 配合硬件绑定、在线验证等其他安全措施
- AppSecret泄露不等于完全失效（仍需许可证密钥+硬件指纹）

### Q2: 没有Redis可以使用吗？

**可以，但有限制：**
- 使用本地缓存（ConcurrentHashMap）
- 仅适用于单机部署
- 分布式环境必须使用Redis

### Q3: 签名验证失败怎么排查？

**排查步骤：**
1. 检查参数排序是否一致
2. 检查timestamp是否在±5分钟内
3. 检查appId和appSecret是否正确
4. 检查nonce是否重复使用
5. 检查List类型参数的序列化格式
6. 使用日志输出排序后的参数字符串

### Q4: 如何测试签名功能？

**测试工具：**
```csharp
// 客户端测试
var parameters = new Dictionary<string, object> {
    ["test"] = "value",
    ["timestamp"] = 1697533200,
    ["nonce"] = "abc123"
};
var sign = SignatureHelper.GenerateSignature(parameters);
Console.WriteLine($"签名: {sign}");
```

```java
// 服务器测试
Map<String, Object> params = new HashMap<>();
params.put("test", "value");
params.put("timestamp", 1697533200);
params.put("nonce", "abc123");
String sign = SignatureUtil.generateSignature(params, "appSecret");
System.out.println("签名: " + sign);
```

---

## 十、安全评估

### 当前方案安全评分：⭐⭐⭐⭐☆ (4/5)

**优势：**
- ✅ 完整的签名验证流程
- ✅ 有效防止重放和篡改
- ✅ 时间常量比较防时序攻击
- ✅ 易于实施和维护

**局限：**
- ⚠️ AppSecret可能被逆向提取
- ⚠️ 需要Redis支持（最佳实践）
- ⚠️ 暴力破解防护需要额外实现

**适用场景：**
- ✅ 商业软件授权系统
- ✅ 中小规模部署（<10万激活）
- ✅ 需要离线激活支持的场景

**不适用场景：**
- ❌ 极高安全要求（如金融系统）
- ❌ 大规模分布式部署（需要更复杂架构）
- ❌ 完全离线环境（无法验证nonce）

---

**总结：** 本方案提供了生产级的API安全保护，在合理的实施成本下实现了较高的安全性。配合硬件绑定、在线验证、代码混淆等其他措施，可以构建一个可靠的软件授权系统。