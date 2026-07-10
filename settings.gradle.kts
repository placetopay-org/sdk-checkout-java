rootProject.name = "checkout-sdk"

include("sdk", "examples")

// Gradle auto-loads `gradle/libs.versions.toml` as the `libs` version catalog,
// so no explicit `versionCatalogs { create("libs") { from(...) } }` is needed
// (doing that explicitly is a hard error on Gradle 9).
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
