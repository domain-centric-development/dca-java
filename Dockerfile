# syntax=docker/dockerfile:1
# Library build: `docker build .` succeeds exactly when the library builds, its tests pass and the rule
# catalog renders. The image carries the built artifacts and the catalog under /out.
#   docker build -t dca-java .
#   docker run --rm dca-java                       → lists the artifacts
#   docker run --rm -v "$PWD/out:/copy" dca-java cp -r /out/. /copy   → copies them onto the host
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY dca-building-blocks ./dca-building-blocks
COPY dca-archunit ./dca-archunit
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon build :dca-archunit:rulesCatalog \
 && mkdir -p /out \
 && cp dca-building-blocks/build/libs/*.jar dca-archunit/build/libs/*.jar /out/ \
 && cp rules.json RULES.md /out/

FROM eclipse-temurin:21-jre
COPY --from=build /out /out
CMD ["ls", "-l", "/out"]
