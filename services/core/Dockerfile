# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/nexa-core ./cmd/nexa-core

FROM alpine:3.20
RUN apk add --no-cache ca-certificates tzdata wget \
  && adduser -D -H -u 10001 nexa
WORKDIR /app
COPY --from=build /out/nexa-core /app/nexa-core
COPY configs/config.docker.json /app/config.json
RUN mkdir -p /data && chown -R nexa:nexa /data /app
USER nexa
ENV NEXA_DATA_DIR=/data
EXPOSE 48080
ENTRYPOINT ["/app/nexa-core", "-config", "/app/config.json"]
