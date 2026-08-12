package io.bluetape4k.gradle

const val TESTCONTAINERS_PROJECT_PATH: String = ":bluetape4k-testcontainers"

/**
 * Testcontainers 모듈 자체 또는 해당 모듈에 직접 의존하는 프로젝트의 테스트를 직렬화할지 판정합니다.
 */
fun shouldSerializeTestcontainersTests(
    projectPath: String,
    dependencyProjectPaths: Iterable<String>,
): Boolean = projectPath == TESTCONTAINERS_PROJECT_PATH ||
        dependencyProjectPaths.any { it == TESTCONTAINERS_PROJECT_PATH }
