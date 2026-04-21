FROM eclipse-temurin:25-jre-alpine
LABEL maintainer="bluetape4k"

ARG JAR_FILE=*.jar
COPY ${JAR_FILE} /app/mock-server.jar

EXPOSE 8888

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "/app/mock-server.jar"]
