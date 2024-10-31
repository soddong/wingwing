FROM gradle:8.8-jdk-focal AS build



WORKDIR /app

COPY build.gradle settings.gradle ./

RUN apt-get update && apt-get install -y openjdk-21-jdk

COPY . /app

RUN gradle clean build --no-daemon -x test

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/main-server.jar

EXPOSE 8080

ENTRYPOINT [ "java" ]
CMD [ "-jar", "main-server.jar", "--spring.profiles.active=prod"]