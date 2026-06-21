FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn .mvn
RUN ./mvnw --version
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
