#!/usr/bin/env bash
#
# Releases one artifact to Maven Central from this machine. The GPG signing key never leaves it.
#
#   ./scripts/release.sh building-blocks 0.1.0
#   ./scripts/release.sh archunit 0.1.0
#   ./scripts/release.sh spring 0.1.0
#   ./scripts/release.sh archunit-spring-modulith 0.1.0
#
# The Central Portal user token is never stored in a build file. It is taken from, in order:
#   1. ORG_GRADLE_PROJECT_mavenCentralUsername / …Password in the environment
#   2. the macOS keychain item "dca-maven-central" (see RELEASING.md for how to store it)
#   3. an interactive prompt
# Signing key: DCA_SIGNING_KEY holds the fingerprint, or set it below.
#
set -euo pipefail

DEFAULT_SIGNING_KEY="6CE5EF6C96B86413FC5E1F17C6773EDA846201AB"  # gitleaks:allow — public key fingerprint, not a secret
SIGNING_KEY="${DCA_SIGNING_KEY:-$DEFAULT_SIGNING_KEY}"

cd "$(dirname "$0")/.."

die() { echo "error: $*" >&2; exit 1; }

# shellcheck source=lib/artifacts.sh
. scripts/lib/artifacts.sh

[ $# -eq 2 ] || die "usage: $0 <building-blocks|archunit|spring|archunit-spring-modulith> <version>"
ARTIFACT="$1"
VERSION="$2"

PROJECT="$(artifact_project "$ARTIFACT")" || exit 1
PROP="$(artifact_version_property "$ARTIFACT")"

[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]] || die "not a release version: $VERSION"
is_snapshot_version "$VERSION" && die "not a release version: $VERSION"

# --- preflight -------------------------------------------------------------------------------------

[ -z "$(git status --porcelain)" ] || die "working tree is dirty — commit the changelog first"

TAG="${PROJECT#dca-}/v$VERSION"
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null \
  && die "tag $TAG already exists"

grep -q "^## \[$VERSION\]" "$PROJECT/CHANGELOG.md" \
  || die "$PROJECT/CHANGELOG.md has no '## [$VERSION]' section"

DEPENDENCY="$(artifact_dependency "$ARTIFACT")"
if [ -n "$DEPENDENCY" ]; then
  DEP_VERSION="$(artifact_version "$DEPENDENCY")"
  DEP_PROP="$(artifact_version_property "$DEPENDENCY")"
  if [ -z "$DEP_VERSION" ] || is_snapshot_version "$DEP_VERSION"; then
    die "$DEP_PROP in gradle.properties is '$DEP_VERSION' — pin the released version, it becomes the POM dependency"
  fi
  echo "$PROJECT $VERSION will depend on $(artifact_project "$DEPENDENCY") $DEP_VERSION"
fi

gpg --list-secret-keys "$SIGNING_KEY" >/dev/null 2>&1 || die "no secret key $SIGNING_KEY in this keyring"

# --- Central Portal user token ---------------------------------------------------------------------

# shellcheck source=lib/central-token.sh
. scripts/lib/central-token.sh

# --- sign and publish ------------------------------------------------------------------------------

gpgconf --kill all >/dev/null 2>&1 || true

read -r -s -p "Passphrase for GPG key $SIGNING_KEY: " PASSPHRASE
echo

export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$PASSPHRASE"

# The passphrase goes in on stdin, not as an argument — arguments are visible in ps.
ORG_GRADLE_PROJECT_signingInMemoryKey="$(
  printf '%s' "$PASSPHRASE" |
    gpg --batch --pinentry-mode loopback --passphrase-fd 0 --export-secret-keys --armor "$SIGNING_KEY" 2>/dev/null
)" || true

# Loopback pinentry needs allow-loopback-pinentry in gpg-agent.conf; fall back to the agent's own dialog.
if [ -z "$ORG_GRADLE_PROJECT_signingInMemoryKey" ]; then
  echo "loopback export failed, asking the gpg agent instead"
  ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor "$SIGNING_KEY")" || true
fi

export ORG_GRADLE_PROJECT_signingInMemoryKey
case "$ORG_GRADLE_PROJECT_signingInMemoryKey" in
  *"BEGIN PGP PRIVATE KEY BLOCK"*) ;;
  *) die "exporting the secret key failed — wrong passphrase?" ;;
esac

echo "==> build and test $PROJECT $VERSION"
./gradlew ":$PROJECT:build" "-P$PROP=$VERSION"

echo "==> local dry run (signatures land in ~/.m2)"
./gradlew ":$PROJECT:publishToMavenLocal" "-P$PROP=$VERSION"
SIGNATURES="$(ls "$HOME/.m2/repository/dev/domaincentric/$PROJECT/$VERSION/"*.asc 2>/dev/null | wc -l | tr -d ' ')"
[ "$SIGNATURES" -ge 4 ] || die "expected at least 4 signatures in ~/.m2, found $SIGNATURES"
echo "$SIGNATURES signatures written"

echo
echo "About to upload $PROJECT $VERSION to the Central Portal."
echo "The deployment is validated but NOT released — publishing it is a click in the Portal."
read -r -p "type the version to confirm: " CONFIRM
[ "$CONFIRM" = "$VERSION" ] || die "aborted"

./gradlew ":$PROJECT:publishToMavenCentral" "-P$PROP=$VERSION"

# --- afterwards ------------------------------------------------------------------------------------

echo
echo "uploaded and validated. The deployment is waiting for release:"
echo "  https://central.sonatype.com/publishing/deployments"
echo
echo "next:"
echo "  1. check the deployment there, then Publish (or Drop, if something is wrong)"
echo "  2. wait until Maven Central serves it (10–30 minutes):"
echo "     https://repo1.maven.org/maven2/dev/domaincentric/$PROJECT/$VERSION/"
echo "  3. git tag ${PROJECT#dca-}/v$VERSION && git push origin ${PROJECT#dca-}/v$VERSION"
case "$PROJECT" in
  dca-building-blocks|dca-archunit)
    echo "  4. set $PROP=$VERSION in gradle.properties and commit — the artifacts depending on it read it from there" ;;
esac
