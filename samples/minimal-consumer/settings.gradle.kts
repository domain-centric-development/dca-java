rootProject.name = "minimal-consumer"

// Resolves dev.domaincentric:* from Maven Central, exactly as a real consumer does. To test
// unreleased changes of the surrounding build instead, run with -PwithDcaJava.
if (providers.gradleProperty("withDcaJava").isPresent) {
    includeBuild("../..")
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
}
