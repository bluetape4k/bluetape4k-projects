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

val projectName = "bluetape4k"

rootProject.name = "$projectName-projects"

includeModules("bluetape4k", true, false)

includeModules("cache", withBaseDir = false)
includeModules("data", withBaseDir = false)
includeModules("infra", withBaseDir = false)
includeModules("io", withBaseDir = false)
includeModules("spring-boot", withBaseDir = true)
includeModules("testing", withBaseDir = false)
includeModules("utils", withBaseDir = false)
includeModules("virtualthread", withProjectName = true, withBaseDir = true)

// Examples (library style examples)
includeModules("examples", withProjectName = true, withBaseDir = true)
includeModules("examples/ktor", false, false)

fun includeModules(baseDir: String, withProjectName: Boolean = true, withBaseDir: Boolean = true) {
    files("$rootDir/$baseDir").files
        .filter { it.isDirectory }
        .forEach { moduleDir ->
            moduleDir.listFiles()
                ?.filter { it.isDirectory }
                ?.filter { File(it, "build.gradle.kts").isFile }
                ?.forEach { dir ->
                    val basePath = baseDir.replace("/", "-")
                    val projectName = when {
                        !withProjectName && !withBaseDir -> dir.name
                        withProjectName && !withBaseDir  -> projectName + "-" + dir.name
                        withProjectName                  -> projectName + "-" + basePath + "-" + dir.name
                        else                             -> basePath + "-" + dir.name
                    }
                    // println("include modules: $projectName")

                    include(projectName)
                    project(":$projectName").projectDir = dir
                }
        }
}
