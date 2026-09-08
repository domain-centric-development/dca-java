import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.mavenPublish) apply false
}

// dca-building-blocks and dca-archunit are framework-free by contract: the rules forbid Spring in
// the domain, and these two are what the rules are written against. dca-spring and
// dca-archunit-modulith exist precisely so that Spring never has to appear here.
val frameworkFree = setOf("dca-building-blocks", "dca-archunit")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "com.diffplug.spotless")

    group = "dev.domaincentric"

    // Build and test on 21 by default; -PjavaToolchain=17 runs the same build on the oldest
    // supported runtime, which is how the "Java 17+" claim is verified (CI runs both).
    val javaToolchain = providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(21)
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaToolchain))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all,-processing")
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    if (name in frameworkFree) {
        val verifyFrameworkFree = tasks.register("verifyFrameworkFree") {
            group = "verification"
            description = "Fails if Spring appears on the compile or runtime class path of $name"
            val compile = configurations.named("compileClasspath")
            val runtime = configurations.named("runtimeClasspath")
            doLast {
                val offenders = (compile.get().resolvedConfiguration.resolvedArtifacts +
                    runtime.get().resolvedConfiguration.resolvedArtifacts)
                    .map { it.moduleVersion.id }
                    .filter { it.group.startsWith("org.springframework") }
                    .map { "${it.group}:${it.name}:${it.version}" }
                    .toSortedSet()
                check(offenders.isEmpty()) { "$name must stay framework-free, found: $offenders" }
            }
        }
        tasks.named("check") { dependsOn(verifyFrameworkFree) }
    }

    // Every published jar carries the license it is released under.
    tasks.withType<Jar>().configureEach {
        from(rootProject.layout.projectDirectory.file("LICENSE")) { into("META-INF") }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            target("src/**/*.java")
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        // Sources jar, javadoc jar, checksums and the Central Portal upload come from the plugin.
        // The deployment is uploaded and validated but not released: it waits for "Publish" on
        // https://central.sonatype.com/publishing/deployments, so a bad release can still be dropped.
        publishToMavenCentral()

        // Signing only when a key is configured, so publishToMavenLocal and the composite build
        // of the sample work without GPG.
        if (providers.gradleProperty("signingInMemoryKey").isPresent ||
            providers.gradleProperty("signing.keyId").isPresent
        ) {
            signAllPublications()
        }

        pom {
            // name and description are set per artifact; the rest is identical for both.
            inceptionYear.set("2026")
            url.set("https://domaincentric.dev")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("chbloemer")
                    name.set("Christoph Bloemer")
                    url.set("https://github.com/chbloemer")
                }
            }
            scm {
                url.set("https://github.com/domain-centric-development/dca-java")
                connection.set("scm:git:https://github.com/domain-centric-development/dca-java.git")
                developerConnection.set("scm:git:ssh://git@github.com/domain-centric-development/dca-java.git")
            }
            issueManagement {
                system.set("GitHub Issues")
                url.set("https://github.com/domain-centric-development/dca-java/issues")
            }
        }
    }
}
