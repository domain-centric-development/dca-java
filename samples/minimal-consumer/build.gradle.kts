plugins { java }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

// The released versions a consumer would pin. CI overrides them to test a fresh local publication.
val buildingBlocksVersion = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.1.1")
val archunitVersion = providers.gradleProperty("archunitVersion").getOrElse("0.2.0")

dependencies {
    implementation("dev.domaincentric:dca-building-blocks:$buildingBlocksVersion")

    testImplementation("dev.domaincentric:dca-archunit:$archunitVersion")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
