val version: String by project
val sonatypeUsername: String? = System.getenv("sonatypeUsername")
val sonatypePassword: String? = System.getenv("sonatypePassword")

plugins {
    id("com.github.ben-manes.versions") version "0.44.0"
    jacoco
}

allprojects {
    repositories {
        maven {
            name = "kdataloader"
            url = uri("https://maven.pkg.github.com/tpasipanodya/kdataloader")
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
    version = version

}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
