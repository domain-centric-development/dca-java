plugins { java }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

// The versions a consumer pins - kept equal to the README's quick start, and bumped with every
// release (see RELEASING.md). CI overrides them: once through the composite build, once against a
// fresh local publication. While the version below is not on Maven Central yet, run this sample with
// -PwithDcaJava, which substitutes the surrounding checkout.
val buildingBlocksVersion = providers.gradleProperty("buildingBlocksVersion").getOrElse("0.3.1")
val archunitVersion = providers.gradleProperty("archunitVersion").getOrElse("0.6.0")

dependencies {
    implementation("dev.domaincentric:dca-building-blocks:$buildingBlocksVersion")

    testImplementation("dev.domaincentric:dca-archunit:$archunitVersion")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
