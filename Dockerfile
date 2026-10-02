# syntax=docker/dockerfile:1
FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests && \
    artifact="$(./mvnw -q help:evaluate -Dexpression=project.build.finalName -DforceStdout)" && \
    packaging="$(./mvnw -q help:evaluate -Dexpression=project.packaging -DforceStdout)" && \
    mkdir /out && cp "target/${artifact}.${packaging}" /out/application.war

FROM eclipse-temurin:26-jre-noble AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /out/application.war ./application.war
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.war"]
