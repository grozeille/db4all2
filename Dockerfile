# syntax=docker/dockerfile:1

# Build stage: compiles the UI (Node via frontend-maven-plugin) and packages
# the API jar with the UI statics embedded (release profile).
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /src

COPY pom.xml ./
COPY api/pom.xml api/
COPY ui/pom.xml ui/
COPY ui/package.json ui/package-lock.json ui/
RUN mvn -B -DskipTests dependency:go-offline

COPY . .
RUN mvn -B -DskipTests package -P release

# Runtime stage: minimal JRE + packaged jar.
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /src/api/target/db4all-api-*.jar /app/app.jar

EXPOSE 8080
ENV SERVER_PORT=8080
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/data/db4all;DB_CLOSE_DELAY=-1"

VOLUME /data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
