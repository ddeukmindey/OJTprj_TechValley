# Multi-stage build: build React (Vite) frontend, rồi bake dist/ vào image nginx
# cùng với nginx.conf hiện có. Mục đích: máy bàn giao KHÔNG CẦN cài Node/npm,
# chỉ cần `docker compose up --build` là xong — đúng tinh thần "production,
# chỉ đổi IP".

# ---- Stage 1: build frontend ----
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: nginx phục vụ static + reverse proxy ----
FROM nginx:1.25-alpine
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html
