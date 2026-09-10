plugins { java }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.1.2")
    implementation("org.springframework:spring-tx:7.0.3")
    testImplementation("dev.domaincentric:dca-archunit:0.4.0")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform(); testLogging.showStandardStreams = true }
