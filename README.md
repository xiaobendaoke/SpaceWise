# 井井 (JingJing)

<div align="center">

📦 **一个优雅的个人物品管理应用**

让每一件物品都井井有条

[English](#english-version) | 简体中�?

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)

</div>

---

## 📖 简�?

**井井**是一款专为家庭和个人设计的离线优先物品管理应用。它帮助你轻松记录、查找和管理散落在家中各个角落的物品，让生活更有条理�?

### �?核心特�?

#### 🏠 多层级空间管�?
- **场所（Locations�?*：支持多个空间（如我的家、父母家、办公室�?
- **区域（Folders�?*：无限层级嵌套（房间 �?家具 �?抽屉 �?隔层�?
- **可视化导�?*：面包屑导航，快速定位任何物品的位置

#### 📸 智能物品录入
- **拍照识别**：集�?Google ML Kit OCR，拍照自动识别物品名�?
- **照片存储**：每个物品可附带照片，一目了�?
- **批量添加**：支持一次性添加多个同类物�?
- **标签系统**：自定义标签分类，支持多级标�?

#### 🔍 强大的搜索功�?
- **全文搜索**：按物品名称、备注快速查�?
- **路径筛�?*：按场所、区域精确定�?
- **标签筛�?*：多维度组合筛�?
- **过期提醒**：自动追踪物品有效期

#### 📋 清单管理
- **自定义清�?*：旅行打包、购物计划随心创�?
- **智能补货**：基于库存量自动生成补货清单
- **关联物品**：清单项可关联现有物品，一键检�?

#### 🔐 保险箱功�?
- **离线加密**：重要物品单独存储，完全离线
- **生物识别**：指�?面部识别保护隐私
- **独立数据�?*：与主应用数据物理隔�?

#### 🌙 现代化设�?
- **Material 3**：遵循最�?Material Design 规范
- **深色模式**：护眼舒适的夜间主题
- **流畅动画**：原�?Jetpack Compose 打造丝滑体�?
- **离线优先**：无需网络，所有数据本地存�?

---

## 🚀 快速开�?

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版�?
- JDK 11 或更高版�?
- Android SDK (minSdk 24, targetSdk 36)
- Gradle 8.0+

### 构建步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/xiaobendaoke/SpaceWise.git
   cd jingjing
   ```

2. **配置 Firebase（可选）**
   
   如果需�?Crashlytics 功能，请�?
   - �?[Firebase Console](https://console.firebase.google.com/) 创建项目
   - 下载 `google-services.json` 并放置到 `app/` 目录
   - 如不需要，可移�?`build.gradle.kts` 中的相关依赖

3. **打开项目**
   ```bash
   # 使用 Android Studio 打开项目
   # File -> Open -> 选择项目目录
   ```

4. **同步依赖**
   ```bash
   ./gradlew build
   ```

5. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Android Studio �?Run 按钮 ▶️

---

## 🏗�?技术架�?

### 技术栈

| 技�?| 用�?|
|------|------|
| **Kotlin** | 主要开发语言 |
| **Jetpack Compose** | 声明�?UI 框架 |
| **Material 3** | UI 设计规范 |
| **Room** | 本地数据库（SQLite�?|
| **Coroutines & Flow** | 异步编程 |
| **ViewModel** | MVVM 架构组件 |
| **Navigation Compose** | 应用导航 |
| **ML Kit Text Recognition** | OCR 文字识别 |
| **DataStore** | 轻量级数据存�?|
| **WorkManager** | 后台任务调度 |
| **Biometric** | 生物识别认证 |
| **Security Crypto** | 数据加密 |

### 架构设计

```
┌─────────────────────────────────────────�?
�?         UI Layer (Compose)             �?
�? LocationsScreen, FolderBrowserScreen   �?
�? SearchScreen, ListsScreen, VaultScreen �?
└──────────────────┬──────────────────────�?
                   �?
┌──────────────────▼──────────────────────�?
�?        ViewModel Layer                 �?
�? SpaceViewModel, VaultViewModel         �?
└──────────────────┬──────────────────────�?
                   �?
┌──────────────────▼──────────────────────�?
�?      Data Layer (Repository)           �?
�? AppRepository, VaultRepository         �?
└──────────────────┬──────────────────────�?
                   �?
