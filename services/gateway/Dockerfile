# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-gateway ./cmd/nexa-gateway

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-gateway /app/nexa-gateway
RUN mkdir -p /data
ENV NEXA_DATA_DIR=/data
EXPOSE 48080
ENTRYPOINT ["/app/nexa-gateway"]
