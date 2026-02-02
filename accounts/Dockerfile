FROM gradle:8.4-jdk21 as build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon -x test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
RUN apk update && apk add --no-cache curl jq
COPY --from=build /app/build/libs/*.jar /app/application.jar
COPY --from=build /app/src/main/resources/static/public /app/src/main/resources/static/public
ENTRYPOINT ["java", "-jar", "application.jar"]