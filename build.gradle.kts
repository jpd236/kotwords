import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("org.jetbrains.dokka") version "1.9.10"
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

group = "com.jeffpdavidson.kotwords"
version = "1.3.10-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
    }

    jvmToolchain(8)

    js(IR) {
        browser {}
        binaries.executable()
    }

    @Suppress("UNUSED_VARIABLE") // https://youtrack.jetbrains.com/issue/KT-38871
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
                implementation("com.squareup.okio:okio:3.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
                // TODO: Update once https://github.com/pdvrieze/xmlutil/discussions/186 is resolved.
                implementation("io.github.pdvrieze.xmlutil:serialization:0.86.0")
                implementation("com.github.ajalt.colormath:colormath:3.3.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // TODO: Migrate to kotlinx-datetime if parsing/formatting support is added.
                implementation("com.soywiz.korlibs.klock:klock:4.0.10")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.apache.pdfbox:pdfbox:3.0.0")
                implementation("org.jsoup:jsoup:1.16.2")
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
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
            }
        }

        val jsTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
                implementation(npm("pdfjs-dist", "3.11.174"))
            }

            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }
}

tasks {
    // Omit .web package from documentation
    dokkaHtml.configure {
        dokkaSourceSets {
            configureEach {
                perPackageOption {
                    matchingRegex.set("""com.jeffpdavidson\.kotwords\.web.*""")
                    suppress.set(true)
                }
            }
        }
    }

    val browserProductionWebpackTask = getByName("jsBrowserProductionWebpack", KotlinWebpack::class)

    @Suppress("UNUSED_VARIABLE") // https://youtrack.jetbrains.com/issue/KT-38871
    val browserDistributionZip by creating(Zip::class) {
        dependsOn(browserProductionWebpackTask)
        from (browserProductionWebpackTask.outputDirectory)
        destinationDirectory.set(layout.buildDirectory.dir("zip").get().getAsFile())
        archiveAppendix.set("browser-distribution")
    }

    assemble {
        dependsOn(browserDistributionZip)
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(dokkaJar)
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
}

if (System.getenv("PGP_KEY_ID") != null) {
    signing {
        useInMemoryPgpKeys(System.getenv("PGP_KEY_ID"), System.getenv("PGP_KEY"), System.getenv("PGP_PASSPHRASE"))
        sign(publishing.publications)
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_DEPLOY_USERNAME"))
            password.set(System.getenv("OSSRH_DEPLOY_PASSWORD"))
        }
    }
}

// TODO: Remove workaround once https://github.com/gradle/gradle/issues/26091 is resolved.
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}
