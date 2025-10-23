# .NET Reactor 增强配置完整指南

## 📋 概述

本指南提供.NET Reactor的完整配置方案，针对许可证系统的关键代码进行最强级别保护。

## 🎯 保护目标

### 核心目标（按重要性排序）
1. **签名相关方法** → NecroBit (Native代码)
2. **加密解密方法** → NecroBit (Native代码)
3. **硬件指纹采集** → NecroBit (Native代码)
4. **所有字符串常量** → 字符串加密
5. **程序集完整性** → Anti-Tampering
6. **控制流** → 混淆
7. **调试检测** → Anti-Debug

---

## 一、.NET Reactor 项目配置文件

### 创建配置文件: `LicenseSystem.nrproj`

```xml
<?xml version="1.0" encoding="utf-8"?>
<dotNetReactorProject version="6.0">
  
  <!-- ==================== 基本设置 ==================== -->
  <settings>
    <projectName>LicenseSystem Client Protection</projectName>
    <inputAssembly>bin\Release\LicenseSystem.exe</inputAssembly>
    <outputAssembly>bin\Protected\LicenseSystem.exe</outputAssembly>
    <strongName>
      <enabled>true</enabled>
      <keyFile>YourCompany.snk</keyFile>
    </strongName>
  </settings>

  <!-- ==================== NecroBit (最高优先级) ==================== -->
  <necroBit>
    <enabled>true</enabled>
    <mode>full</mode> <!-- full, standard, compatibility -->
    
    <!-- 签名相关方法 -->
    <methods>
      <method>LicenseSystem.Client.SignatureHelper::GenerateSignature</method>
      <method>LicenseSystem.Client.SignatureHelper::SortAndJoinParams</method>
      <method>LicenseSystem.Client.SignatureHelper::SerializeValue</method>
      <method>LicenseSystem.Client.SignatureHelper::AddSignatureFields</method>
      <method>LicenseSystem.Client.SignatureHelper::GenerateNonce</method>
      <method>LicenseSystem.Client.SignatureHelper::GetTimestamp</method>
    </methods>
    
    <!-- 加密解密方法 -->
    <methods>
      <method>LicenseSystem.Client.CryptoHelper::DeriveKey</method>
      <method>LicenseSystem.Client.CryptoHelper::HKDF</method>
      <method>LicenseSystem.Client.CryptoHelper::AesGcmEncrypt</method>
      <method>LicenseSystem.Client.CryptoHelper::AesGcmDecrypt</method>
      <method>LicenseSystem.Client.CryptoHelper::VerifySignature</method>
      <method>LicenseSystem.Client.CryptoHelper::ComputeSha256</method>
    </methods>
    
    <!-- 硬件指纹方法 -->
    <methods>
      <method>LicenseSystem.Client.HardwareFingerprint::GetFingerprint</method>
      <method>LicenseSystem.Client.HardwareFingerprint::GetCpuId</method>
      <method>LicenseSystem.Client.HardwareFingerprint::GetBoardSerial</method>
      <method>LicenseSystem.Client.HardwareFingerprint::GetMachineGuid</method>
      <method>LicenseSystem.Client.HardwareFingerprint::IsMatch</method>
    </methods>
    
    <!-- 许可证验证方法 -->
    <methods>
      <method>LicenseSystem.Client.LicenseManager::ValidateLicenseAsync</method>
      <method>LicenseSystem.Client.LicenseManager::PerformLocalValidation</method>
      <method>LicenseSystem.Client.LicenseManager::PerformHeartbeatAsync</method>
      <method>LicenseSystem.Client.LicenseToken::VerifySignature</method>
    </methods>
    
    <!-- 加密配置方法 -->
    <methods>
      <method>LicenseSystem.Client.EncryptedConfig::DecryptString</method>
      <method>LicenseSystem.Client.EncryptedConfig::DeriveKeyFromEnvironment</method>
    </methods>
  </necroBit>

  <!-- ==================== 字符串加密 ==================== -->
  <stringEncryption>
    <enabled>true</enabled>
    <mode>advanced</mode> <!-- basic, advanced, extreme -->
    
    <!-- 加密所有字符串 -->
    <encryptAllStrings>true</encryptAllStrings>
    
    <!-- 特别保护的字符串模式 -->
    <patterns>
      <pattern>*secret*</pattern>
      <pattern>*key*</pattern>
      <pattern>*password*</pattern>
      <pattern>*token*</pattern>
      <pattern>https://*</pattern>
      <pattern>SELECT *</pattern>
    </patterns>
    
    <!-- 排除的命名空间（避免UI字符串加密导致问题） -->
    <excludeNamespaces>
      <namespace>System.*</namespace>
      <namespace>Microsoft.*</namespace>
    </excludeNamespaces>
  </stringEncryption>

  <!-- ==================== 控制流混淆 ==================== -->
  <controlFlowObfuscation>
    <enabled>true</enabled>
    <level>9</level> <!-- 1-10, 10最强 -->
    
    <!-- 混淆的类型 -->
    <types>
      <type>LicenseSystem.Client.SignatureHelper</type>
      <type>LicenseSystem.Client.CryptoHelper</type>
      <type>LicenseSystem.Client.HardwareFingerprint</type>
      <type>LicenseSystem.Client.LicenseManager</type>
      <type>LicenseSystem.Client.EncryptedConfig</type>
    </types>
  </controlFlowObfuscation>

  <!-- ==================== 名称混淆 ==================== -->
  <nameObfuscation>
    <enabled>true</enabled>
    <renameMode>unprintable</renameMode> <!-- simple, unprintable -->
    
    <!-- 混淆私有成员 -->
    <obfuscatePrivateMembers>true</obfuscatePrivateMembers>
    
    <!-- 混淆内部成员 -->
    <obfuscateInternalMembers>true</obfuscateInternalMembers>
    
    <!-- 保留公共API（如果有SDK） -->
    <keepPublicApi>false</keepPublicApi>
    
    <!-- 排除UI相关（避免XAML绑定失败） -->
    <excludeTypes>
      <type>LicenseSystem.Client.ActivationWindow</type>
      <type>LicenseSystem.Client.MainWindow</type>
    </excludeTypes>
  </nameObfuscation>

  <!-- ==================== Anti-Tampering ==================== -->
  <antiTampering>
    <enabled>true</enabled>
    <mode>strong</mode> <!-- basic, strong, extreme -->
    
    <!-- 检测修改后的行为 -->
    <onTamperDetected>exit</onTamperDetected> <!-- exit, exception, custom -->
    
    <!-- 静默退出（不显示错误信息） -->
    <silentExit>true</silentExit>
  </antiTampering>

  <!-- ==================== Anti-Debug ==================== -->
  <antiDebug>
    <enabled>true</enabled>
    <mode>advanced</mode> <!-- basic, advanced -->
    
    <!-- 检测方法 -->
    <detectManagedDebugger>true</detectManagedDebugger>
    <detectUnmanagedDebugger>true</detectUnmanagedDebugger>
    <detectDebuggerPorts>true</detectDebuggerPorts>
    
    <!-- 检测到调试器后的行为 -->
    <onDebugDetected>exit</onDebugDetected>
  </antiDebug>

  <!-- ==================== 资源加密 ==================== -->
  <resourceEncryption>
    <enabled>true</enabled>
    
    <!-- 加密嵌入的资源 -->
    <encryptEmbeddedResources>true</encryptEmbeddedResources>
  </resourceEncryption>

  <!-- ==================== Strong Name 移除保护 ==================== -->
  <strongNameRemovalProtection>
    <enabled>true</enabled>
  </strongNameRemovalProtection>

  <!-- ==================== 压缩 ==================== -->
  <compression>
    <enabled>true</enabled>
    <level>9</level> <!-- 1-9 -->
  </compression>

  <!-- ==================== 合并依赖 ==================== -->
  <merging>
    <enabled>false</enabled> <!-- 如果需要单文件分发则启用 -->
    <assemblies>
      <assembly>Newtonsoft.Json.dll</assembly>
      <!-- 其他依赖 -->
    </assemblies>
  </merging>

  <!-- ==================== 排除项 ==================== -->
  <exclusions>
    <!-- 不混淆的类型（如果有外部依赖） -->
    <types>
      <!-- 保留序列化类 -->
      <type>LicenseSystem.Client.LicenseToken</type>
      <type>LicenseSystem.Client.ActivationResponse</type>
      <type>LicenseSystem.Client.HeartbeatResponse</type>
    </types>
  </exclusions>

</dotNetReactorProject>
```

