val version: String by project
val sonatypeUsername: String? = System.getenv("sonatypeUsername")
val sonatypePassword: String? = System.getenv("sonatypePassword")

plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
    jacoco
}

allprojects {
    repositories {
        maven {
            name = "deferred-json-builder"
            url = uri("https://maven.pkg.github.com/tpasipanodya/deferred-json-builder")
            credentials {
                username = System.getenv("PACKAGE_STORE_USERNAME")
                password = System.getenv("PACKAGE_STORE_TOKEN")
            }
        }
        mavenCentral()
    }
}

subprojects {
    group = "io.taff"
    version = "$version${if(isReleaseBuild()) "" else "-SNAPSHOT"}"

}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

fun isReleaseBuild() = System.getenv("IS_RELEASE_BUILD")?.toBoolean() == true
