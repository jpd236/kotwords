import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("org.jetbrains.dokka") version "2.1.0"
    kotlin("multiplatform") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
}

group = "com.jeffpdavidson.kotwords"
version = "1.5.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)  // for compilerOptions
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    js(IR) {
        browser {}
        binaries.executable()
    }

    mingwX64()
    linuxX64()
    macosX64()

    targets.all {
        if (this is KotlinNativeTarget) {
            binaries.executable {
                entryPoint = "com.jeffpdavidson.kotwords.cli.main"
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.js.ExperimentalJsExport")
                optIn("com.jeffpdavidson.kotwords.KotwordsInternal")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.16.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
                implementation("io.github.pdvrieze.xmlutil:serialization:0.91.3")
                implementation("com.github.ajalt.colormath:colormath:3.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("com.soywiz:korlibs-image-core:6.0.0")

                // TODO: Migrate to kotlinx-datetime when it can be done without breaking ksoup.
                // Ensure any size hit to the JS bundle is acceptable.
                implementation("com.soywiz:korlibs-time:6.0.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation("io.github.pdvrieze.xmlutil:testutil:0.91.3")
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jsoup:jsoup:1.22.1")
            }
        }

        val jvmTest by getting {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(npm("jszip", "3.10.1"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.12.0")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-pako:2026.3.5-2.1.0")
            }
        }

        val jsTest by getting {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val nativeMain by getting {
            dependencies {
                implementation("com.soywiz:korlibs-io:6.0.0")
                implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
                implementation("net.thauvin.erik.urlencoder:urlencoder-lib:1.6.0")
                implementation("com.github.ajalt.clikt:clikt:5.1.0")
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<Test> {
    maxHeapSize = "2G"
    useJUnitPlatform()
}

dokka {
    // Omit .web and .cli package from documentation
    dokkaSourceSets {
        configureEach {
            perPackageOption {
                matchingRegex.set("""com.jeffpdavidson\.kotwords\.(?:web|cli).*""")
                suppress.set(true)
            }
        }
    }
}

tasks {
    val browserProductionWebpackTask = getByName("jsBrowserProductionWebpack", KotlinWebpack::class)

    assemble {
        dependsOn(browserProductionWebpackTask)
    }
}

mavenPublishing {
    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
        sourcesJar = SourcesJar.Sources(),
    ))

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Kotwords")
        description.set("Collection of crossword puzzle file format converters and other utilities, written in Kotlin.")
        url.set("https://jpd236.github.io/kotwords")
        developers {
            developer {
                id.set("jpd236")
                name.set("Jeff Davidson")
            }
        }
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/jpd236/kotwords.git")
            developerConnection.set("scm:git:ssh://git@github.com/jpd236/kotwords.git")
            url.set("https://github.com/jpd236/kotwords")
        }
    }
}
