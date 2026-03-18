pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
        id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    }
}

val PROJECT_NAME = "bluetape4k"

rootProject.name = "$PROJECT_NAME-projects"

includeModules("bluetape4k", true, false)

include("bluetape4k-aws")
project(":bluetape4k-aws").projectDir = file("aws")
include("bluetape4k-aws-kotlin")
project(":bluetape4k-aws-kotlin").projectDir = file("aws-kotlin")

includeModules("data", withBaseDir = false)
includeModules("infra", withBaseDir = false)
includeModules("io", withBaseDir = false)
// includeModules("javers", withBaseDir = true)
includeModules("spring-boot3", withBaseDir = true)
includeModules("spring-boot4", withBaseDir = true)
// includeModules("tokenizer", withBaseDir = true)
includeModules("testing", withBaseDir = false)
// includeModules("timefold", withBaseDir = true)
includeModules("utils", withBaseDir = false)

include("bluetape4k-vertx")
project(":bluetape4k-vertx").projectDir = file("vertx")

includeModules("virtualthread", withProjectName = true, withBaseDir = true)

// Examples (library style examples)
includeModules("examples", withProjectName = true, withBaseDir = true)

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
