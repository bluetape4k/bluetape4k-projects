pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
        id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
    }
}

val PROJECT_NAME = "bluetape4k"

rootProject.name = "$PROJECT_NAME-projects"

includeModules("bluetape4k", true, false)

includeModules("aws", withBaseDir = true)
includeModules("aws-kotlin", withBaseDir = true)
includeModules("data", withBaseDir = false)
includeModules("infra", withBaseDir = false)
includeModules("io", withBaseDir = false)
includeModules("javers", withBaseDir = true)
includeModules("quarkus", withBaseDir = true)
includeModules("spring", withBaseDir = true)
includeModules("tokenizer", withBaseDir = true)
includeModules("testing", withBaseDir = false)
includeModules("utils", withBaseDir = false)
includeModules("vertx", withBaseDir = true)

// Examples (library style examples)
includeModules("examples", withProjectName = true, withBaseDir = true)

// Workshop (application style examples) --> moved to bluetape4k-workshop
//
// includeModules("workshop/docker", false, false)
// includeModules("workshop/kafka", false, false)
//
// includeModules("workshop/ratelimiter", false, false)
includeModules("workshop/quarkus", false, false)
// includeModules("workshop/spring-boot", false, false)
// includeModules("workshop/spring-cloud", false, false)
// includeModules("workshop/spring-data", false, false)
// includeModules("workshop/spring-security", false, false)
// includeModules("workshop/vertx", false, false)

fun includeModules(baseDir: String, withProjectName: Boolean = true, withBaseDir: Boolean = true) {
    files("$rootDir/$baseDir").files
        .filter { it.isDirectory }
        .forEach { moduleDir ->
            moduleDir.listFiles()
                ?.filter { it.isDirectory }
                ?.forEach { dir ->
                    val basePath = baseDir.replace("/", "-")
                    val projectName = when {
                        !withProjectName && !withBaseDir -> dir.name
                        withProjectName && !withBaseDir  -> PROJECT_NAME + "-" + dir.name
                        withProjectName                  -> PROJECT_NAME + "-" + basePath + "-" + dir.name
                        else                             -> basePath + "-" + dir.name
                    }
                    // println("include modules: $projectName")

                    include(projectName)
                    project(":$projectName").projectDir = dir
                }
        }
}
