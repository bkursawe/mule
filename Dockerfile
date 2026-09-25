# Mühle web server. The first stage builds with Gradle, the second holds only a JRE and the application.
#   docker build -t mule .
#   docker run --rm -p 8080:8080 mule

# Same major version as jvmToolchain in build.gradle
ARG JAVA_VERSION=21

FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /workspace
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.welcome=never"

# Wrapper and build scripts only: Gradle and the dependencies for compiling and packaging land in this layer,
# which is rebuilt only when one of these files changes
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle/ gradle/
COPY engine/build.gradle engine/
COPY backend/build.gradle backend/
RUN ./gradlew --quiet downloadDependencies

# The sources without tests; the tests run in the CI build job.
# Offline, so a dependency missing from the layer above fails the build instead of being downloaded every time.
COPY engine/src/main/ engine/src/main/
COPY backend/src/main/ backend/src/main/
COPY frontend/ frontend/
RUN ./gradlew --quiet --offline :backend:installDist -Pkotlin.compiler.execution.strategy=in-process

# Our own jars change with every commit, the libraries rarely: keep them apart for separate image layers
WORKDIR /workspace/backend/build/install/backend/lib
RUN mkdir -p /app/libraries /app/application \
    && mv backend-*.jar engine-*.jar /app/application/ \
    && mv ./*.jar /app/libraries/


FROM eclipse-temurin:${JAVA_VERSION}-jre
LABEL org.opencontainers.image.title="Mühle" \
      org.opencontainers.image.description="Nine men's morris against the computer, played in the browser" \
      org.opencontainers.image.source="https://github.com/bkursawe/mule"

RUN groupadd --system --gid 10001 mule \
    && useradd --system --uid 10001 --gid mule --no-create-home --shell /usr/sbin/nologin mule
WORKDIR /app
COPY --from=build /app/libraries/ lib/
COPY --from=build /app/application/ lib/

# Numeric, so that Kubernetes can check runAsNonRoot
USER 10001:10001
# The heap follows the memory limit of the container. After an OutOfMemoryError the JVM exits, so a restart policy
# can start it anew instead of it carrying on in a broken state. Both can be overridden with -e JDK_JAVA_OPTIONS=...
ENV PORT=8080 \
    JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --start-interval=2s --retries=3 \
    CMD ["sh", "-c", "curl -fsS \"http://localhost:${PORT}/health\" || exit 1"]

# java as process 1 receives SIGTERM from docker stop directly and stops the server cleanly
ENTRYPOINT ["java", "-cp", "/app/lib/*", "de.praxisit.mule.server.ServerKt"]
