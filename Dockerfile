# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
ENV MAVEN_OPTS="-Xms128m -Xmx384m"
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-XX:+UseG1GC", "-jar", "app.jar"]