---

## 二、命令行配置（自动化构建）

### 1. 创建保护脚本: `protect.bat`

```batch
@echo off
echo ==========================================
echo .NET Reactor Protection Script
echo ==========================================
echo.

REM 设置路径
set REACTOR_PATH="C:\Program Files\Eziriz\.NET Reactor\dotNET_Reactor.Console.exe"
set PROJECT_FILE="LicenseSystem.nrproj"
set INPUT_DIR="bin\Release"
set OUTPUT_DIR="bin\Protected"

REM 清理输出目录
echo [1/5] 清理输出目录...
if exist %OUTPUT_DIR% rmdir /s /q %OUTPUT_DIR%
mkdir %OUTPUT_DIR%

REM 编译项目
echo [2/5] 编译项目...
dotnet build -c Release
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

REM 生成强命名密钥（如果不存在）
if not exist "YourCompany.snk" (
    echo [3/5] 生成强命名密钥...
    sn -k YourCompany.snk
)

REM 运行.NET Reactor
echo [4/5] 运行.NET Reactor保护...
%REACTOR_PATH% -file %INPUT_DIR%\LicenseSystem.exe ^
               -targetfile %OUTPUT_DIR%\LicenseSystem.exe ^
               -project %PROJECT_FILE% ^
               -verbose

if %errorlevel% neq 0 (
    echo .NET Reactor保护失败！
    pause
    exit /b 1
)

REM 复制依赖文件
echo [5/5] 复制依赖文件...
xcopy %INPUT_DIR%\*.dll %OUTPUT_DIR%\ /Y /Q
xcopy %INPUT_DIR%\*.config %OUTPUT_DIR%\ /Y /Q

echo.
echo ==========================================
echo 保护完成！
echo 输出目录: %OUTPUT_DIR%
echo ==========================================
pause
```

