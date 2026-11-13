FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/dispatch-load-balancer-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
