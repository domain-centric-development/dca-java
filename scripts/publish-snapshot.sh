#!/usr/bin/env bash
#
# Publishes a snapshot of both artifacts to the Central Portal snapshot repository. Nothing reaches
# Maven Central, the upload is repeatable, and it proves the user token works.
#
#   ./scripts/publish-snapshot.sh                    # both artifacts
#   ./scripts/publish-snapshot.sh dca-archunit       # one of them
#
set -euo pipefail

cd "$(dirname "$0")/.."
# shellcheck source=lib/central-token.sh
. scripts/lib/central-token.sh

VERSION_SUFFIX="$(sed -n 's/^buildingBlocksVersion=//p' gradle.properties)"
case "$VERSION_SUFFIX" in
  *-SNAPSHOT) ;;
  *) echo "error: gradle.properties pins released versions — pass -PbuildingBlocksVersion=X.Y.Z-SNAPSHOT yourself" >&2; exit 1 ;;
esac

if [ $# -gt 0 ]; then
  TASKS=(":$1:publishToMavenCentral")
else
  TASKS=(":dca-building-blocks:publishToMavenCentral" ":dca-archunit:publishToMavenCentral")
fi

echo "==> ${TASKS[*]}"
./gradlew "${TASKS[@]}"

echo
echo "snapshot repository: https://central.sonatype.com/repository/maven-snapshots/dev/domaincentric/"
