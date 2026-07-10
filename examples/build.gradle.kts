plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

description = "Spring Boot demo app for the PlaceToPay Checkout SDK"

// Not published to Maven Central — local test harness only.
tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }

dependencies {
    implementation(project(":sdk"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
