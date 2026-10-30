FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S chamados && adduser -S chamados -G chamados
COPY --from=build /app/target/sistema-chamados-0.0.1-SNAPSHOT.jar app.jar
USER chamados
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
