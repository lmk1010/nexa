# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-bpm ./cmd/nexa-bpm

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-bpm /app/nexa-bpm
COPY configs/config.docker.json /app/config.json
RUN mkdir -p /data
EXPOSE 48082
ENTRYPOINT ["/app/nexa-bpm", "-config", "/app/config.json"]
