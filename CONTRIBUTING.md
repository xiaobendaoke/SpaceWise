# 贡献指南 / Contributing Guide

[English](#english-version) | 简体中�?

---

## 🙌 欢迎贡献�?

感谢你对"井井"项目的关注！我们欢迎所有形式的贡献，无论是报告 Bug、提出新功能建议、改进文档，还是直接提交代码�?

## 📋 贡献方式

### 1. 报告 Bug

如果你发现了 Bug，请�?

1. 先在 [Issues](https://github.com/xiaobendaoke/SpaceWise/issues) 中搜索，确认问题尚未被报�?
2. 如果是新问题，请创建一个新 Issue，并提供�?
   - 📱 设备信息（品牌、型号、Android 版本�?
   - 🔢 应用版本�?
   - 📝 详细的问题描�?
   - 🔄 重现步骤
   - 📸 截图或录屏（如适用�?
   - 📄 相关日志（如有）

### 2. 提出功能建议

我们欢迎新的创意！提交功能建议时请：

1. 先查�?[Issues](https://github.com/xiaobendaoke/SpaceWise/issues) 确认建议未被提出
2. 创建一个新 Issue，标注为 `enhancement`
3. 清楚描述�?
   - 🎯 你想解决什么问�?
   - 💡 你希望如何实�?
   - 🎨 可能�?UI 设计（如适用�?
   - 📊 预期对用户的价�?

### 3. 改进文档

文档同样重要！你可以�?

- 修复文档中的错别�?
- 改进措辞和表�?
- 添加更多示例
- 翻译文档到其他语言
- 补充缺失的说�?

### 4. 提交代码

我们最欢迎高质量的代码贡献�?

---

## 🔧 开发环境设�?

### 前置要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版�?
- **JDK**: 11 或更高版�?
- **Android SDK**: API 24 - API 36
- **Git**: 用于版本控制

### 安装步骤

1. **Fork 仓库**
   
   点击仓库页面右上角的 "Fork" 按钮

2. **克隆你的 Fork**
   ```bash
   git clone https://github.com/你的用户�?jingjing.git
   cd jingjing
   ```

3. **添加上游仓库**
   ```bash
   git remote add upstream https://github.com/原作�?jingjing.git
   ```

4. **打开项目**
   
   使用 Android Studio 打开项目目录

5. **同步依赖**
   
   Android Studio 会自动同�?Gradle 依赖

6. **配置 Firebase（可选）**
   
   - 如果不需�?Crashlytics，可以移除相关依�?
   - 如需测试 Crashlytics，请创建自己�?Firebase 项目

---

## 💻 代码规范

### Kotlin 风格指南

我们遵循 [Kotlin 官方代码规范](https://kotlinlang.org/docs/coding-conventions.html)，请确保�?

- �?使用 4 空格缩进
- �?类名使用 PascalCase
- �?函数和变量使�?camelCase
- �?常量使用 UPPER_SNAKE_CASE
- �?每行不超�?120 字符（适度允许例外�?

### 代码质量要求

#### 1. **文件顶部注释**

每个新文件应包含简洁的 KDoc 注释说明职责�?

```kotlin
/**
 * 物品管理组件�?
 *
 * 职责�?
 * - 处理物品的增删改查操作�?
 * - 管理物品图片和标签关联�?
 */
package com.example.myapplication
```

#### 2. **公共 API 文档**

对外暴露的类、函数应添加 KDoc�?

```kotlin
/**
 * 添加新物品到指定区域�?
 *
 * @param folderId 目标区域 ID
 * @param itemName 物品名称
 * @param imagePath 图片路径（可选）
 * @return 新创建的物品 ID
 */
suspend fun addItem(folderId: String, itemName: String, imagePath: String?): String
```

#### 3. **函数单一职责**

- 每个函数只做一件事
- 函数体不超过 40 行（复杂逻辑需拆分�?
- 避免过深的嵌套（最�?3 层）

#### 4. **避免硬编�?*

- 字符串资源放�?`strings.xml`
- 颜色定义�?`Color.kt`
- 尺寸统一管理

#### 5. **异常处理**

- I/O 操作必须�?try-catch
- 对用户友好的错误提示
- 不要吞掉异常

```kotlin
try {
    val data = loadData()
} catch (e: IOException) {
    Log.e(TAG, "Failed to load data", e)
    // 向用户显示友好提�?
}
```

---

## 🧪 测试规范

### 单元测试

- 为业务逻辑编写单元测试
- 测试文件放在 `app/src/test/` 目录
- 使用 JUnit 4 框架

```kotlin
@Test
fun `addItem should save item to database`() {
    // Given
    val folderId = "folder-1"
    val itemName = "测试物品"
    
    // When
    viewModel.addItem(folderId, itemName)
    
    // Then
    val items = viewModel.getItems(folderId).value
    assertTrue(items.any { it.name == itemName })
}
```

### 运行测试

```bash
# 运行所有单元测�?
./gradlew test

# 运行所有检查（lint + test�?
./gradlew check
```

---

## 📝 提交规范

### Commit 信息格式

使用清晰�?Commit 信息，遵循约定式规范�?

```
<类型>: <简短描�?

<详细描述>（可选）

<关联 Issue>（可选）
```

**类型标签**�?
- `feat`: 新功�?
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整（不影响功能�?
- `refactor`: 重构（既不是新功能也不是修复�?
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具配置

**示例**�?

```
feat: 添加物品批量删除功能

- 支持长按多选物�?
- 添加全�?反选按�?
- 确认对话框防止误�?

Closes #42
```

### Pull Request 流程

1. **创建功能分支**
   ```bash
   git checkout -b feature/amazing-feature
   ```

2. **开发并提交**
   ```bash
   git add .
   git commit -m "feat: add amazing feature"
   ```

3. **保持分支最�?*
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

4. **推送到你的 Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

5. **创建 Pull Request**
   - 前往 GitHub 仓库页面
   - 点击 "New Pull Request"
   - 填写 PR 模板
   - 等待代码审查

### PR 标题格式

- �?`feat: 添加保险箱功能`
- �?`fix: 修复搜索页面崩溃问题`
- �?`docs: 更新贡献指南`
- �?`update`
- �?`修改了一些东西`

### PR 描述模板

```markdown
## 📝 改动描述
简要说明这�?PR 做了什�?

## 🔗 关联 Issue
Closes #123

## �?改动类型
- [ ] 🐛 Bug 修复
- [ ] �?新功�?
- [ ] 📝 文档更新
- [ ] 🎨 UI/UX 改进
- [ ] ⚡️ 性能优化
- [ ] ♻️ 代码重构

## 🧪 测试
- [ ] 已通过单元测试
- [ ] 已手动测�?
- [ ] 已在多个设备上验�?

## 📸 截图（如适用�?
（附上前后对比截图）

## 📌 注意事项
需要特别关注的�?
```

---

## 👀 代码审查

提交 PR 后，维护者会进行代码审查。请�?

- 💬 及时回复审查意见
- 🔁 根据反馈进行修改
- �?确保所�?CI 检查通过
- 🙏 保持耐心和友�?

---

## 🎯 优先级任�?

查看 [Issues](https://github.com/xiaobendaoke/SpaceWise/issues) 中标注为�?

- `good first issue` - 适合新手
- `help wanted` - 需要帮�?
- `priority: high` - 高优先级

---

## �?常见问题

### Q: 我不熟悉 Kotlin/Compose，可以贡献吗�?

A: 当然可以！你可以从以下方面开始：
- 改进文档
- 报告 Bug
- 提出功能建议
- 翻译界面文字

### Q: 提交 PR 需要多长时间审核？

A: 通常�?3-7 个工作日内。复杂的 PR 可能需要更长时间�?

### Q: 我的 PR 被拒绝了怎么办？

A: 不要气馁！仔细阅读拒绝原因，改进后可以重新提交�?

---

## 💡 行为准则

参与本项目即表示你同意遵守：

- 🤝 尊重所有贡献�?
- 💬 使用友好和包容的语言
- 🎯 专注于对项目最有利的事�?
- 🙅 不发表人身攻击或贬损言�?

违反行为准则可能导致被禁止参与项目�?

---

## 🙏 致谢

感谢每一位贡献者的付出！你的贡献让"井井"变得更好�?

---

## English Version

# Contributing Guide

English | [简体中文](#贡献指南--contributing-guide)

## 🙌 Welcome!

Thank you for your interest in contributing to JingJing! We welcome all forms of contributions, whether it's reporting bugs, suggesting features, improving documentation, or submitting code.

## 📋 Ways to Contribute

### 1. Report Bugs

If you find a bug:

1. Search [Issues](https://github.com/xiaobendaoke/SpaceWise/issues) first
2. If it's new, create an issue with:
   - 📱 Device info (brand, model, Android version)
   - 🔢 App version
   - 📝 Detailed description
   - 🔄 Steps to reproduce
   - 📸 Screenshots/videos (if applicable)

### 2. Suggest Features

We love new ideas! When suggesting features:

1. Check [Issues](https://github.com/xiaobendaoke/SpaceWise/issues) first
2. Create a new issue labeled `enhancement`
3. Clearly describe:
   - 🎯 What problem you're solving
   - 💡 How you'd like it implemented
   - 📊 Expected value to users

### 3. Improve Documentation

Documentation is crucial! You can:

- Fix typos
- Improve clarity
- Add examples
- Translate docs
- Fill gaps

### 4. Submit Code

We love high-quality code contributions!

---

## 🔧 Development Setup

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or higher
- **JDK**: 11 or higher
- **Android SDK**: API 24 - API 36
- **Git**: For version control

### Setup Steps

1. **Fork the repository**

2. **Clone your fork**
   ```bash
   git clone https://github.com/xiaobendaoke/SpaceWise.git
   cd jingjing
   ```

3. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/originalauthor/jingjing.git
   ```

4. **Open in Android Studio**

5. **Sync dependencies**

---

## 💻 Code Standards

### Kotlin Style Guide

Follow [Kotlin Official Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- �?Use 4-space indentation
- �?Classes in PascalCase
- �?Functions/variables in camelCase
- �?Constants in UPPER_SNAKE_CASE

### Documentation

Add KDoc comments for public APIs:

```kotlin
/**
 * Add a new item to the specified folder.
 *
 * @param folderId Target folder ID
 * @param itemName Item name
 * @return ID of the newly created item
 */
suspend fun addItem(folderId: String, itemName: String): String
```

---

## 🧪 Testing

### Unit Tests

- Write tests for business logic
- Place tests in `app/src/test/`
- Use JUnit 4

```bash
# Run all tests
./gradlew test
```

---

## 📝 Commit Standards

Use clear commit messages:

```
<type>: <short description>

<detailed description> (optional)

<issue reference> (optional)
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `test`: Testing

**Example**:

```
feat: add batch delete for items

- Support long-press multi-select
- Add select all/invert buttons
- Confirmation dialog to prevent accidents

Closes #42
```

---

## 📌 Pull Request Process

1. **Create feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

2. **Develop and commit**
   ```bash
   git commit -m "feat: add amazing feature"
   ```

3. **Keep branch updated**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

4. **Push to your fork**
   ```bash
   git push origin feature/amazing-feature
   ```

5. **Create Pull Request**
   - Go to GitHub
   - Click "New Pull Request"
   - Fill out the PR template
   - Wait for review

---

## 🎯 Priority Tasks

Check [Issues](https://github.com/xiaobendaoke/SpaceWise/issues) labeled:

- `good first issue` - Great for newcomers
- `help wanted` - Need assistance
- `priority: high` - High priority

---

## 🙏 Acknowledgments

Thank you to all contributors for making JingJing better!

---

<div align="center">

**Happy Contributing! 🎉**

</div>
