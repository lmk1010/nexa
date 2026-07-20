# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-ai ./cmd/nexa-ai

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-ai /app/nexa-ai
RUN mkdir -p /data
ENV NEXA_DATA_DIR=/data
EXPOSE 48089
ENTRYPOINT ["/app/nexa-ai"]
