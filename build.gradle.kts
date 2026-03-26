import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.taskTree)
}

subprojects {
    group = "io.github.nicolasfara"

    repositories {
        mavenCentral()
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.register("unitTest") {
    group = "verification"
    description = "Runs unit tests for all modules."
    dependsOn(":mktt-core:allTests", ":mktt-client:unitTest")
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests for modules that expose them."
    dependsOn(":mktt-client:jvmIntegrationTest")
}
