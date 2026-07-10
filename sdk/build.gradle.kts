plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
}

description = "PlaceToPay Checkout SDK for Java"

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.wiremock)
    testImplementation(libs.slf4j.simple)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "PlaceToPay Checkout SDK",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "PlaceToPay",
            "Automatic-Module-Name" to "com.placetopay.checkout"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Publishing (Maven Central)
// ─────────────────────────────────────────────────────────────────────────────

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "checkout-sdk"

            pom {
                name.set("PlaceToPay Checkout SDK")
                description.set("Official Java SDK for the PlaceToPay Checkout API.")
                url.set("https://github.com/placetopay/checkout-java")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("placetopay")
                        name.set("PlaceToPay Checkout SDK Team")
                        email.set("developers@placetopay.com")
                        organization.set("PlaceToPay")
                        organizationUrl.set("https://www.placetopay.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/placetopay/checkout-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/placetopay/checkout-java.git")
                    url.set("https://github.com/placetopay/checkout-java")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/placetopay/checkout-java/issues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if ((version as String).endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
            credentials {
                username = (findProperty("ossrhUsername") as String?) ?: System.getenv("OSSRH_USERNAME")
                password = (findProperty("ossrhPassword") as String?) ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey: String? = (findProperty("signingKey") as String?) ?: System.getenv("SIGNING_KEY")
    val signingPassword: String? = (findProperty("signingPassword") as String?) ?: System.getenv("SIGNING_PASSWORD")
    isRequired = signingKey != null && !(version as String).endsWith("SNAPSHOT")
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        // Integration tests talk to a real sandbox; opt-in via `-PrunIntegration=true`.
        if (!project.hasProperty("runIntegration")) {
            excludeTags("integration")
        }
    }
    systemProperty("java.util.logging.SimpleFormatter.format", "[%1\$tT] [%4\$s] %5\$s%6\$s%n")
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
