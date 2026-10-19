# ============ 构建阶段 ============
FROM node:20-alpine AS build
WORKDIR /app

# 先复制依赖清单，用 npmmirror 加速 + npm ci 精确安装
COPY package.json package-lock.json ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ============ 运行阶段 ============
FROM nginx:alpine

# 构建产物
COPY --from=build /app/dist /usr/share/nginx/html
# nginx 配置（静态托管 + /api 反向代理）
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
