package io.bluetape4k.gradle

import org.gradle.api.Project
import java.io.File

private val applicationOnlyProjects = setOf(
    "bluetape4k-mock-web-server",
    "bluetape4k-mock-webflux-server",
)

fun isPublishableLibraryProject(relativeProjectDir: String, projectName: String): Boolean {
    val normalizedProjectDir = relativeProjectDir.replace(File.separatorChar, '/')

    return projectName != "bluetape4k-bom" &&
            projectName !in applicationOnlyProjects &&
            normalizedProjectDir != "workshop" &&
            !normalizedProjectDir.startsWith("workshop/") &&
            normalizedProjectDir != "examples" &&
            !normalizedProjectDir.startsWith("examples/") &&
            !projectName.contains("-demo") &&
            !projectName.endsWith("-benchmark")
}

fun Project.isPublishableLibraryProject(): Boolean {
    val relativeProjectDir = rootDir.toPath()
        .relativize(projectDir.toPath())
        .toString()

    return isPublishableLibraryProject(relativeProjectDir, name)
}
