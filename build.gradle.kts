// Root build script — holds shared conventions for all subprojects.
//
// Individual modules (sdk/, examples/) own their own plugin activations and
// dependency declarations; this root only sets the Java toolchain and the
// repositories contract.

plugins {
    java
    // Spotless enforces google-java-format on every Java source file. Mirrors
    // ruff's role in the Python SDK: catches style drift in CI rather than in
    // code review. Run `./gradlew spotlessApply` to auto-fix locally.
    id("com.diffplug.spotless") version "7.0.2"
}

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.22.0").aosp()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-parameters"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}
