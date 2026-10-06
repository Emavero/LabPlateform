# ---- Build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Couche de dépendances mise en cache tant que pom.xml ne change pas.
#
# Cette étape n'est qu'une optimisation : elle préremplit le dépôt local pour
# que modifier une ligne de src/ ne retélécharge pas cent trente artefacts. Son
# échec ne doit donc pas arrêter la construction — c'est « mvn package », plus
# bas, qui fait autorité sur ce dont le projet a réellement besoin, et qui
# s'arrêtera avec un message clair s'il manque quelque chose.
#
# La distinction compte depuis l'arrivée du client Compute Engine : go-offline
# résout aussi les dépendances des greffons, pour toutes les phases du cycle de
# vie, et non les seules dépendances du projet. Il échoue donc là où la
# construction, elle, aboutit — sur un artefact facultatif ou une coupure
# réseau passagère.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline \
        -Dmaven.wagon.http.retryHandler.count=3 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    || echo "Préchargement incomplet : les dépendances manquantes seront récupérées au packaging."

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
