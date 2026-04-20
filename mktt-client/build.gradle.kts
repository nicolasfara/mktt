import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }

        compilations.create("integrationTest") {
            associateWith(this@jvm.compilations.getByName("main"))
        }

        testRuns.create("integrationTest") {
            setExecutionSourceFrom(compilations.getByName("integrationTest"))
            executionTask.configure {
                description = "Runs JVM integration tests against a local broker."
                shouldRunAfter(testRuns.getByName("test").executionTask)
                filter {
                    includeTestsMatching("*IntegrationTest")
                    includeTestsMatching("*IT")
                }
            }
        }
    }

    wasmJs {
        browser()
        nodejs()
    }
    js {
        browser()
        nodejs()
    }

    linuxX64()
    linuxArm64()
    mingwX64()
    macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":mktt-core"))
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.io)
            implementation(libs.ktor.network)
            implementation(libs.ktor.network.tls)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        named("jvmIntegrationTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.testcontainers)
                implementation(libs.slf4j.simple)
            }
        }
    }
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs mktt-client integration tests."
    dependsOn("jvmIntegrationTest")
}

tasks.register("unitTest") {
    group = "verification"
    description = "Runs mktt-client unit tests."
    dependsOn("jvmTest")
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

signing {
    if (System.getenv("CI") == "true") {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

publishOnCentral {
    projectLongName.set("MKTT Client")
    projectDescription.set("Coroutine and Flow based MQTT 5 client for MKTT.")
    projectUrl.set("https://github.com/nicolasfara/${rootProject.name}")
    licenseName.set("Apache-2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
    publishing {
        publications {
            withType<MavenPublication> {
                artifactId = "mktt-client"
                scmConnection.set("git:git@github.com:nicolasfara/${rootProject.name}")
                projectUrl.set("https://github.com/nicolasfara/${rootProject.name}")
                pom {
                    developers {
                        developer {
                            name.set("Nicolas Farabegoli")
                            email.set("nicolas.farabegoli@gmail.com")
                            url.set("https://www.nicolasfarabegoli.it/")
                        }
                    }
                }
            }
        }
    }
}
