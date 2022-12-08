ARG JGROUPS_VERSION=3.6.20
FROM eclipse-temurin:17-jdk-jammy
FROM ghcr.io/scc311/jgroups:${JGROUPS_VERSION} as builder

WORKDIR /src/backend
COPY backend ./backend

COPY dtos ./dtos
COPY helper ./helper
COPY models ./models
COPY requests ./requests
COPY responses ./responses
COPY services ./services

RUN javac -Xlint ./*/*.java

FROM ghcr.io/scc311/jgroups:jre-${JGROUPS_VERSION}

WORKDIR /app/backend
COPY --from=builder /src/backend .

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "backend.Backend"]
