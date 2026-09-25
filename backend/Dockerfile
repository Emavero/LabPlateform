# ---- Build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Couche de dépendances mise en cache tant que pom.xml ne change pas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Les tests tournent en CI (.github/workflows/tests.yml) ; l'image se contente de packager.
RUN mvn -B -q package -DskipTests

# ---- Runtime --------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
# Client Docker : utilisé uniquement en mode APP_HYPERVISOR_MODE=docker, pour
# piloter les conteneurs des machines Linux via le socket de l'hôte.
# easy-rsa : autorité de certification des profils VPN des utilisateurs.
RUN apk add --no-cache docker-cli easy-rsa openssl \
 && addgroup -S app && adduser -S app -G app \
 && mkdir -p /var/lib/labplatform/vpn && chown app:app /var/lib/labplatform/vpn
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
