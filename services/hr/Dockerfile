# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-hr ./cmd/nexa-hr

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-hr /app/nexa-hr
COPY configs/config.docker.json /app/config.json
RUN mkdir -p /data
EXPOSE 48083
ENTRYPOINT ["/app/nexa-hr", "-config", "/app/config.json"]
