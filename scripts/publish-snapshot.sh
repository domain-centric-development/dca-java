#!/usr/bin/env bash
#
# Publishes a snapshot of one or both artifacts to the Central Portal snapshot repository. Nothing
# reaches Maven Central, the upload is repeatable, and it proves the user token works.
#
#   ./scripts/publish-snapshot.sh                                  # both artifacts
#   ./scripts/publish-snapshot.sh dca-archunit                     # one of them
#   ./scripts/publish-snapshot.sh dca-building-blocks -PbuildingBlocksVersion=0.2.0-SNAPSHOT
#
# Every artifact that is actually requested must resolve to a -SNAPSHOT version — from
# gradle.properties or from a -P override, which is forwarded to Gradle. The check runs before any
# credential is loaded or anything is uploaded.
#
set -euo pipefail

cd "$(dirname "$0")/.."
# shellcheck source=lib/artifacts.sh
. scripts/lib/artifacts.sh

ARTIFACTS=()
GRADLE_ARGS=()
for arg in "$@"; do
  case "$arg" in
    -P*) GRADLE_ARGS+=("$arg") ;;
    *)   ARTIFACTS+=("$arg") ;;
  esac
done
[ ${#ARTIFACTS[@]} -gt 0 ] || ARTIFACTS=(dca-building-blocks dca-archunit dca-spring dca-archunit-spring-modulith)

TASKS=()
for artifact in "${ARTIFACTS[@]}"; do
  project="$(artifact_project "$artifact")"
  version="$(artifact_version "$artifact" "${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"}")"
  if ! is_snapshot_version "$version"; then
    property="$(artifact_version_property "$artifact")"
    echo "error: $project resolves to '$version', not a snapshot — pass -P$property=X.Y.Z-SNAPSHOT or change gradle.properties" >&2
    exit 1
  fi
  echo "$project $version"
  TASKS+=(":$project:publishToMavenCentral")
done

# shellcheck source=lib/central-token.sh
. scripts/lib/central-token.sh

echo "==> ${TASKS[*]} ${GRADLE_ARGS[*]+"${GRADLE_ARGS[*]}"}"
./gradlew "${TASKS[@]}" "${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"}"

echo
echo "snapshot repository: https://central.sonatype.com/repository/maven-snapshots/dev/domaincentric/"