┌──────────────────▼──────────────────────�?
�?   Database Layer (Room DAO)            �?
�? AppDatabase, VaultDatabase             �?
└─────────────────────────────────────────�?
```

### 核心模块

- **`data/`** - 数据层（Entity、DAO、Database、Repository�?
- **`vault/`** - 保险箱功能模�?
- **`ocr/`** - OCR 识别模块
- **`storage/`** - 图片存储管理
- **`backup/`** - 备份与导�?
- **`work/`** - 后台任务（过期检查）
- **`settings/`** - 应用设置
- **`ui/theme/`** - 主题样式

详细架构说明请查�?[ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 📱 功能详解

### 场所与区域管�?

创建多层级的空间结构，模拟真实的物理位置�?

```
我的�?(Location)
├── 客厅 (Folder)
�?  ├── 电视�?(Folder)
�?  �?  └── 遥控�?(Item)
�?  └── 沙发 (Folder)
├── 卧室 (Folder)
�?  └── 衣柜 (Folder)
�?      ├── 上层 (Folder)
�?      └── 下层 (Folder)
└── 厨房 (Folder)
```

### 物品属�?

每个物品支持以下属性：
- �?名称
- �?照片
- �?备注
- �?有效�?
- �?库存数量（当�?最小）
- �?标签（多个）

### 搜索策略

- **名称搜索**：模糊匹配物品名�?
- **路径搜索**：按场所 �?区域层级筛�?
- **标签搜索**：支持多标签组合
- **过期物品**：单独视图查看即将过期的物品

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！详细贡献流程请查看 [CONTRIBUTING.md](CONTRIBUTING.md)

### 快速贡献步�?

1. Fork 本仓�?
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 开发规�?

- 遵循 [Kotlin 官方代码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 为新功能编写单元测试
- 提交前运�?`./gradlew check` 确保测试通过
- Commit 信息应清晰描述改动内�?

---

## 📄 许可�?

本项目采�?MIT 许可�?- 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代�?Android UI 工具
- [Material Design 3](https://m3.material.io/) - Google 设计规范
- [Google ML Kit](https://developers.google.com/ml-kit) - OCR 识别引擎
- 所有为本项目贡献代码的开发�?

---

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系�?

- 提交 [Issue](https://github.com/xiaobendaoke/SpaceWise/issues)
- 发起 [Discussion](https://github.com/xiaobendaoke/SpaceWise/discussions)

---

<div align="center">

**如果这个项目对你有帮助，请给一�?⭐️ Star�?*

Made with ❤️ by JingJing Contributors

</div>

---

## English Version

# JingJing

<div align="center">

📦 **An Elegant Personal Item Management App**

Keep every item organized and accessible

English | [简体中文](#井井-jingjing)

</div>

## 📖 Introduction

**JingJing** is an offline-first item management application designed for homes and individuals. It helps you easily record, find, and manage items scattered throughout your home, making life more organized.

### �?Core Features

#### 🏠 Multi-level Space Management
- **Locations**: Support for multiple spaces (e.g., My Home, Parents' Home, Office)
- **Folders**: Unlimited nesting levels (Room �?Furniture �?Drawer �?Compartment)
- **Visual Navigation**: Breadcrumb navigation to quickly locate any item

#### 📸 Smart Item Entry
- **Photo Recognition**: Integrated Google ML Kit OCR for automatic item name recognition from photos
- **Photo Storage**: Attach photos to each item for easy identification
- **Batch Addition**: Add multiple similar items at once
- **Tag System**: Custom tagging with support for hierarchical tags

#### 🔍 Powerful Search
- **Full-text Search**: Quickly find items by name or notes
- **Path Filtering**: Precisely locate by location and area
- **Tag Filtering**: Multi-dimensional combination filtering
- **Expiry Alerts**: Automatically track item expiration dates

#### 📋 List Management
- **Custom Lists**: Create lists for travel packing, shopping plans, etc.
- **Smart Restock**: Auto-generate restock lists based on inventory levels
- **Linked Items**: List items can be linked to existing items for quick checking

#### 🔐 Vault Feature
- **Offline Encryption**: Store important items separately, completely offline
- **Biometric Authentication**: Fingerprint/Face ID protects privacy
- **Separate Database**: Physically isolated from main app data

#### 🌙 Modern Design
- **Material 3**: Follows the latest Material Design guidelines
- **Dark Mode**: Eye-friendly night theme
- **Smooth Animations**: Silky experience built with native Jetpack Compose
- **Offline-first**: No network required, all data stored locally

---

## 🚀 Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or higher
- JDK 11 or higher
- Android SDK (minSdk 24, targetSdk 36)
- Gradle 8.0+

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/xiaobendaoke/SpaceWise.git
   cd jingjing
   ```

2. **Configure Firebase (Optional)**
   
   If you need Crashlytics functionality:
   - Create a project in [Firebase Console](https://console.firebase.google.com/)
   - Download `google-services.json` and place it in the `app/` directory
   - If not needed, remove related dependencies from `build.gradle.kts`

3. **Open the project**
   ```bash
   # Open with Android Studio
   # File -> Open -> Select project directory
   ```

4. **Sync dependencies**
   ```bash
   ./gradlew build
   ```

5. **Run the app**
   - Connect an Android device or start an emulator
   - Click the Run button ▶️ in Android Studio

---

## 🏗�?Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary development language |
| **Jetpack Compose** | Declarative UI framework |
| **Material 3** | UI design guidelines |
| **Room** | Local database (SQLite) |
| **Coroutines & Flow** | Asynchronous programming |
| **ViewModel** | MVVM architecture component |
| **Navigation Compose** | App navigation |
| **ML Kit Text Recognition** | OCR text recognition |
| **DataStore** | Lightweight data storage |
| **WorkManager** | Background task scheduling |
| **Biometric** | Biometric authentication |
| **Security Crypto** | Data encryption |

For detailed architecture documentation, see [ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 🤝 Contributing

We welcome all forms of contributions! For detailed contribution process, see [CONTRIBUTING.md](CONTRIBUTING.md)

### Quick Contribution Steps

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Submit a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

<div align="center">

**If this project helps you, please give it a ⭐️ Star!**

Made with ❤️ by JingJing Contributors

</div>
