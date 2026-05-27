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

val bluetape4kDependenciesCatalogRef = providers.gradleProperty("bluetape4kDependenciesCatalogRef")
    .orElse(providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_REF"))
    .orElse("catalog/2026-05-26-01")
    .get()

fun resolveBluetape4kDependenciesCatalogFile(): File {
    providers.gradleProperty("bluetape4kDependenciesCatalogPath")
        .orElse(providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_PATH"))
        .orNull
        ?.let(::file)
        ?.let { return it }

    listOf(
        "../bluetape4k-dependencies/gradle/libs.versions.toml",
        "bluetape4k-dependencies/gradle/libs.versions.toml",
    ).map(::file).firstOrNull { it.isFile }?.let { return it }

    val catalogFile = file(".gradle/bluetape4k-dependencies/libs.versions.toml")
    if (!catalogFile.isFile) {
        catalogFile.parentFile.mkdirs()
        val catalogUrl =
            "https://raw.githubusercontent.com/bluetape4k/bluetape4k-dependencies/$bluetape4kDependenciesCatalogRef/gradle/libs.versions.toml"
        uri(catalogUrl).toURL().openStream().use { input ->
            catalogFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return catalogFile
}

val bluetape4kDependenciesCatalogFile = resolveBluetape4kDependenciesCatalogFile()

require(bluetape4kDependenciesCatalogFile.isFile) {
    "bluetape4k-dependencies catalog not found: $bluetape4kDependenciesCatalogFile. " +
        "Checkout bluetape4k-dependencies at the release-train tag or set bluetape4kDependenciesCatalogPath."
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
    versionCatalogs {
        create("bt4k") {
            from(files(bluetape4kDependenciesCatalogFile))
        }
    }
}

val projectName = "bluetape4k"

rootProject.name = "$projectName-projects"

includeModules("bluetape4k", true, false)

includeModules("cache", withBaseDir = false)
includeModules("data", withBaseDir = false)
includeModules("infra", withBaseDir = false)
includeModules("io", withBaseDir = false)
includeModules("ktor", withBaseDir = true)
includeModules("spring-boot", withBaseDir = true)
includeModules("testing", withBaseDir = false)
includeModules("utils", withBaseDir = false)
includeModules("virtualthread", withProjectName = true, withBaseDir = true)

// Examples (library style examples)
includeModules("examples", withProjectName = true, withBaseDir = true)
includeModules("examples/spring-boot", false, false)
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
