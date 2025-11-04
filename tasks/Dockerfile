FROM gradle:8.4-jdk21 as build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon -x test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]