### 2. 创建 PowerShell 脚本: `protect.ps1`

```powershell
# .NET Reactor Protection Script (PowerShell)

param(
    [string]$Configuration = "Release",
    [string]$ReactorPath = "C:\Program Files\Eziriz\.NET Reactor\dotNET_Reactor.Console.exe",
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================="
Write-Host ".NET Reactor Protection Script"
Write-Host "=========================================="
Write-Host ""

# 路径配置
$ProjectFile = "LicenseSystem.nrproj"
$InputDir = "bin\$Configuration"
$OutputDir = "bin\Protected"

# 1. 清理输出目录
Write-Host "[1/6] 清理输出目录..." -ForegroundColor Cyan
if (Test-Path $OutputDir) {
    Remove-Item -Path $OutputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null

# 2. 编译项目
if (-not $SkipBuild) {
    Write-Host "[2/6] 编译项目..." -ForegroundColor Cyan
    dotnet build -c $Configuration
    if ($LASTEXITCODE -ne 0) {
        Write-Error "编译失败！"
        exit 1
    }
} else {
    Write-Host "[2/6] 跳过编译..." -ForegroundColor Yellow
}

# 3. 生成强命名密钥
if (-not (Test-Path "YourCompany.snk")) {
    Write-Host "[3/6] 生成强命名密钥..." -ForegroundColor Cyan
    & sn -k YourCompany.snk
}

# 4. 验证文件存在
Write-Host "[4/6] 验证文件..." -ForegroundColor Cyan
$inputAssembly = Join-Path $InputDir "LicenseSystem.exe"
if (-not (Test-Path $inputAssembly)) {
    Write-Error "找不到输入程序集: $inputAssembly"
    exit 1
}

# 5. 运行.NET Reactor
Write-Host "[5/6] 运行.NET Reactor保护..." -ForegroundColor Cyan
$outputAssembly = Join-Path $OutputDir "LicenseSystem.exe"

& $ReactorPath -file $inputAssembly `
               -targetfile $outputAssembly `
               -project $ProjectFile `
               -verbose

if ($LASTEXITCODE -ne 0) {
    Write-Error ".NET Reactor保护失败！"
    exit 1
}

