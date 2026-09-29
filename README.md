# 个人生活记录（Personal Life Records）

一个部署在阿里云 ECS 上的**私人图片记录站**：答题通过后才能浏览内容，防止爬虫与陌生人窥探。由前身项目「奖状展示系统」改造而来。

> 线上地址：https://starcurtainone.cn （需先答对门禁题）

## 功能特性

### 访问门禁（防爬虫核心）
- 访客必须答对一道私密问题（答案仅存在服务器 `.env`，前端包中不可见）才能获得 7 天有效的签名凭证（HttpOnly Cookie，绑定 IP）
- **接口层强制拦截**：公开数据接口（主页/列表/搜索）无有效凭证一律拒绝，绕过前端直接请求 API 无效
- 失败封禁策略：5 次答错 → 封 5 分钟；24 小时内再犯 → 时长翻倍（上限 24 小时）；反复被封 → 永久拉黑（重启后端即解封）
- 答题接口独立限流（5 次/分钟），防脚本爆破
- `robots.txt` 全站 Disallow + `noindex`，搜索引擎不收录
- 答错校验全部在后端完成（fail-closed），IP 获取信任 nginx 写入的 X-Real-IP（防伪造绕过）

### 记录管理（管理员）
- 登录认证：JWT（1 小时有效期 + 无感续期 + 3 天会话绝对上限），密码 BCrypt 存储
- 上传记录：名称 + 日期 + 想法（附录）+ 图片（支持手机拍照/相册，单张最大 20MB）
- 记录浏览：置顶排序、编辑、图片替换、删除（普通管理员提交申请 → 超级管理员审核，全程消息留痕）
- 消息中心：删除申请/审核结果按未读时间角标提醒，已处理消息可删除（超管专属）
- 管理员管理：超级管理员可增删普通管理员（超管自身受保护，仅可数据库修改）
- Excel 批量导入/导出（后端接口保留）

### 基础设施
- HTTPS：Let's Encrypt 免费证书（acme.sh DNS 验证签发，每 60 天自动续期并热重载）
- 防刷限流：接口 10r/s、图片 5r/s、答题 5r/m（Nginx limit_req，按量计费服务器的刚性需求）
- 全站 301 统一到 HTTPS 主域，JSON 响应 gzip 压缩
- ICP 备案号页脚

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 21 · Spring Boot 3 · MyBatis-Plus · MySQL 8 · JJWT · Spring Security BCrypt |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · Axios · Vue Router |
| 部署 | Docker Compose（backend/mysql/nginx 三容器）· Nginx（反代+静态托管+限流+HTTPS）· acme.sh |

## 目录结构

```
├── CertificateManageSystem/        # Spring Boot 后端
│   └── src/main/java/.../
│       ├── config/                 # AuthInterceptor（登录鉴权）、GateInterceptor（答题门禁）、WebConfig
│       ├── controller/             # Certificate / Admin / Gate / DeleteAudit …
│       ├── service/                # 业务逻辑（含 GateService 门禁状态机）
│       ├── entity / mapper / dto / vo / common
│       └── src/main/resources/
│           ├── application.yml             # 本地开发配置（不入库）
│           └── application-example.yml     # 配置模板（占位符，复制后填真实值）
├── CertificateManagementWebs/
│   └── CertificateManageWeb/       # Vue3 前端
│       └── src/
│           ├── page/               # GateView（门禁）/ HomeView（主页）/ UploadView / BrowseView / Login / AdminLayout / MessagesView…
│           ├── api/                # axios 封装与接口定义
│           ├── router/ utils/ styles/
│           └── public/robots.txt   # 禁止搜索引擎收录
└── deploy/                         # 部署三件套
    ├── docker-compose.yml          # backend + mysql + nginx 编排（资源限额、健康检查、卷挂载）
    ├── backend/Dockerfile          # 后端镜像构建（Temurin JRE 21）
    ├── nginx/default.conf          # 反代、静态托管、限流、HTTPS、robots
    ├── .env.example                # 环境变量模板（DB_PASSWORD / JWT_SECRET / GATE_ANSWER / GATE_SECRET）
    └── sql/init.sql                # 数据库初始化建表
```

## 快速开始（本地开发）

```bash
# 后端：复制模板填入本地 MySQL 信息
cp CertificateManageSystem/src/main/resources/application-example.yml CertificateManageSystem/src/main/resources/application.yml

# 启动后端（9090）
cd CertificateManageSystem && mvn spring-boot:run

# 前端（5173，/api 已代理到 9090）
cd CertificateManagementWebs/CertificateManageWeb && npm install && npm run dev
```

数据库建表可用 `deploy/sql/init.sql`；管理员账号写入 `admin` 表（密码为 BCrypt 哈希）。

## 服务器部署

前置：域名 + ICP 备案、EIP、安全组放行 80/443、服务器安装 Docker。

```bash
# 服务器 /opt/cert 下
cp .env.example .env    # 填 DB_PASSWORD、JWT_SECRET（openssl rand -hex 32）、GATE_ANSWER（门禁答案）
chmod 600 .env
docker compose up -d --build
```

HTTPS 证书由 acme.sh 管理：`--install-cert` 安装到 `certs/` 目录（只读挂载进 nginx），续期后通过 reloadcmd 自动重启 nginx。更新版本 = 重新构建 jar/dist 覆盖对应目录 → `docker compose up -d --build`，数据均在 Docker 卷中持久化。

## 分支说明

- **`personal-life-records`**（默认开发分支）：个人生活记录版（当前线上版本）
- **`main`**：前身「奖状展示系统」完整版，可随时切回

## 安全要点备忘

- 所有密钥/答案仅存于 `.env`（不入库，`chmod 600`），代码仓库中只有占位符模板
- 后端鉴权默认拒绝（白名单制），新接口默认需要登录
- 公开接口仅返回 `status=0` 的记录，且全部经过答题门禁
- 全局异常处理覆盖上传超限（20MB）等场景，不向前端暴露堆栈
