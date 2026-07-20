ARG GO_IMAGE=golang:1.25-alpine
ARG RUNTIME_IMAGE=alpine:3.20

FROM ${GO_IMAGE} AS builder

WORKDIR /src

# 换国内 goproxy 加速依赖拉取
ENV GOPROXY=https://goproxy.cn,direct \
    CGO_ENABLED=0 \
    GOOS=linux

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN go build -trimpath -ldflags="-s -w" -o /out/kyx-data-center ./cmd/kyx-data-center

FROM ${RUNTIME_IMAGE}

RUN apk add --no-cache tzdata ca-certificates && \
    ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo Asia/Shanghai > /etc/timezone && \
    addgroup -g 1001 kyx && \
    adduser -D -u 1001 -G kyx kyx && \
    mkdir -p /data/exports && chown -R kyx:kyx /data

USER kyx
WORKDIR /app

COPY --from=builder /out/kyx-data-center /app/kyx-data-center

EXPOSE 48092

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:48092/actuator/health || exit 1

ENTRYPOINT ["/app/kyx-data-center"]
CMD ["-config", "/etc/kyx-data-center/config.yaml"]
