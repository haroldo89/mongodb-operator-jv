FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/mongodb-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
