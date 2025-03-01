import org.danilopianini.gradle.mavencentral.DocStyle
import org.danilopianini.gradle.mavencentral.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
//    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
//    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

group = "it.nicolasfarabegoli"

repositories {
    mavenCentral()
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
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.rx2)
            implementation(libs.hive.mqtt)
        }
        jsMain.dependencies {
            implementation(npm("mqtt", "5.10.3"))
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

tasks.dokkaJavadoc {
    enabled = false
}

tasks.withType<JavadocJar>().configureEach {
    val dokka = tasks.dokkaHtml.get()
    dependsOn(dokka)
    from(dokka.outputDirectory)
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
    licenseName.set("MIT License")
    licenseUrl.set("https://opensource.org/license/mit/")
    docStyle.set(DocStyle.HTML)
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

publishing {
    publications {
        publications.withType<MavenPublication>().configureEach {
            if ("OSSRH" !in name) {
                artifact(tasks.javadocJar)
            }
        }
    }
}
