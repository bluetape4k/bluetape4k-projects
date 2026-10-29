configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NATS
    api(bt4k.jnats)
    // jnats의 전이 버전은 취약하므로 컴파일 경로도 중앙 catalog 버전으로 맞춥니다.
    compileOnly(bt4k.bouncycastle.bcprov.lts8on)
    // jnats의 전이 버전은 취약하므로 Maven 소비자도 패치 버전을 선택하도록 중앙 catalog를 런타임에 선언합니다.
    runtimeOnly(bt4k.bouncycastle.bcprov.lts8on)
    // nats-spring은 Spring Boot 3/4 통합 시 사용자가 직접 선언 (compileOnly로 API만 노출)
    compileOnly(bt4k.nats.spring)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Jackson (테스트 페이로드 인코딩용) — bluetape4k-jackson3가 나머지 전이 포함
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.jackson3.databind)
    testImplementation(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)

    // Compressors / Serializers (테스트 페이로드용)
    testImplementation(bt4k.at.yawk.lz4.java)
    testImplementation(bt4k.snappy.java)
    testImplementation(bt4k.zstd.jni)
    testImplementation(bt4k.fory.kotlin)
    testImplementation(bt4k.kryo5)
}

val verifyNatsBouncyCastleCompileClasspathVersion = tasks.register("verifyNatsBouncyCastleCompileClasspathVersion") {
    group = "verification"
    description = "NATS compileClasspath가 패치된 Bouncy Castle LTS 버전을 선택하는지 확인합니다."
    inputs.files(configurations.getByName("compileClasspath"))

    doLast {
        val artifacts = inputs.files.files.filter {
            it.name.startsWith("bcprov-lts8on-") && it.name.endsWith(".jar")
        }
        check(artifacts.size == 1) {
            "compileClasspath에서 bcprov-lts8on artifact를 정확히 하나 확인해야 합니다. " +
                "실제: ${artifacts.map { it.name }}"
        }

        val artifact = artifacts.single()
        val version = Regex("""^bcprov-lts8on-(\d+(?:\.\d+)+)\.jar$""")
            .matchEntire(artifact.name)?.groupValues?.get(1)
            ?: error("Bouncy Castle artifact 버전을 읽을 수 없습니다: ${artifact.name}")
        val actualVersion = version.split(".").map { segment ->
            segment.toIntOrNull() ?: error("Bouncy Castle artifact 버전을 비교할 수 없습니다: $version")
        }
        val minimumVersion = listOf(2, 73, 12)
        val comparison = (0 until maxOf(actualVersion.size, minimumVersion.size))
            .map { index -> actualVersion.getOrElse(index) { 0 }.compareTo(minimumVersion.getOrElse(index) { 0 }) }
            .firstOrNull { it != 0 } ?: 0

        check(comparison >= 0) {
            "compileClasspath에서 취약한 Bouncy Castle 버전을 선택했습니다: $version (최소: 2.73.12)"
        }
        logger.lifecycle("NATS compileClasspath Bouncy Castle 버전 검증 통과: $version")
    }
}

val verifyNatsBouncyCastleRuntimeClasspathVersion = tasks.register("verifyNatsBouncyCastleRuntimeClasspathVersion") {
    group = "verification"
    description = "NATS runtimeClasspath가 패치된 Bouncy Castle LTS 버전을 선택하는지 확인합니다."
    inputs.files(configurations.getByName("runtimeClasspath"))

    doLast {
        val artifacts = inputs.files.files.filter {
            it.name.startsWith("bcprov-lts8on-") && it.name.endsWith(".jar")
        }
        check(artifacts.size == 1) {
            "runtimeClasspath에서 bcprov-lts8on artifact를 정확히 하나 확인해야 합니다. " +
                "실제: ${artifacts.map { it.name }}"
        }

        val artifact = artifacts.single()
        val version = Regex("""^bcprov-lts8on-(\d+(?:\.\d+)+)\.jar$""")
            .matchEntire(artifact.name)?.groupValues?.get(1)
            ?: error("Bouncy Castle artifact 버전을 읽을 수 없습니다: ${artifact.name}")
        val actualVersion = version.split(".").map { segment ->
            segment.toIntOrNull() ?: error("Bouncy Castle artifact 버전을 비교할 수 없습니다: $version")
        }
        val minimumVersion = listOf(2, 73, 12)
        val comparison = (0 until maxOf(actualVersion.size, minimumVersion.size))
            .map { index -> actualVersion.getOrElse(index) { 0 }.compareTo(minimumVersion.getOrElse(index) { 0 }) }
            .firstOrNull { it != 0 } ?: 0

        check(comparison >= 0) {
            "runtimeClasspath에서 취약한 Bouncy Castle 버전을 선택했습니다: $version (최소: 2.73.12)"
        }
        logger.lifecycle("NATS runtimeClasspath Bouncy Castle 버전 검증 통과: $version")
    }
}

val verifyNatsBouncyCastleVersion = tasks.register("verifyNatsBouncyCastleVersion") {
    group = "verification"
    description = "NATS 컴파일·런타임 경로가 패치된 Bouncy Castle LTS 버전을 선택하는지 확인합니다."
    dependsOn(verifyNatsBouncyCastleCompileClasspathVersion, verifyNatsBouncyCastleRuntimeClasspathVersion)
}

tasks.named("check") {
    dependsOn(verifyNatsBouncyCastleVersion)
}
