# ---------- Etapa de build ----------
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
# Descarga dependencias en una capa separada para aprovechar cache de Docker
RUN ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Etapa de runtime ----------
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Limite de memoria de la JVM para no exceder los recursos del plan gratuito
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m"

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
