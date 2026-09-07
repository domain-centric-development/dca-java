import com.vanniktech.maven.publish.MavenPublishBaseExtension

description = "Domain-Centric Architecture building blocks: DDD tactical and strategic markers, hexagonal port interfaces. Zero dependencies."

version = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.1.0-SNAPSHOT")

dependencies {
    // Test scope only — the published artifact stays dependency-free.
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extensions.configure<MavenPublishBaseExtension> {
    coordinates("dev.domaincentric", "dca-building-blocks", version.toString())
    pom {
        name.set("DCA Building Blocks")
        description.set(project.description)
    }
}
