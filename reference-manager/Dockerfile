FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
