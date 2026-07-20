# syntax=docker/dockerfile:1
FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/nexa-op ./cmd/nexa-op

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /out/nexa-op /app/nexa-op
RUN mkdir -p /data
ENV NEXA_DATA_DIR=/data
EXPOSE 48088
ENTRYPOINT ["/app/nexa-op"]
