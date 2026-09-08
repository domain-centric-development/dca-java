rootProject.name = "dca-java"

include("dca-building-blocks", "dca-archunit", "dca-spring", "dca-archunit-spring-modulith")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
