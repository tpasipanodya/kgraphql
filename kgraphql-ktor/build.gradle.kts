import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    base
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("maven-publish")
    signing
}

val caffeine_version: String by project
val kDataLoader_version: String by project
val kotlin_version: String by project
val serialization_version: String by project
val coroutine_version: String by project
val jackson_version: String by project
val ktor_version: String by project

val netty_version: String by project
val hamcrest_version: String by project
val kluent_version: String by project
val junit_version: String by project

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(project(":kgraphql"))
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("org.amshove.kluent:kluent:$kluent_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.ktor:ktor-server-auth:$ktor_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
}


tasks {
    kotlin  { compilerOptions { jvmTarget.set(JvmTarget.JVM_20) } }

    test { useJUnitPlatform() }
    dokkaHtml {
        outputDirectory.set(buildDir.resolve("javadoc"))
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(20)
                reportUndocumented.set(true)
                platform.set(org.jetbrains.dokka.Platform.jvm)
            }
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/tpasipanodya/kgraphql")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)

            pom {
                name.set("KGraphQL")
                description.set("KGraphQL is a Kotlin implementation of GraphQL. It provides a rich DSL to set up the GraphQL schema.")
                url.set("https://github.com/tpasipanodya/kgraphql")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/tpasipanodya/kgraphql/blob/main/LICENSE.md")
                    }
                }

                developers {
                    developer {
                        id.set("tpasipanodya")
                        name.set("Tafadzwa Pasipanodya")
                        email.set("tmpasipanodya@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/tpasipanodya/kgraphql.git")
                    developerConnection.set("scm:git:https://github.com/tpasipanodya/kgraphql.git")
                    url.set("https://github.com/tpasipanodya/kgraphql")
                    tag.set("HEAD")
                }
            }
        }
    }
}

