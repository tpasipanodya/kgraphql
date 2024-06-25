import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    base
    application
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("maven-publish")
    signing
}

val ktor_version: String by project
val logback_version: String by project
val exposed_version: String by project
val h2_version: String by project
val hikari_version: String by project

val junit_version: String by project

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(":kgraphql-ktor"))
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")
}


tasks {
    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_20) } }
    test { useJUnitPlatform() }
    dokkaHtml {
        outputDirectory.set(layout.buildDirectory.dir("javadoc"))
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
                        name.set("Tafadzea Pasipanodya")
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

