FROM openjdk:8-alpine

COPY target/uberjar/namr.jar /namr/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/namr/app.jar"]