# 6. 复制依赖文件
Write-Host "[6/6] 复制依赖文件..." -ForegroundColor Cyan
Copy-Item -Path "$InputDir\*.dll" -Destination $OutputDir -Force
Copy-Item -Path "$InputDir\*.config" -Destination $OutputDir -Force -ErrorAction SilentlyContinue

# 7. 生成保护报告
Write-Host ""
Write-Host "=========================================="
Write-Host "保护完成！" -ForegroundColor Green
Write-Host "=========================================="
Write-Host "输出目录: $OutputDir" -ForegroundColor Yellow
Write-Host ""

# 文件大小对比
$originalSize = (Get-Item $inputAssembly).Length / 1MB
$protectedSize = (Get-Item $outputAssembly).Length / 1MB
Write-Host "原始大小: $($originalSize.ToString('F2')) MB"
Write-Host "保护后大小: $($protectedSize.ToString('F2')) MB"
Write-Host "大小增加: $(($protectedSize - $originalSize).ToString('F2')) MB"
Write-Host ""

# 列出保护的文件
Write-Host "保护的文件:"
Get-ChildItem $OutputDir | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor Gray
}
```

---

## 三、CI/CD 集成（自动化）

### 1. GitHub Actions 配置

```yaml
# .github/workflows/build-and-protect.yml

name: Build and Protect

