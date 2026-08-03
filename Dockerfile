FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 skytech \
    && useradd --system --uid 10001 --gid skytech --no-create-home skytech
WORKDIR /app
RUN mkdir -p /app/uploads/profiles && chown -R skytech:skytech /app
COPY --from=build --chown=skytech:skytech /workspace/target/skytech-crm-1.0.0.jar app.jar
USER skytech
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
