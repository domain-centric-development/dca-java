import com.vanniktech.maven.publish.MavenPublishBaseExtension

description = "Spring implementations of the dca-building-blocks runtime contracts: DomainEventPublisher over ApplicationEventPublisher, TransactionBoundary over TransactionTemplate, plus an in-memory boundary and a Spring Boot auto-configuration."

version = providers.gradleProperty("dcaSpringVersion").getOrElse("0.1.0-SNAPSHOT")

dependencies {
    api(project(":dca-building-blocks"))
    // The consumer's Spring Boot BOM pins the framework version; this artifact must not fight it.
    compileOnly(libs.spring.context)
    compileOnly(libs.spring.tx)
    compileOnly(libs.spring.boot.autoconfigure)

    testImplementation(libs.spring.context)
    testImplementation(libs.spring.tx)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.assertj.core) // ApplicationContextRunner needs it
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extensions.configure<MavenPublishBaseExtension> {
    coordinates("dev.domaincentric", "dca-spring", version.toString())
    pom {
        name.set("DCA Spring Adapters")
        description.set(project.description)
    }
}

// A released dca-spring must not carry a snapshot of dca-building-blocks in its POM.
val buildingBlocksVersion = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.1.0-SNAPSHOT")
val dcaSpringVersion = version.toString()
tasks.matching { it.name.contains("MavenCentral") }
    .configureEach {
        doFirst {
            check(dcaSpringVersion.endsWith("-SNAPSHOT") || !buildingBlocksVersion.endsWith("-SNAPSHOT")) {
                "dca-spring $dcaSpringVersion would depend on dca-building-blocks $buildingBlocksVersion. " +
                    "Set buildingBlocksVersion in gradle.properties to the released version first."
            }
        }
    }