on:
  push:
    branches: [ main, release/* ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-protect:
    runs-on: windows-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Setup .NET
      uses: actions/setup-dotnet@v3
      with:
        dotnet-version: '6.0.x'
    
    - name: Restore dependencies
      run: dotnet restore
    
    - name: Build (Release)
      run: dotnet build -c Release --no-restore
    
    - name: Run tests
      run: dotnet test --no-build --verbosity normal
    
    - name: Install .NET Reactor
      run: |
        # 从安全存储下载.NET Reactor
        # 注意：需要购买许可证
        Invoke-WebRequest -Uri ${{ secrets.REACTOR_DOWNLOAD_URL }} -OutFile reactor-setup.exe
        Start-Process -FilePath reactor-setup.exe -ArgumentList "/SILENT" -Wait
    
    - name: Protect with .NET Reactor
      run: |
        powershell -ExecutionPolicy Bypass -File protect.ps1 -SkipBuild
    
    - name: Upload protected artifacts
      uses: actions/upload-artifact@v3
      with:
        name: protected-release
        path: bin/Protected/
        retention-days: 30
```

### 2. Azure DevOps Pipeline

```yaml
# azure-pipelines.yml

trigger:
  branches:
    include:
    - main
    - release/*

pool:
  vmImage: 'windows-latest'

variables:
  solution: '**/*.sln'
  buildPlatform: 'Any CPU'
  buildConfiguration: 'Release'

steps:
- task: NuGetToolInstaller@1

- task: NuGetCommand@2
  inputs:
    restoreSolution: '$(solution)'

- task: VSBuild@1
  inputs:
    solution: '$(solution)'
    platform: '$(buildPlatform)'
    configuration: '$(buildConfiguration)'

- task: VSTest@2
  inputs:
    platform: '$(buildPlatform)'
    configuration: '$(buildConfiguration)'

- task: PowerShell@2
  displayName: 'Protect with .NET Reactor'
  inputs:
    filePath: 'protect.ps1'
    arguments: '-Configuration $(buildConfiguration)'
  env:
    REACTOR_LICENSE: $(ReactorLicenseKey)

- task: PublishBuildArtifacts@1
  inputs:
    PathtoPublish: 'bin/Protected'
    ArtifactName: 'protected-drop'
    publishLocation: 'Container'
```

---

## 四、验证保护效果

### 1. 创建验证脚本: `verify-protection.ps1`

```powershell
# 验证.NET Reactor保护效果

param(
    [string]$ProtectedAssembly = "bin\Protected\LicenseSystem.exe"
)

Write-Host "=========================================="
Write-Host "验证.NET Reactor保护效果"
Write-Host "=========================================="
Write-Host ""

# 检查文件存在
if (-not (Test-Path $ProtectedAssembly)) {
    Write-Error "找不到保护后的程序集: $ProtectedAssembly"
    exit 1
}

# 1. 检查是否可以反编译
Write-Host "[1/5] 检查反编译难度..." -ForegroundColor Cyan
try {
    # 尝试用ildasm反编译
    $ildasmOutput = & ildasm $ProtectedAssembly /out=temp.il 2>&1
    
    if (Test-Path "temp.il") {
        $ilContent = Get-Content "temp.il" -Raw
        
        if ($ilContent -match "SignatureHelper|GenerateSignature") {
            Write-Host "  ❌ 警告: 方法名未混淆！" -ForegroundColor Red
        } else {
            Write-Host "  ✅ 方法名已混淆" -ForegroundColor Green
        }
        
        if ($ilContent -match "app_test_001|secret_test") {
            Write-Host "  ❌ 警告: 字符串未加密！" -ForegroundColor Red
        } else {
            Write-Host "  ✅ 字符串已加密" -ForegroundColor Green
        }
        
        Remove-Item "temp.il" -Force
    }
} catch {
    Write-Host "  ✅ 无法反编译（最佳效果）" -ForegroundColor Green
}

# 2. 检查NecroBit
Write-Host ""
Write-Host "[2/5] 检查NecroBit保护..." -ForegroundColor Cyan
$assembly = [System.Reflection.Assembly]::LoadFile((Resolve-Path $ProtectedAssembly).Path)
$necroBitFound = $false

foreach ($type in $assembly.GetTypes()) {
    if ($type.Name -like "*NativeMethods*" -or $type.Name -like "*<Native>*") {
        $necroBitFound = $true
        break
    }
}

if ($necroBitFound) {
    Write-Host "  ✅ 检测到NecroBit Native方法" -ForegroundColor Green
} else {
    Write-Host "  ⚠ 未检测到NecroBit（可能配置有误）" -ForegroundColor Yellow
}

# 3. 检查Anti-Tampering
Write-Host ""
Write-Host "[3/5] 检查Anti-Tampering..." -ForegroundColor Cyan
$peHeader = [System.IO.File]::ReadAllBytes($ProtectedAssembly)
$tamperDetected = $false

# 简单检查是否有额外的Section
if ($peHeader.Length -gt 1MB) {
    Write-Host "  ✅ 程序集包含额外保护数据" -ForegroundColor Green
    $tamperDetected = $true
}

if (-not $tamperDetected) {
    Write-Host "  ⚠ 未明确检测到Anti-Tampering" -ForegroundColor Yellow
}

# 4. 检查Strong Name
Write-Host ""
Write-Host "[4/5] 检查Strong Name..." -ForegroundColor Cyan
try {
    $strongName = [System.Reflection.AssemblyName]::GetAssemblyName($ProtectedAssembly)
    if ($strongName.GetPublicKeyToken()) {
        Write-Host "  ✅ 程序集已签名" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 程序集未签名" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ 无法验证签名" -ForegroundColor Red
}

# 5. 文件大小检查
Write-Host ""
Write-Host "[5/5] 文件大小分析..." -ForegroundColor Cyan
$originalSize = (Get-Item "bin\Release\LicenseSystem.exe").Length / 1MB
$protectedSize = (Get-Item $ProtectedAssembly).Length / 1MB
$increase = (($protectedSize - $originalSize) / $originalSize) * 100

Write-Host "  原始大小: $($originalSize.ToString('F2')) MB"
Write-Host "  保护后大小: $($protectedSize.ToString('F2')) MB"
Write-Host "  增加: $($increase.ToString('F1'))%"

if ($increase -gt 50) {
    Write-Host "  ✅ 大小增加显著（强保护）" -ForegroundColor Green
} elseif ($increase -gt 20) {
    Write-Host "  ⚠ 大小增加适中（中等保护）" -ForegroundColor Yellow
} else {
    Write-Host "  ❌ 大小增加较少（保护可能不足）" -ForegroundColor Red
}

Write-Host ""
Write-Host "=========================================="
Write-Host "验证完成"
Write-Host "=========================================="
```

---

## 五、常见问题和解决方案

### 问题1: NecroBit导致程序崩溃

**症状:**
```
System.ExecutionEngineException: An error occurred during the execution of the native code
```

**原因:** NecroBit将方法转为Native代码,某些情况下不兼容

**解决方案:**
```xml
<!-- 降低NecroBit级别 -->
<necroBit>
  <mode>standard</mode> <!-- 从 full 改为 standard -->
</necroBit>

<!-- 或排除有问题的方法 -->
<necroBit>
  <excludeMethods>
    <method>问题方法的完整名称</method>
  </excludeMethods>
</necroBit>
```

### 问题2: XAML绑定失败

**症状:**
```
Cannot find the resource named 'XXX'
System.Windows.Markup.XamlParseException
```

**原因:** 名称混淆导致XAML无法找到类型

**解决方案:**
```xml
<nameObfuscation>
  <!-- 排除UI相关类 -->
  <excludeTypes>
    <type>LicenseSystem.Client.*Window</type>
    <type>LicenseSystem.Client.*Control</type>
  </excludeTypes>
  
  <!-- 或保留公共属性 -->
  <obfuscatePublicMembers>false</obfuscatePublicMembers>
</nameObfuscation>
```

### 问题3: 序列化失败

**症状:**
```
Newtonsoft.Json.JsonSerializationException: Could not create an instance of type
```

**原因:** 混淆后的类型名导致反序列化失败

**解决方案:**
```xml
<exclusions>
  <!-- 保留DTO类的原始名称 -->
  <types>
    <type>LicenseSystem.Client.LicenseToken</type>
    <type>LicenseSystem.Client.*Response</type>
    <type>LicenseSystem.Client.*Request</type>
  </types>
</exclusions>
```

或使用JsonProperty特性:
```csharp
public class LicenseToken
{
    [JsonProperty("token_id")]
    public string TokenId { get; set; }
    // ...
}
```

### 问题4: Anti-Debug误杀正常用户

**症状:** 程序在某些环境下无故退出

**原因:** 某些安全软件或系统工具被误判为调试器

**解决方案:**
```xml
<antiDebug>
  <!-- 使用基础模式 -->
  <mode>basic</mode>
  
  <!-- 或完全禁用（依赖其他保护） -->
  <enabled>false</enabled>
</antiDebug>
```

---

## 六、最佳实践建议

### 1. 渐进式保护

```
第1周: 基础配置
  ✓ 启用控制流混淆
  ✓ 启用名称混淆
  ✓ 启用字符串加密

第2周: 测试并调整
  ✓ 全面测试功能
  ✓ 修复混淆导致的问题
  ✓ 性能测试

第3周: 增强保护
  ✓ 启用NecroBit（关键方法）
  ✓ 启用Anti-Tampering
  ✓ 启用Anti-Debug

第4周: 最终验证
  ✓ 用户验收测试
  ✓ 尝试破解测试
  ✓ 性能基准测试
```

### 2. 保护级别选择

| 软件类型 | NecroBit | 字符串加密 | 控制流混淆 | Anti-Debug |
|---------|---------|----------|-----------|-----------|
| 免费软件 | ❌ | ✅ Basic | Level 5 | ❌ |
| 低价软件(<$100) | ⚠️ 部分 | ✅ Advanced | Level 7 | ⚠️ Basic |
| 中价软件($100-500) | ✅ Full | ✅ Advanced | Level 9 | ✅ Advanced |
| 高价软件(>$500) | ✅ Full | ✅ Extreme | Level 10 | ✅ Advanced |

### 3. 性能影响

```
预期性能影响:
- 启动时间: +10-30%（Anti-Tampering验证）
- 运行时性能: +5-15%（字符串解密开销）
- 内存使用: +5-10MB（Native代码）
- 文件大小: +30-80%

优化建议:
1. 只对关键方法使用NecroBit
2. 排除高频调用的简单方法
3. 使用"standard"而非"extreme"模式
4. 测试并调整混淆级别
```

---

## 七、总结检查清单

部署前确认:

- [ ] NecroBit已启用,覆盖所有关键方法
- [ ] 字符串加密已启用,AppSecret等敏感信息已加密
- [ ] 控制流混淆级别≥7
- [ ] Anti-Tampering已启用
- [ ] Strong Name已签名
- [ ] 所有功能测试通过（特别是XAML、序列化）
- [ ] 性能测试通过（启动<5秒,运行流畅）
- [ ] 验证脚本检查通过
- [ ] 尝试用ILSpy/dnSpy反编译（应该看到乱码）
- [ ] 生产环境真实测试

**预期效果:**
- ✅ 普通用户无法破解
- ✅ 初级破解者需要1-2周
- ✅ 中级破解者需要2-4周
- ✅ 对用户体验影响