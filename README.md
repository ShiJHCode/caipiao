# 大乐透智能选号系统

基于中国体育彩票官网大乐透历史开奖数据，进行多维度统计分析，并依据概率分布与回归期望生成推荐号码。

> 免责声明：彩票开奖为独立随机事件，历史统计无法预测未来开奖结果。本系统仅供数据分析与学习参考，请理性购彩。

## 技术栈

| 层级 | 技术 |
| ---- | ---- |
| 前端 | Vue 3 + Vite + Axios |
| 后端 | Spring Boot 3.2 + Spring Data JPA + Maven |
| 数据库 | MySQL 8（172.16.206.128:3306，root / 123456） |
| 数据源 | 中国体育彩票官网大乐透历史开奖接口（gameNo=85） |

## 目录结构

```
caipiao/
├── sql/
│   └── schema.sql              # 建库建表语句
├── backend/                    # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/java/com/caipiao/dlt/
│       ├── DltApplication.java
│       ├── config/CorsConfig.java
│       ├── entity/DltNumber.java
│       ├── repository/DltNumberRepository.java
│       ├── dto/                # ApiResponse / BetNumber / RecommendResponse / StatisticsInfo ...
│       ├── service/            # CrawlService / StatisticsService / RecommendService
│       └── controller/LotteryController.java
│   └── src/main/resources/application.yml
├── frontend/                   # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js / App.vue / style.css
│       ├── api/index.js
│       └── components/         # RecommendPanel / StatisticsPanel / HistoryPanel / BallGroup
└── README.md
```

## 快速开始

### 1. 初始化数据库

在 `172.16.206.128` 的 MySQL 上执行 `sql/schema.sql`：

```bash
mysql -h 172.16.206.128 -uroot -p123456 < sql/schema.sql
```

脚本会创建数据库 `caipiao_dlt` 和表 `dlt_number`（含 `uk_issue` 唯一索引，保证幂等入库）。

### 2. 启动后端（Maven）

```bash
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。数据库连接等配置见 `src/main/resources/application.yml`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，`/api` 请求已通过 Vite 代理转发到后端 8080。

打开浏览器访问 `http://localhost:5173`，点击「获取最新开奖号码」即可首次拉取最新 150 期并生成推荐。

## 接口说明

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/lottery/fetch?count=150` | 拉取最新开奖数据（首次默认 150 期，之后按期号增量入库） |
| GET  | `/api/lottery/history?limit=100` | 查询历史开奖（按开奖日期降序） |
| GET  | `/api/lottery/statistics` | 获取统计分析（分布 + 遗漏 + 冷热频次） |
| GET  | `/api/lottery/recommend` | 生成 5 注推荐号码 |

## 统计维度与选号逻辑

系统依据大乐透历史数据计算六大统计维度，并与理论概率 / 理论回归周期对比：

1. **空间分布**：前区按 `1-12 / 13-24 / 25-35` 分三区，统计三区全有 / 至少断一区 / 断两区。
2. **结构特征 · 奇偶**：前区 5 码的奇偶配比（3奇2偶、2奇3偶等）。
3. **跨度分布**：前区最大值 - 最小值（20-30、15-19、<15、>30）。
4. **重号**：与上一期前区号码的重复个数（0 / 1 / 2 / 3 个）。
5. **连号**：相邻号码连号组数及最长连号长度。
6. **后区奇偶**：后区 2 码的奇偶配比（1奇1偶、2奇、0奇）。

### 推荐号码生成策略

- **前 3 注 —— 回归期望 + 概率最大**：以概率最大的分布为核心约束（3奇2偶 / 2奇3偶、跨度 20-30、三区覆盖），并结合「当前遗漏 ≥ 理论回归周期」的欠回归特征择优命中。
- **后 2 注 —— 最冷号 + 回归期望 + 概率最大**：从最近 30 期频次最低的号码（冷号池）中选号，叠加回归期望与概率最大约束；冷号池受限时逐级放宽并自动兜底。

## 关键实现说明

- **爬虫**：调用体彩官网接口 `webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry?gameNo=85`，分页拉取（pageSize 上限约 100），按期号去重增量入库。返回结果 `lotteryDrawResult` 为 `"03 10 12 20 25 01 09"` 格式，前 5 码为前区、后 2 码为后区。
- **幂等**：`dlt_number` 表的 `issue` 字段唯一索引，重复拉取不会产生脏数据。
- **冷热号**：最近 30 期（`lottery.crawl.cold-window`）内统计 1-35 / 1-12 的频次与遗漏，按频次升序（同频次遗漏降序）排序得到冷号池。

## 双色球（`/api/ssq/*`）

在现有基础上新增双色球选号系统，接口与大乐透对称：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/ssq/fetch?count=150` | 拉取双色球最新开奖（福利彩票官网接口，一次最多 200 期） |
| GET  | `/api/ssq/history?limit=100` | 查询双色球历史开奖 |
| GET  | `/api/ssq/statistics` | 双色球统计分析（红球三区 1-11/12-22/23-33、奇偶、跨度、重号、连号、蓝球奇偶） |
| GET  | `/api/ssq/recommend` | 生成双色球 5 注推荐（红 6 + 蓝 1） |

前端主页可选择「大乐透」或「双色球」，路由 `/dlt`、`/ssq`。

## Docker 部署（OrbStack）

项目已 Docker 化，通过 `docker compose` 一键部署到本机 OrbStack。数据库仍使用远程 MySQL（`172.16.206.128`）。

### 1. 构建并启动

```bash
docker compose up -d --build
```

### 2. 访问地址

| 服务 | 地址 |
| ---- | ---- |
| 前端（nginx 托管 + `/api` 反向代理） | http://localhost:18080 |
| 后端（调试端口） | http://localhost:18081 |

### 3. 配置说明

- 后端镜像：多阶段构建（`maven:3.9-eclipse-temurin-17` 构建 → `eclipse-temurin:17-jre` 运行），Maven 使用阿里云镜像加速。
- 前端镜像：多阶段构建（`node:20-alpine` 构建 → `nginx:alpine` 运行），npm 使用 npmmirror 加速。
- 数据库连接通过环境变量 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 覆盖（默认值见 `docker-compose.yml`，与远程 MySQL 一致）。
- 前端容器 `18080:80`、后端容器 `18081:8080`（使用冷门端口，避开 8080 等常见端口）。

### 4. 停止

```bash
docker compose down
```
