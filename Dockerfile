# syntax=docker/dockerfile:1
# Library build: `docker build .` succeeds exactly when the library builds, its tests pass, the rule
# catalog renders and every jar carries the license. The image holds the artifacts and the catalog
# under /out.
#   docker build -t dca-java .
#   docker run --rm dca-java                       → lists the artifacts
#   docker run --rm -v "$PWD/out:/copy" dca-java cp -r /out/. /copy   → copies them onto the host
# Fully qualified image names, so the build works under Podman without a registry alias.
FROM docker.io/library/eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties LICENSE ./
COPY gradle ./gradle
COPY dca-building-blocks ./dca-building-blocks
COPY dca-archunit ./dca-archunit
COPY dca-spring ./dca-spring
COPY dca-archunit-modulith ./dca-archunit-modulith
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon build :dca-archunit:rulesCatalog \
 && mkdir -p /out \
 && cp dca-building-blocks/build/libs/*.jar dca-archunit/build/libs/*.jar dca-spring/build/libs/*.jar dca-archunit-modulith/build/libs/*.jar /out/ \
 && cp rules.json RULES.md /out/ \
 && for jar in /out/*.jar; do \
      jar tf "$jar" | grep -qx 'META-INF/LICENSE' || { echo "no META-INF/LICENSE in $jar"; exit 1; }; \
    done

FROM docker.io/library/eclipse-temurin:21-jre
COPY --from=build /out /out
CMD ["ls", "-l", "/out"]
