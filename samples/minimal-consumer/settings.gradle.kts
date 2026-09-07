rootProject.name = "minimal-consumer"

// Resolves dev.domaincentric:* from Maven Central, exactly as a real consumer does.
//   -PwithDcaJava      substitutes the surrounding checkout (composite build) — tests the sources
//   -PfromMavenLocal   adds ~/.m2 first — tests what publishToMavenLocal produced (POM, module
//                      metadata, packaged license); pair it with -PbuildingBlocksVersion / -ParchunitVersion
if (providers.gradleProperty("withDcaJava").isPresent) {
    includeBuild("../..")
}

dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("fromMavenLocal").isPresent) {
            mavenLocal()
        }
        mavenCentral()
    }
}
