FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

RUN chmod +x mvnw \
    && ./mvnw -DskipTests clean package \
    && JAR_FILE=$(ls target/*.jar | grep -v '\.original$' | head -n 1) \
    && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080

CMD ["sh", "-c", "java -jar /app/app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080}"]
