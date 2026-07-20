# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-business ./cmd/nexa-business

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-business /app/nexa-business
RUN mkdir -p /data
ENV NEXA_DATA_DIR=/data
EXPOSE 48084
ENTRYPOINT ["/app/nexa-business"]
