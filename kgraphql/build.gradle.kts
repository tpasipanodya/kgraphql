
plugins {
    base
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version "1.7.20"
    id("maven-publish")
    signing
}

val caffeine_version: String by project
val kDataLoader_version: String by project
val deferredJsonBuilder_version: String by project
val kotlin_version: String by project
val serialization_version: String by project
val coroutine_version: String by project
val jackson_version: String by project

val netty_version: String by project
val hamcrest_version: String by project
val kluent_version: String by project
val junit_version: String by project

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version") // JVM dependency

    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeine_version")
    implementation("io.taff:deferred-json-builder:$deferredJsonBuilder_version")

//    api("de.nidomiro:KDataLoader:$kDataLoader_version")


    testImplementation("io.netty:netty-all:$netty_version")
    testImplementation("org.hamcrest:hamcrest:$hamcrest_version")
    testImplementation("org.amshove.kluent:kluent:$kluent_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutine_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutine_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }
    compileTestKotlin {
        kotlinOptions { jvmTarget = JavaVersion.VERSION_11.toString() }
    }

    test {
        useJUnitPlatform()
    }
    dokkaHtml {
        outputDirectory.set(buildDir.resolve("javadoc"))
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(11)
                reportUndocumented.set(true)
                platform.set(org.jetbrains.dokka.Platform.jvm)
            }
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    classifier = "javadoc"
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
                name.set("kgraphql")
                description.set("KGraphQL is a Kotlin implementation of GraphQL. It provides a rich DSL to set up the GraphQL schema.")
                url.set("https://github.com/tpasipanodya/tpasipanodya/kgraphql")

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
                    url.set("https://github.com/tpasipanodya/")
                    tag.set("HEAD")
                }
            }
        }
    }
}
