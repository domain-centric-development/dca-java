description = "Domain-Centric Architecture building blocks: DDD tactical and strategic markers, hexagonal port interfaces. Zero dependencies."

version = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.1.0-SNAPSHOT")

extensions.configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        artifactId = "dca-building-blocks"
        pom {
            name.set("DCA Building Blocks")
            description.set(project.description)
        }
    }
}
