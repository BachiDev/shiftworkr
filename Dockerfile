# Stage 1: Build & Vaadin Production Bundle
# NOTE: jdk-jammy builder carries 1 HIGH (libssl3 CVE-2026-84782, fix .30 not yet in
# Ubuntu jammy as of 2026-10-15). Builder-only, never shipped — runtime below is clean.
FROM eclipse-temurin:21-jdk-jammy AS builder

# Maven (no wrapper in repo) — pinned binary from Apache archive
ARG MAVEN_VERSION=3.9.9
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz \
  && tar xzf /tmp/maven.tar.gz -C /opt \
  && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/bin/mvn \
  && rm /tmp/maven.tar.gz \
  && apt-get purge -y curl \
  && apt-get autoremove -y \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src src
COPY frontend frontend
RUN mvn clean package -Pproduction -DskipTests

# Stage 2: Minimal Production JRE Runtime (alpine = smallest CVE surface;
# glibc only matters for the Vaadin Node build in the builder stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/shiftworkr-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
