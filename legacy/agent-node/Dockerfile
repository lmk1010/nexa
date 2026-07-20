ARG NODE_IMAGE=node:20-bullseye-slim

FROM ${NODE_IMAGE} AS deps

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

FROM ${NODE_IMAGE}

ENV NODE_ENV=production
ENV TZ=Asia/Shanghai
ENV PORT=48091

# apt/pip 换国内源 —— debian.org / pypi.org 到国内奇慢；腾讯云机上直接走内网 mirror
# python 数据栈（pandas/numpy/openpyxl）是重点：直接 apt 二进制包，避免 pip 编译
RUN sed -i 's|http://deb.debian.org|http://mirrors.aliyun.com|g; s|http://security.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list \
    && apt-get update && apt-get install -y --no-install-recommends \
        jq \
        xxd \
        python3 \
        python3-pip \
        python3-pandas \
        python3-openpyxl \
        python3-numpy \
        python3-dateutil \
    && pip3 config set global.index-url https://mirrors.aliyun.com/pypi/simple/ \
    && pip3 install --no-cache-dir xlsxwriter==3.2.0 \
    && rm -rf /var/lib/apt/lists/* /root/.cache

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    groupadd -g 1001 kyx && \
    useradd -m -u 1001 -g kyx -s /bin/bash kyx

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY package*.json ./
COPY src ./src
COPY generated ./generated
COPY templates ./templates

# workspace mount point —— 宿主机 bind-mount /opt/kyx/agent-workspace 到这里
RUN mkdir -p /app/logs /workspace && chown -R kyx:kyx /app /workspace

USER kyx

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:' + (process.env.PORT || 48091) + '/actuator/health').then(r => r.ok ? process.exit(0) : process.exit(1)).catch(() => process.exit(1))"

EXPOSE 48091

CMD ["node", "src/index.js"]
