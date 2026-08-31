#!/usr/bin/env bash
#
# Releases one artifact to Maven Central from this machine. The GPG signing key never leaves it.
#
#   ./scripts/release.sh building-blocks 0.1.0
#   ./scripts/release.sh archunit 0.1.0
#
# The Central Portal user token is never stored in a build file. It is taken from, in order:
#   1. ORG_GRADLE_PROJECT_mavenCentralUsername / …Password in the environment
#   2. the macOS keychain item "dca-maven-central" (see RELEASING.md for how to store it)
#   3. an interactive prompt
# Signing key: DCA_SIGNING_KEY holds the fingerprint, or set it below.
#
set -euo pipefail

DEFAULT_SIGNING_KEY="6CE5EF6C96B86413FC5E1F17C6773EDA846201AB"
SIGNING_KEY="${DCA_SIGNING_KEY:-$DEFAULT_SIGNING_KEY}"

cd "$(dirname "$0")/.."

die() { echo "error: $*" >&2; exit 1; }

[ $# -eq 2 ] || die "usage: $0 <building-blocks|archunit> <version>"
ARTIFACT="$1"
VERSION="$2"

case "$ARTIFACT" in
  building-blocks) PROJECT="dca-building-blocks"; PROP="buildingBlocksVersion" ;;
  archunit)        PROJECT="dca-archunit";        PROP="archunitVersion" ;;
  *) die "unknown artifact '$ARTIFACT' (building-blocks|archunit)" ;;
esac

[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]] || die "not a release version: $VERSION"

# --- preflight -------------------------------------------------------------------------------------

[ -z "$(git status --porcelain)" ] || die "working tree is dirty — commit the changelog first"

git rev-parse -q --verify "refs/tags/$ARTIFACT/v$VERSION" >/dev/null \
  && die "tag $ARTIFACT/v$VERSION already exists"

grep -q "^## \[$VERSION\]" "$PROJECT/CHANGELOG.md" \
  || die "$PROJECT/CHANGELOG.md has no '## [$VERSION]' section"

if [ "$ARTIFACT" = "archunit" ]; then
  BB_VERSION="$(sed -n 's/^buildingBlocksVersion=//p' gradle.properties)"
  case "$BB_VERSION" in
    *-SNAPSHOT|"") die "buildingBlocksVersion in gradle.properties is '$BB_VERSION' — pin the released version, it becomes the POM dependency" ;;
  esac
  echo "dca-archunit $VERSION will depend on dca-building-blocks $BB_VERSION"
fi

gpg --list-secret-keys "$SIGNING_KEY" >/dev/null 2>&1 || die "no secret key $SIGNING_KEY in this keyring"

# --- Central Portal user token ---------------------------------------------------------------------

KEYCHAIN_ITEM="dca-maven-central"

if [ -n "${ORG_GRADLE_PROJECT_mavenCentralUsername:-}" ] && [ -n "${ORG_GRADLE_PROJECT_mavenCentralPassword:-}" ]; then
  echo "using the Central token from the environment"
elif TOKEN="$(security find-generic-password -s "$KEYCHAIN_ITEM" -w 2>/dev/null)"; then
  # Stored as "username:password" — the token halves contain no colon.
  export ORG_GRADLE_PROJECT_mavenCentralUsername="${TOKEN%%:*}"
  export ORG_GRADLE_PROJECT_mavenCentralPassword="${TOKEN#*:}"
  unset TOKEN
  echo "using the Central token from the keychain item $KEYCHAIN_ITEM"
else
  echo "no Central token in the environment or keychain item $KEYCHAIN_ITEM"
  read -r -p "Central Portal token username: " CENTRAL_USERNAME
  read -r -s -p "Central Portal token password: " CENTRAL_PASSWORD
  echo
  export ORG_GRADLE_PROJECT_mavenCentralUsername="$CENTRAL_USERNAME"
  export ORG_GRADLE_PROJECT_mavenCentralPassword="$CENTRAL_PASSWORD"
  unset CENTRAL_USERNAME CENTRAL_PASSWORD
fi

[ -n "$ORG_GRADLE_PROJECT_mavenCentralUsername" ] || die "no Central token username"
[ -n "$ORG_GRADLE_PROJECT_mavenCentralPassword" ] || die "no Central token password"

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
echo "About to upload $PROJECT $VERSION to Maven Central and release the deployment."
echo "This is irreversible — a released version can never be replaced or removed."
read -r -p "type the version to confirm: " CONFIRM
[ "$CONFIRM" = "$VERSION" ] || die "aborted"

./gradlew ":$PROJECT:publishToMavenCentral" "-P$PROP=$VERSION"

# --- afterwards ------------------------------------------------------------------------------------

echo
echo "uploaded and released. Maven Central serves it in 10–30 minutes:"
echo "  https://repo1.maven.org/maven2/dev/domaincentric/$PROJECT/$VERSION/"
echo
echo "next:"
echo "  git tag $ARTIFACT/v$VERSION && git push origin $ARTIFACT/v$VERSION"
if [ "$ARTIFACT" = "building-blocks" ]; then
  echo "  set buildingBlocksVersion=$VERSION in gradle.properties and commit"
fi
