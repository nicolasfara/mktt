import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

val Provider<PluginDependency>.id: String get() = get().pluginId

allprojects {
    group = "it.nicolasfarabegoli"

    repositories {
        mavenCentral()
    }

    with(rootProject.libs.plugins) {
        apply(plugin = kotlin.multiplatform.id)
        apply(plugin = kotlin.qa.id)
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    js {
        fun KotlinJsSubTargetDsl.configureTimeout(timeout: String) {
            testTask {
                useMocha {
                    this.timeout = timeout
                }
            }
        }
        nodejs {
            configureTimeout("1m")
        }
        browser {
            configureTimeout("1m")
        }
        binaries.library()
    }
    //    wasmJs {
    //        browser()
    //        nodejs()
    //        binaries.library()
    //    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.arrow)
            implementation(libs.arrow.coroutines)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.rx2)
            implementation(libs.hive.mqtt)
        }
        jsMain.dependencies {
            implementation(npm("mqtt", "5.13.3"))
        }
    }

//
//    val nativeSetup: KotlinNativeTarget.() -> Unit = {
//        binaries {
//            sharedLib()
//            staticLib()
//        }
//    }
//
//    applyDefaultHierarchyTemplate()
//    /*
//     * Linux 64
//     */
//    linuxX64(nativeSetup)
//    linuxArm64(nativeSetup)
//    /*
//     * Win 64
//     */
//    mingwX64(nativeSetup)
//    /*
//     * Apple OSs
//     */
//    macosX64(nativeSetup)
//    macosArm64(nativeSetup)
//    iosArm64(nativeSetup)
//    iosSimulatorArm64(nativeSetup)
//    watchosArm32(nativeSetup)
//    watchosArm64(nativeSetup)
//    watchosSimulatorArm64(nativeSetup)
//    tvosArm64(nativeSetup)
//    tvosSimulatorArm64(nativeSetup)
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
    projectLongName.set("MKTT")
    projectDescription.set("A Kotlin multiplatform MQTT client library.")
    projectUrl.set("https://github.com/nicolasfara/${rootProject.name}")
    licenseName.set("Apache-2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
    publishing {
        publications {
            withType<MavenPublication> {
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
