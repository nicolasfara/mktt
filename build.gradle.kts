import org.danilopianini.gradle.mavencentral.DocStyle
import org.danilopianini.gradle.mavencentral.JavadocJar
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

group = "it.nicolasfarabegoli"

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "it.nicolasfarabegoli"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    androidTarget {
        publishAllLibraryVariants()
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting { }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.testing.common)
                implementation(libs.bundles.kotest.common)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }

    wasmJs {
//        browser()
        nodejs()
        binaries.library()
    }

    js(IR) {
//        browser()
        nodejs()
        binaries.library()
    }

    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        binaries {
            sharedLib()
            staticLib()
        }
    }

    applyDefaultHierarchyTemplate()
    /*
     * Linux 64
     */
    linuxX64(nativeSetup)
    linuxArm64(nativeSetup)
    /*
     * Win 64
     */
    mingwX64(nativeSetup)
    /*
     * Apple OSs
     */
    macosX64(nativeSetup)
    macosArm64(nativeSetup)
    iosArm64(nativeSetup)
    iosSimulatorArm64(nativeSetup)
    watchosArm32(nativeSetup)
    watchosArm64(nativeSetup)
    watchosSimulatorArm64(nativeSetup)
    tvosArm64(nativeSetup)
    tvosSimulatorArm64(nativeSetup)

    compilerOptions {
        allWarningsAsErrors = true
        apiVersion = KOTLIN_2_0
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    val os = OperatingSystem.current()
    val excludeTargets = when {
        os.isLinux -> kotlin.targets.filterNot { "linux" in it.name }
        os.isWindows -> kotlin.targets.filterNot { "mingw" in it.name }
        os.isMacOsX -> kotlin.targets.filter { "linux" in it.name || "mingw" in it.name }
        else -> emptyList()
    }.mapNotNull { it as? KotlinNativeTarget }

    configure(excludeTargets) {
        compilations.configureEach {
            cinterops.configureEach { tasks[interopProcessingTaskName].enabled = false }
            compileTaskProvider.get().enabled = false
            tasks[processResourcesTaskName].enabled = false
        }
        binaries.configureEach { linkTask.enabled = false }

        mavenPublication {
            tasks.withType<AbstractPublishToMaven>().configureEach {
                onlyIf { publication != this@mavenPublication }
            }
            tasks.withType<GenerateModuleMetadata>().configureEach {
                onlyIf { publication.get() != this@mavenPublication }
            }
        }
    }
}

tasks.dokkaJavadoc {
    enabled = false
}

tasks.withType<JavadocJar>().configureEach {
    val dokka = tasks.dokkaHtml.get()
    dependsOn(dokka)
    from(dokka.outputDirectory)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
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
