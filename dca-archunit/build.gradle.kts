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

extensions.configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        artifactId = "dca-archunit"
        pom {
            name.set("DCA ArchUnit Rules")
            description.set(project.description)
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
