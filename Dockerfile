FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY event-common/pom.xml event-common/pom.xml
COPY event-ingest/pom.xml event-ingest/pom.xml
COPY event-engine/pom.xml event-engine/pom.xml
COPY event-admin/pom.xml event-admin/pom.xml
RUN mvn dependency:go-offline -B -q 2>/dev/null || true
COPY event-common/src event-common/src
COPY event-ingest/src event-ingest/src
COPY event-engine/src event-engine/src
COPY event-admin/src event-admin/src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
