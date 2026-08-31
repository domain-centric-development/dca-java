import com.vanniktech.maven.publish.MavenPublishBaseExtension

description = "Domain-Centric Architecture governance rules for ArchUnit, pinned to the dca-building-blocks markers."

version = providers.gradleProperty("archunitVersion").getOrElse("0.1.0-SNAPSHOT")

dependencies {
    api(project(":dca-building-blocks"))
    api(libs.archunit)
    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extensions.configure<MavenPublishBaseExtension> {
    coordinates("dev.domaincentric", "dca-archunit", version.toString())
    pom {
        name.set("DCA ArchUnit Rules")
        description.set(project.description)
    }
}

// A released dca-archunit must not carry a snapshot of dca-building-blocks in its POM. Its version
// comes from the buildingBlocksVersion property, which the release workflow only sets for its own tag.
val buildingBlocksVersion = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.1.0-SNAPSHOT")
val archunitVersion = version.toString()
tasks.matching { it.name.contains("MavenCentral") }
    .configureEach {
        doFirst {
            check(archunitVersion.endsWith("-SNAPSHOT") || !buildingBlocksVersion.endsWith("-SNAPSHOT")) {
                "dca-archunit $archunitVersion would depend on dca-building-blocks $buildingBlocksVersion. " +
                    "Set buildingBlocksVersion in gradle.properties to the released version first."
            }
        }
    }

// Renders RULES.md + rules.json (consumed by the DCA knowledge catalog) into the repo root.
tasks.register<JavaExec>("rulesCatalog") {
    group = "documentation"
    description = "Writes RULES.md and rules.json from the rule catalog"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.domaincentric.dca.archunit.catalog.RuleCatalog")
    args(rootProject.layout.projectDirectory.asFile.absolutePath)
}
