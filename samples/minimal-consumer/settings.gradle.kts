rootProject.name = "minimal-consumer"

// Local development: substitute the published coordinates with the sibling build.
// Drop this line once the artifacts are on Maven Central.
includeBuild("../..")

dependencyResolutionManagement {
    repositories { mavenCentral() }
}
