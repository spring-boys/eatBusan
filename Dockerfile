FROM amazoncorretto:21-alpine
COPY ./build/libs/*SNAPSHOT.jar /project.jar
ENTRYPOINT ["java", "-jar", "/project.jar"]