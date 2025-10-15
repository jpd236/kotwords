import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    id("com.vanniktech.maven.publish") version "0.33.0"
    id("org.jetbrains.dokka") version "1.9.10"
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.jeffpdavidson.kotwords"
version = "1.4.11"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvm {
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
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
                implementation("com.squareup.okio:okio:3.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
                implementation("io.github.pdvrieze.xmlutil:serialization:0.86.3")
                implementation("com.github.ajalt.colormath:colormath:3.3.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // TODO: Migrate to kotlinx-datetime when it can be done without breaking ksoup.
                // Ensure any size hit to the JS bundle is acceptable.
                implementation("com.soywiz.korlibs.klock:klock:4.0.10")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
                implementation("io.github.pdvrieze.xmlutil:testutil:0.86.3")
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.apache.pdfbox:pdfbox:3.0.1")
                implementation("org.jsoup:jsoup:1.17.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(npm("jszip", "3.10.1"))
                implementation(npm("pdf-lib", "1.17.1"))
                implementation(npm("@pdf-lib/fontkit", "1.1.1"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.10.1")
            }
        }

        val jsTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
                // TODO: Find out how to use newer versions - 4.x seems to use ES6 modules which are not handled
                // smoothly. Note also that PdfJs.kt and ImageComparator.kt will need updates.
                implementation(npm("pdfjs-dist", "3.11.174"))
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val nativeMain by getting {
            dependencies {
                implementation("com.soywiz.korlibs.korio:korio:4.0.10")
                implementation("com.fleeksoft.ksoup:ksoup:0.1.2")
                implementation("net.thauvin.erik.urlencoder:urlencoder-lib:1.4.0")
                implementation("com.github.ajalt.clikt:clikt:4.2.2")
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<Test> {
    maxHeapSize = "2G"
}

tasks {
    // Omit .web and .cli package from documentation
    dokkaHtml.configure {
        dokkaSourceSets {
            configureEach {
                perPackageOption {
                    matchingRegex.set("""com.jeffpdavidson\.kotwords\.(?:web|cli).*""")
                    suppress.set(true)
                }
            }
        }
    }

    val browserProductionWebpackTask = getByName("jsBrowserProductionWebpack", KotlinWebpack::class)

    assemble {
        dependsOn(browserProductionWebpackTask)
    }
}

mavenPublishing {
    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true,
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
