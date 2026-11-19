import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

abstract class DependencyManagementPluginClasspathArgumentProvider : CommandLineArgumentProvider {
    @get:Classpath
    abstract val pluginClasspath: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf("-Dbluetape4k.test.dependency-management-plugin-classpath=${pluginClasspath.asPath}")
}

repositories {
    mavenCentral()
    google()
}

val repositoryRoot = projectDir.parentFile
val settingsText = repositoryRoot.resolve("settings.gradle.kts").readText()
val defaultCatalogRef = Regex(
    """val bluetape4kDependenciesCatalogRef =(?s).*?\.orElse\("([^"]+)"\)""",
).find(settingsText)?.groupValues?.get(1)
    ?: error("Could not find the default bluetape4k-dependencies catalog ref in settings.gradle.kts")
val catalogRef = providers.gradleProperty("bluetape4kDependenciesCatalogRef").orNull
    ?: providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_REF").orNull
    ?: defaultCatalogRef
val catalogCommitPattern = Regex("[0-9a-f]{40}|[0-9a-f]{64}")
val catalogSha = providers.gradleProperty("bluetape4kDependenciesCatalogSha").orNull
    ?: providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_SHA").orNull
    ?: catalogRef.takeIf(catalogCommitPattern::matches)
    ?: error("The bluetape4k-dependencies catalog tag requires its exact commit SHA")
val catalogPathOverride = providers.gradleProperty("bluetape4kDependenciesCatalogPath").orNull
    ?: providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_PATH").orNull
val catalogFile = catalogPathOverride?.let { path ->
    val candidate = File(path)
    if (candidate.isAbsolute) candidate else repositoryRoot.resolve(candidate)
} ?: repositoryRoot.resolve(".gradle/bluetape4k-dependencies/$catalogSha/libs.versions.toml")
require(catalogFile.isFile) { "Shared bluetape4k-dependencies catalog not found: $catalogFile" }

val catalogText = catalogFile.readText()
val dependencyManagementVersionRef = Regex(
    """(?m)^dependency-management\s*=\s*\{[^\r\n]*version\.ref\s*=\s*"([^"]+)"[^\r\n]*}$""",
).find(catalogText)?.groupValues?.get(1)
    ?: error("Could not find the dependency-management plugin alias in $catalogFile")
val dependencyManagementPluginVersion = Regex(
    """(?m)^${Regex.escape(dependencyManagementVersionRef)}\s*=\s*"([^"]+)"(?:\s*#.*)?$""",
).find(catalogText)?.groupValues?.get(1)
    ?: error("Could not find version $dependencyManagementVersionRef in $catalogFile")
val dependencyManagementPluginClasspath = configurations.create("dependencyManagementPluginClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

plugins {
    `kotlin-dsl`
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    add(
        dependencyManagementPluginClasspath.name,
        "io.spring.gradle:dependency-management-plugin:$dependencyManagementPluginVersion",
    )
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgumentProviders.add(
        objects.newInstance<DependencyManagementPluginClasspathArgumentProvider>().apply {
            pluginClasspath.from(dependencyManagementPluginClasspath)
        },
    )
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}
