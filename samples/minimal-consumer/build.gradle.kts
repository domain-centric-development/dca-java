plugins { java }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.1.0-SNAPSHOT")

    testImplementation("dev.domaincentric:dca-archunit:0.1.0-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
