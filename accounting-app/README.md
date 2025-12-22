# 记账软件 (Accounting Application)

一个跨平台的记账软件，支持Windows、iOS和Android平台。使用Java开发，提供完整的账目管理、预算管理、数据同步和图表展示功能。

## 📋 项目概述

本项目是一个功能完整的记账软件，代码量约4000行，包含以下核心模块：

### 🧩 核心功能

1. **账目管理模块** (900-1100行)
   - 交易记录增删改查
   - 多条件过滤（日期/分类/金额/关键字）
   - CSV导入/导出
   - 自动生成ID

2. **预算管理模块** (700-900行)
   - 月度预算设置
   - 实时计算已用金额
   - 超额红色提醒
   - 消费趋势预测（线性回归）

3. **多端数据同步模块** (900-1100行)
   - 用户注册/登录
   - Token认证
   - REST API接口
   - 离线缓存与合并算法
   - 冲突解决策略（取最新）

4. **图表展示模块** (800-1000行)
   - 柱状图、饼图、折线图
   - 分类统计图
   - 趋势分析图
   - 导出PNG/PDF

## 🛠️ 技术栈

- **后端框架**: Spring Boot 3.1.5
- **UI框架**: JavaFX 21
- **数据库**: SQLite
- **数据格式**: JSON
- **图表库**: JavaFX Charts
- **PDF生成**: Apache PDFBox
- **构建工具**: Maven

## 📁 项目结构

```
accounting-app/
├── pom.xml                          # Maven配置文件
├── README.md                        # 项目说明文档
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── accounting/
│   │   │           ├── AccountingApplication.java    # Spring Boot主类
│   │   │           ├── model/                        # 数据模型
│   │   │           │   ├── Transaction.java          # 交易实体
│   │   │           │   ├── Category.java             # 分类实体
│   │   │           │   ├── Budget.java               # 预算实体
│   │   │           │   └── User.java                 # 用户实体
│   │   │           ├── service/                      # 业务逻辑层
│   │   │           │   ├── TransactionService.java   # 交易服务
│   │   │           │   ├── BudgetService.java        # 预算服务
│   │   │           │   ├── StatisticService.java     # 统计服务
│   │   │           │   ├── UserService.java          # 用户服务
│   │   │           │   └── SyncService.java          # 同步服务
│   │   │           ├── filter/                       # 过滤模块
│   │   │           │   └── FilterRule.java           # 过滤规则
│   │   │           ├── chart/                        # 图表模块
│   │   │           │   ├── Chart.java                # 图表基类
│   │   │           │   ├── BarChart.java             # 柱状图
│   │   │           │   ├── PieChart.java              # 饼图
│   │   │           │   ├── LineChart.java            # 折线图
│   │   │           │   └── ChartAnalyzer.java        # 图表分析器
│   │   │           ├── storage/                      # 存储模块
│   │   │           │   └── StorageManager.java       # 存储管理器
│   │   │           ├── api/                           # API控制器
│   │   │           │   └── SyncController.java       # 同步API
│   │   │           ├── ui/                            # UI模块
│   │   │           │   ├── MainApplication.java      # JavaFX主类
│   │   │           │   └── MainController.java       # UI控制器
│   │   │           └── config/                        # 配置类
│   │   │               └── AppConfig.java            # 应用配置
│   │   └── resources/
│   │       └── application.properties                # Spring Boot配置
│   └── test/                                         # 测试代码
└── data/                                             # 数据存储目录（运行时生成）
```

## 🚀 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- Windows/Linux/macOS

### 安装步骤

1. **进入项目目录**
   ```bash
   cd E:\accounting-app
   ```

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **运行桌面应用（JavaFX）**
   ```bash
   mvn exec:java -Dexec.mainClass="com.accounting.ui.MainApplication"
   ```
   或双击运行 `run-desktop.bat`

4. **运行后端API服务器**
   ```bash
   mvn spring-boot:run
   ```
   或双击运行 `run-server.bat`
   服务器将在 `http://localhost:8080` 启动

## 📖 使用说明

### 桌面应用功能

1. **添加交易**
   - 选择交易类型（支出/收入）
   - 输入金额和描述
   - 选择分类和日期
   - 点击"添加"按钮

2. **查看交易**
   - 在交易列表中查看所有记录
   - 使用搜索框过滤交易

3. **设置预算**
   - 输入月度预算金额
   - 系统会自动计算使用率
   - 超额时会显示红色提醒

4. **查看图表**
   - 切换到"图表统计"标签页
   - 查看分类支出饼图
   - 查看月度支出柱状图

5. **导入/导出**
   - 导出：将交易数据导出为CSV文件
   - 导入：从CSV文件导入交易数据

### API接口

#### 用户注册
```http
POST /api/sync/register
Content-Type: application/json

{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123"
}
```

#### 用户登录
```http
POST /api/sync/login
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}
```

#### 获取交易数据
```http
GET /api/sync/transactions
Authorization: <token>
```

#### 上传交易数据
```http
POST /api/sync/transactions/upload
Authorization: <token>
Content-Type: application/json

[
  {
    "type": "EXPENSE",
    "amount": 100.0,
    "description": "午餐",
    "categoryId": "food",
    "date": "2024-01-15T12:00:00"
  }
]
```

## 🔧 配置说明

### application.properties

```properties
# 服务器端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:sqlite:data/accounting.db

# 日志级别
logging.level.root=INFO
logging.level.com.accounting=DEBUG
```

## 📊 代码统计

- **总代码行数**: 约4000行
- **账目管理模块**: ~1000行
- **预算管理模块**: ~800行
- **数据同步模块**: ~1000行
- **图表展示模块**: ~900行
- **其他（配置、工具类等）**: ~300行

## 🎯 功能特性

### ✅ 已实现功能

- [x] 交易记录增删改查
- [x] 多条件过滤（日期、分类、金额、关键字）
- [x] CSV导入/导出
- [x] 月度预算设置
- [x] 超额提醒
- [x] 消费趋势预测
- [x] 用户注册/登录
- [x] REST API接口
- [x] 数据同步（基础实现）
- [x] 柱状图、饼图、折线图
- [x] 图表导出PNG/PDF

### 🔮 扩展功能（可选）

- [ ] 多币种支持
- [ ] 深色/浅色主题切换
- [ ] 本地密码锁（PIN验证）
- [ ] 数据加密存储

## 🐛 已知问题

1. JavaFX在某些环境下可能需要额外配置模块路径
2. PDF导出功能需要确保PDFBox库正确加载
3. 移动端（iOS/Android）需要通过REST API调用，需要单独开发客户端

## 📝 开发说明

### 代码组织

- **模型层**: 定义数据实体（Transaction, Category, Budget, User）
- **服务层**: 实现业务逻辑（TransactionService, BudgetService等）
- **控制层**: 处理HTTP请求（SyncController）
- **UI层**: JavaFX界面（MainController, MainApplication）
- **工具层**: 辅助功能（StorageManager, FilterRule等）

### 设计模式

- **策略模式**: FilterRule实现多条件过滤
- **模板方法模式**: Chart基类定义图表通用接口
- **单例模式**: StorageManager管理文件存储
- **工厂模式**: ChartAnalyzer创建不同类型的图表

## 📄 许可证

本项目仅供学习和教育用途。

## 👥 贡献

欢迎提交Issue和Pull Request！

## 📧 联系方式

如有问题或建议，请通过Issue反馈。

---

**注意**: 本项目是一个教学示例项目，实际生产环境使用需要进一步完善安全性和错误处理。

