# Maps an artifact name onto its Gradle project and version property, and resolves the version that
# a build would actually use — gradle.properties, overridden by any -P<property>=<value> argument.
# Accepts both spellings: "building-blocks" / "dca-building-blocks", "archunit" / "dca-archunit".
# Meant to be sourced, not executed.

artifact_project() {
  case "$1" in
    building-blocks|dca-building-blocks)         echo "dca-building-blocks" ;;
    archunit|dca-archunit)                       echo "dca-archunit" ;;
    spring|dca-spring)                           echo "dca-spring" ;;
    archunit-spring-modulith|dca-archunit-spring-modulith)     echo "dca-archunit-spring-modulith" ;;
    *) echo "error: unknown artifact '$1' (building-blocks|archunit|spring|archunit-spring-modulith)" >&2; return 1 ;;
  esac
}

artifact_version_property() {
  case "$(artifact_project "$1")" in
    dca-building-blocks)   echo "buildingBlocksVersion" ;;
    dca-archunit)          echo "archunitVersion" ;;
    dca-spring)            echo "dcaSpringVersion" ;;
    dca-archunit-spring-modulith) echo "archunitSpringModulithVersion" ;;
    *) return 1 ;;
  esac
}

# The in-repo artifact whose released version becomes a POM dependency of the given one, if any.
artifact_dependency() {
  case "$(artifact_project "$1")" in
    dca-archunit|dca-spring) echo "building-blocks" ;;
    dca-archunit-spring-modulith)   echo "archunit" ;;
    *) echo "" ;;
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
