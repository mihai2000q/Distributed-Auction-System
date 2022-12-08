ARG JAVA_VERSION=11
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /src/seller
COPY seller ./seller
COPY interfaces ./interfaces

COPY dtos ./dtos
COPY helper ./helper
COPY models ./models
COPY requests ./requests
COPY responses ./responses
COPY services ./services

RUN javac -Xlint ./*/*.java

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app/seller
COPY --from=builder /src/seller .

ENTRYPOINT ["java", "seller.Seller"]
