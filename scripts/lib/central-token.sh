# Resolves the Central Portal user token into ORG_GRADLE_PROJECT_mavenCentral{Username,Password}.
# Sources, in order: the environment, the macOS keychain item "dca-maven-central" (content
# "username:password"), an interactive prompt. Meant to be sourced, not executed.

KEYCHAIN_ITEM="${DCA_CENTRAL_KEYCHAIN_ITEM:-dca-maven-central}"

if [ -n "${ORG_GRADLE_PROJECT_mavenCentralUsername:-}" ] && [ -n "${ORG_GRADLE_PROJECT_mavenCentralPassword:-}" ]; then
  echo "using the Central token from the environment"
elif _dca_token="$(security find-generic-password -s "$KEYCHAIN_ITEM" -w 2>/dev/null)"; then
  # The token halves contain no colon.
  export ORG_GRADLE_PROJECT_mavenCentralUsername="${_dca_token%%:*}"
  export ORG_GRADLE_PROJECT_mavenCentralPassword="${_dca_token#*:}"
  unset _dca_token
  echo "using the Central token from the keychain item $KEYCHAIN_ITEM"
else
  echo "no Central token in the environment or keychain item $KEYCHAIN_ITEM"
  read -r -p "Central Portal token username: " _dca_user
  read -r -s -p "Central Portal token password: " _dca_pass
  echo
  export ORG_GRADLE_PROJECT_mavenCentralUsername="$_dca_user"
  export ORG_GRADLE_PROJECT_mavenCentralPassword="$_dca_pass"
  unset _dca_user _dca_pass
fi

[ -n "$ORG_GRADLE_PROJECT_mavenCentralUsername" ] || { echo "error: no Central token username" >&2; exit 1; }
[ -n "$ORG_GRADLE_PROJECT_mavenCentralPassword" ] || { echo "error: no Central token password" >&2; exit 1; }
