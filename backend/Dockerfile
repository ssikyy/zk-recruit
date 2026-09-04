FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && artifact="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" \
    && test -n "$artifact" \
    && cp "$artifact" /workspace/app.jar

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
RUN mkdir -p /data/storage && chown -R app:app /data/storage

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
