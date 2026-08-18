package io.bluetape4k.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Gradle TestKit에서 [DetektSourceCoverageTask]의 실제 실행 경계를 검증하는
 * 최소 fixture plugin입니다.
 *
 * 테스트용 project property만 configuration 단계에서 읽고, task action에는
 * 직렬화 가능한 property와 파일 집합만 전달합니다.
 */
class DetektSourceCoverageTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val includeEmptyModule = project.providers.gradleProperty("includeEmptyModule")
            .map(String::toBoolean)
            .orElse(false)
        val fixtureRoot = project.projectDir.absolutePath
        val emptyRoot = project.layout.projectDirectory.dir("empty").asFile.absolutePath
        val fixtureSourceFiles = listOf(
            project.layout.projectDirectory.file("src/main/kotlin/Fixture.kt").asFile,
            project.layout.projectDirectory.file("src/test/kotlin/FixtureTest.kt").asFile,
        )
        val report = project.layout.buildDirectory.file("reports/detekt/source-coverage.md")

        project.tasks.register<DetektSourceCoverageTask>("detektSourceCoverage") {
            projectPaths.set(
                includeEmptyModule.map { includeEmpty ->
                    if (includeEmpty) listOf(":fixture", ":empty") else listOf(":fixture")
                },
            )
            projectRoots.set(
                includeEmptyModule.map { includeEmpty ->
                    buildMap {
                        put(":fixture", fixtureRoot)
                        if (includeEmpty) {
                            put(":empty", emptyRoot)
                        }
                    }
                },
            )
            explicitExclusions.set(
                mapOf(
                    ":zeta" to "Z exclusion",
                    ":alpha" to "A exclusion",
                ),
            )
            sourceFiles.from(fixtureSourceFiles)
            reportFile.set(report)
        }
    }
}
