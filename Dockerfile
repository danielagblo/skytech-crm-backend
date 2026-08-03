FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 skytech
WORKDIR /app
COPY --from=build /workspace/target/skytech-crm-1.0.0.jar app.jar
USER skytech
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
