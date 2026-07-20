# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-iam ./cmd/nexa-iam

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-iam /app/nexa-iam
COPY configs/config.docker.json /app/config.json
RUN mkdir -p /data
EXPOSE 48081
ENTRYPOINT ["/app/nexa-iam", "-config", "/app/config.json"]
