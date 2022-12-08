ARG JAVA_VERSION=11
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /src/buyer
COPY buyer ./buyer
COPY interfaces ./interfaces

COPY dtos ./dtos
COPY helper ./helper
COPY models ./models
COPY requests ./requests
COPY responses ./responses
COPY services ./services

RUN javac -Xlint ./*/*.java

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app/buyer
COPY --from=builder /src/buyer .

ENTRYPOINT ["java", "buyer.Buyer"]
