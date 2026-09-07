# Maps an artifact name onto its Gradle project and version property, and resolves the version that
# a build would actually use — gradle.properties, overridden by any -P<property>=<value> argument.
# Accepts both spellings: "building-blocks" / "dca-building-blocks", "archunit" / "dca-archunit".
# Meant to be sourced, not executed.

artifact_project() {
  case "$1" in
    building-blocks|dca-building-blocks) echo "dca-building-blocks" ;;
    archunit|dca-archunit)               echo "dca-archunit" ;;
    *) echo "error: unknown artifact '$1' (building-blocks|archunit)" >&2; return 1 ;;
  esac
}

artifact_version_property() {
  case "$(artifact_project "$1")" in
    dca-building-blocks) echo "buildingBlocksVersion" ;;
    dca-archunit)        echo "archunitVersion" ;;
    *) return 1 ;;
  esac
}

# artifact_version <artifact> [gradle -P arguments...]
# The effective version: the last matching -P<property>=<value> argument wins over gradle.properties.
artifact_version() {
  local property version arg
  property="$(artifact_version_property "$1")" || return 1
  shift
  version="$(sed -n "s/^${property}=//p" gradle.properties)"
  for arg in "$@"; do
    case "$arg" in
      "-P${property}="*) version="${arg#-P${property}=}" ;;
    esac
  done
  echo "$version"
}

is_snapshot_version() {
  case "$1" in
    *-SNAPSHOT) return 0 ;;
    *) return 1 ;;
  esac
}
