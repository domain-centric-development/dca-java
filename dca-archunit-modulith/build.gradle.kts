import com.vanniktech.maven.publish.MavenPublishBaseExtension

description = "Spring Modulith module verification for Domain-Centric Architecture projects: a JUnit base class next to DcaArchitectureTest, with the test-class exclusion Modulith needs."

version = providers.gradleProperty("archunitModulithVersion").getOrElse("0.1.0-SNAPSHOT")

// Modulith imports with ArchUnit's DoNotIncludeTests, so a fixture below src/test would be
// invisible to it. The fixture application therefore compiles into its own source set.
val fixtures: SourceSet by sourceSets.creating

dependencies {
    api(project(":dca-archunit"))
    // A Modulith project already has spring-modulith-core on its test class path; the consumer's
    // BOM pins the version. Minimum supported: 2.0.
    compileOnly(libs.spring.modulith.core)
    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter)

    testImplementation(libs.spring.modulith.core)
    testImplementation(libs.spring.boot) // Modulith's module detection reads Boot's config data
    testImplementation(fixtures.output)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extensions.configure<MavenPublishBaseExtension> {
    coordinates("dev.domaincentric", "dca-archunit-modulith", version.toString())
    pom {
        name.set("DCA ArchUnit Rules — Spring Modulith")
        description.set(project.description)
    }
}

// A released dca-archunit-modulith must not carry a snapshot of dca-archunit in its POM.
val archunitVersion = providers.gradleProperty("archunitVersion").getOrElse("0.1.0-SNAPSHOT")
val archunitModulithVersion = version.toString()
tasks.matching { it.name.contains("MavenCentral") }
    .configureEach {
        doFirst {
            check(archunitModulithVersion.endsWith("-SNAPSHOT") || !archunitVersion.endsWith("-SNAPSHOT")) {
                "dca-archunit-modulith $archunitModulithVersion would depend on dca-archunit $archunitVersion. " +
                    "Set archunitVersion in gradle.properties to the released version first."
            }
        }
    }
