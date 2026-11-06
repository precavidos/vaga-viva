# Build stage: tests run in CI (they need Docker for Testcontainers), so the image build skips them.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests \
	&& java -Djarmode=tools -jar target/vaga-viva-*.jar extract --layers --launcher --destination target/extracted

# Runtime stage: JRE only, non-root user, layers ordered from least to most frequently changed.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 vagaviva
WORKDIR /app
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
USER vagaviva
EXPOSE 8080
# Free hosting plans have little memory: cap the heap relative to the container limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
