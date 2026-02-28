import io.bluetape4k.gradle.applyBluetape4kPomMetadata
import io.bluetape4k.gradle.centralSnapshotsRepository
import io.bluetape4k.gradle.configurePublishingSigning

plugins {
    `java-platform`
    `maven-publish`
    signing
}

dependencies {
    constraints {
        rootProject.subprojects {
            if (name != "bluetape4k-bom") {
                api(this)
            }
        }
    }
}

publishing {
    publications {
        register("Bluetape4k", MavenPublication::class) {
            from(components["javaPlatform"])
            pom {
                applyBluetape4kPomMetadata(
                    artifactDisplayName = "bluetape4k-bom",
                    artifactDescription = "BOM for Bluetape4k modules",
                )
            }
        }
    }
    repositories {
        centralSnapshotsRepository(project)
        mavenLocal()
    }
}

configurePublishingSigning(publicationName = "Bluetape4k")
