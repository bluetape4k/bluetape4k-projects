import io.bluetape4k.gradle.applyBluetape4kPomMetadata
import io.bluetape4k.gradle.centralSnapshotsRepository
import io.bluetape4k.gradle.configurePublishingSigning
import io.bluetape4k.gradle.resolveCentralPublishingConfig
import io.bluetape4k.gradle.resolvePublishingSigningConfig
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    base
    `maven-publish`
    signing
    // jacoco
    alias(libs.plugins.kotlin.jvm)

    // see: https://kotlinlang.org/docs/reference/compiler-plugins.html
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlin.noarg) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlinx.benchmark) apply false

    alias(libs.plugins.detekt)

    alias(libs.plugins.dependency.management)
    alias(libs.plugins.spring.boot3) apply false

    alias(libs.plugins.dokka)
    alias(libs.plugins.test.logger)
    alias(libs.plugins.shadow) apply false

    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.nmcp.aggregation)
    alias(libs.plugins.nmcp) apply false

    alias(libs.plugins.dependency.check)

    // 테스트 커버리지 (Kotlin inline/suspend 정확 지원)
    alias(libs.plugins.kover)
}

val centralPublishing = resolveCentralPublishingConfig()
val centralUser: String = centralPublishing.username
val centralPassword: String = centralPublishing.password
val centralSnapshotsParallelism: Int = providers
    .gradleProperty("centralSnapshotsParallelism")
    .map(String::toInt)
    .orElse(8)
    .get()

val projectGroup: String by project
val baseVersion: String by project
val snapshotVersion: String by project

allprojects {
    group = projectGroup
    version = baseVersion + snapshotVersion

    repositories {
        mavenCentral()
        google()
        // GeoTools (LGPL) — Shapefile 처리 시 필요
        maven("https://repo.osgeo.org/repository/release/")
        // UCAR/Unidata — NetCDF/CDM 라이브러리
        maven("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    }
}

// Capture root-project catalog reference once; used inside subprojects {} closures
// where `libs` is not in scope (different receiver type in the lambda).
val rootLibs = libs

subprojects {
    if (!path.contains("workshop") && !path.contains("examples") && !path.contains("-demo") && !path.endsWith("-benchmark")) {
        apply(plugin = "com.gradleup.nmcp")
    }

    configurations.matching { it.name.startsWith("nmcp") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
                useVersion("1.9.0")
                because("nmcp 1.4.4 runtime compatibility (avoid serialization ABI mismatch)")
            }
        }
    }


    plugins.withId("com.gradleup.nmcp") {
        extensions.configure<NmcpExtension>("nmcp") {
            publishAllPublicationsToCentralPortal {
                username.set(centralUser)
                password.set(centralPassword)
                publishingType.set("AUTOMATIC")
                uploadSnapshotsParallelism.set(centralSnapshotsParallelism)
            }
        }
    }
}

subprojects {
    if (name == "bluetape4k-bom") {
        return@subprojects
    }

    apply {
        plugin<JavaLibraryPlugin>()

        // Kotlin 1.9.20 부터는 pluginId 를 지정해줘야 합니다.
        plugin("org.jetbrains.kotlin.jvm")

        // Atomicfu
        plugin("org.jetbrains.kotlinx.atomicfu")

        // Kover — Kotlin 코드 커버리지 (examples/workshop/-demo/-benchmark 는 별도 필터링)
        if (!path.contains("workshop") && !path.contains("examples") && !path.contains("-demo") && !path.endsWith("-benchmark")) {
            plugin("org.jetbrains.kotlinx.kover")
        }

        plugin("maven-publish")
        plugin("signing")

        plugin("io.spring.dependency-management")

        plugin("org.jetbrains.dokka")
        plugin("com.adarshr.test-logger")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        kotlin {
            jvmToolchain(21)
            compilerOptions {
                languageVersion.set(KotlinVersion.KOTLIN_2_3)
                apiVersion.set(KotlinVersion.KOTLIN_2_3)
                freeCompilerArgs = listOf(
                    "-Xjsr305=strict",
                    "-jvm-default=enable",
                    // "-Xinline-classes",
                    "-Xstring-concat=indy",         // since Kotlin 1.4.20 for JVM 9+
                    "-Xcontext-parameters",           // since Kotlin 1.6
                    "-Xannotation-default-target=param-property"
                )
                val experimentalAnnotations = listOf(
                    "kotlin.RequiresOptIn",
                    "kotlin.ExperimentalStdlibApi",
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlin.experimental.ExperimentalTypeInference",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.InternalCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview",
                    "kotlinx.coroutines.DelicateCoroutinesApi",
                )
                freeCompilerArgs.addAll(experimentalAnnotations.map { "-opt-in=$it" })
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlinx.atomicfu") {
        atomicfu {
            transformJvm = true
            jvmVariant = "VH"     //  FU, VH, BOTH
        }
    }

    // testFixtures 소스셋은 테스트 유틸리티이므로 커버리지 측정에서 제외
    pluginManager.withPlugin("java-test-fixtures") {
        pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
            kover {
                currentProject {
                    sources {
                        excludedSourceSets.add("testFixtures")
                    }
                }
            }
        }
    }

    // Kotlin 인터페이스 default 메서드 bridge 클래스는 컴파일러 생성 코드 — 커버리지 제외
    pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
        kover {
            currentProject {
                instrumentation {
                    excludedClasses.add("**\$DefaultImpls")
                }
            }
            reports {
                filters {
                    excludes {
                        classes("**\$DefaultImpls")
                    }
                }
            }
        }
    }

    tasks {
        val signingUsesGpgCmd = resolvePublishingSigningConfig().useGpgCmd

        compileJava {
            options.isIncremental = true
        }

        compileKotlin {
            compilerOptions {
                incremental = true
            }
        }

        // 멀티 모듈들을 테스트 시에 동시에 실행되지 않게 하기 위해 Mutex 를 활용합니다.
        abstract class TestMutexService: BuildService<BuildServiceParameters.None>
        abstract class SigningMutexService: BuildService<BuildServiceParameters.None>
        abstract class NmcpPublishMutexService: BuildService<BuildServiceParameters.None>

        val testMutex = gradle.sharedServices.registerIfAbsent(
            "test-mutex",
            TestMutexService::class
        ) {
            maxParallelUsages.set(1)
        }
        val signingMutex = gradle.sharedServices.registerIfAbsent(
            "signing-mutex",
            SigningMutexService::class
        ) {
            maxParallelUsages.set(1)
        }
        val nmcpPublishMutex = gradle.sharedServices.registerIfAbsent(
            "nmcp-publish-mutex",
            NmcpPublishMutexService::class
        ) {
            maxParallelUsages.set(1)
        }

        test {
            usesService(testMutex)

            useJUnitPlatform()

            // bluetape4k.* 시스템 프로퍼티를 테스트 JVM에 전달 (골든 이미지 갱신 모드 등)
            System.getProperties()
                .entries
                .filter { it.key.toString().startsWith("bluetape4k.") }
                .forEach { systemProperty(it.key.toString(), it.value.toString()) }

            // 테스트 시 아래와 같은 예외 메시지를 제거하기 위해서
            // OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
            jvmArgs(
                "-Xshare:off",
                "-Xms2M",
                "-Xmx4G",
                "-XX:+UseG1GC",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableDynamicAgentLoading",
                "--enable-preview",
                "-Didea.io.use.nio2=true"
            )

            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true

                events("failed")
            }
        }

        withType<Sign>().configureEach {
            if (signingUsesGpgCmd) {
                usesService(signingMutex)
            }
        }
        configureEach {
            if (name.startsWith("nmcpPublishAllPublicationsToCentral")) {
                usesService(nmcpPublishMutex)
            }
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            showFullStackTraces = true
        }

        val reportMerge by registering(ReportMergeTask::class) {
            val file = rootProject.layout.buildDirectory.asFile.get().resolve("reports/detekt/exposed.xml")
            output.set(file)
        }
        withType<Detekt>().configureEach detekt@{
            enabled = this@subprojects.name !== "exposed-tests"
            finalizedBy(reportMerge)
            reportMerge.configure {
                input.from(this@detekt.xmlReportFile)
            }
        }

        jar {
            manifest.attributes["Specification-Title"] = project.name
            manifest.attributes["Specification-Version"] = project.version
            manifest.attributes["Implementation-Title"] = project.name
            manifest.attributes["Implementation-Version"] = project.version
            manifest.attributes["Automatic-Module-Name"] = project.name.replace('-', '.')
            manifest.attributes["Created-By"] =
                "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})"
        }

        dokka {
            dokkaPublications.html {
                val javadocDir = layout.buildDirectory.asFile.get().resolve("javadoc")
                outputDirectory.set(javadocDir)
            }
            dokkaSourceSets.configureEach {
                includes.from(project.files("README.md"))
            }
        }

        clean {
            doLast {
                delete("./.project")
                delete("./out")
                delete("./bin")
            }
        }
    }

    dependencyManagement {
        // HINT: Gradle 빌드 시, detachedConfiguration 이 많이 발생하는데, setApplyMavenExclusions(false) 를 추가하면 속도가 개선됩니다.
        // https://discuss.gradle.org/t/what-is-detachedconfiguration-i-have-a-lots-of-them-for-each-subproject-and-resolving-them-takes-95-of-build-time/31595/6
        setApplyMavenExclusions(false)

        imports {
            // spring_integration_bom, spring_cloud_dependencies, spring_boot3_dependencies 는
            // 각 모듈에서 implementation(platform(...)) 으로 직접 선언합니다.
            // (spring-boot4/ 모듈과 mock-server 는 SB4 BOM을 사용)

            // Reactor BOM: SB BOM에서 분리됐으므로 전역 관리 유지
            mavenBom(rootLibs.reactor.bom.get().toString())

            mavenBom(rootLibs.feign.bom.get().toString())
            mavenBom(rootLibs.micrometer.bom.get().toString())
            mavenBom(rootLibs.micrometer.tracing.bom.get().toString())
            mavenBom(rootLibs.opentelemetry.bom.get().toString())
            mavenBom(rootLibs.opentelemetry.alpha.bom.get().toString())
            mavenBom(rootLibs.opentelemetry.instrumentation.bom.alpha.get().toString())
            mavenBom(rootLibs.log4j.bom.get().toString())
            mavenBom(rootLibs.testcontainers.bom.get().toString())
            mavenBom(rootLibs.junit.bom.get().toString())
            mavenBom(rootLibs.aws2.bom.get().toString())
            mavenBom(rootLibs.okhttp3.bom.get().toString())
            mavenBom(rootLibs.grpc.bom.get().toString())
            mavenBom(rootLibs.protobuf.bom.get().toString())
            mavenBom(rootLibs.fabric8.kubernetes.client.bom.get().toString())
            mavenBom(rootLibs.resilience4j.bom.get().toString())
            mavenBom(rootLibs.netty.bom.get().toString())
            mavenBom(rootLibs.jackson.bom.get().toString())

            mavenBom(rootLibs.kotlinx.coroutines.bom.get().toString())
            mavenBom(rootLibs.kotlin.bom.get().toString())
        }
        dependencies {
            dependency(rootLibs.jetbrains.annotations.get().toString())

            // Kotlinx Coroutines (mavenBom 이 적용이 안되어서 추가로 명시했습니다)
            dependency(rootLibs.kotlinx.coroutines.bom.get().toString())
            dependency(rootLibs.kotlinx.coroutines.core.asProvider().get().toString())
            dependency(rootLibs.kotlinx.coroutines.core.jvm.get().toString())
            dependency(rootLibs.kotlinx.coroutines.reactive.get().toString())
            dependency(rootLibs.kotlinx.coroutines.reactor.get().toString())
            dependency(rootLibs.kotlinx.coroutines.rx2.get().toString())
            dependency(rootLibs.kotlinx.coroutines.rx3.get().toString())
            dependency(rootLibs.kotlinx.coroutines.slf4j.get().toString())
            dependency(rootLibs.kotlinx.coroutines.debug.get().toString())
            dependency(rootLibs.kotlinx.coroutines.test.asProvider().get().toString())
            dependency(rootLibs.kotlinx.coroutines.test.jvm.get().toString())

            // Apache Commons
            dependency(rootLibs.commons.beanutils.get().toString())
            dependency(rootLibs.commons.collections4.get().toString())
            dependency(rootLibs.commons.compress.get().toString())
            dependency(rootLibs.commons.codec.get().toString())
            dependency(rootLibs.commons.csv.get().toString())
            dependency(rootLibs.commons.lang3.get().toString())
            dependency(rootLibs.commons.logging.get().toString())
            dependency(rootLibs.commons.math3.get().toString())
            dependency(rootLibs.commons.pool2.get().toString())
            dependency(rootLibs.commons.text.get().toString())
            dependency(rootLibs.commons.exec.get().toString())
            dependency(rootLibs.commons.io.get().toString())

            dependency(rootLibs.slf4j.api.get().toString())
            dependency(rootLibs.jcl.over.slf4j.get().toString())
            dependency(rootLibs.jul.to.slf4j.get().toString())
            dependency(rootLibs.log4j.over.slf4j.get().toString())
            dependency(rootLibs.logback.classic.get().toString())
            dependency(rootLibs.logback.core.get().toString())

            // jakarta
            dependency(rootLibs.jakarta.activation.api.get().toString())
            dependency(rootLibs.jakarta.annotation.api.get().toString())
            dependency(rootLibs.jakarta.el.api.get().toString())
            dependency(rootLibs.jakarta.inject.api.get().toString())
            dependency(rootLibs.jakarta.interceptor.api.get().toString())
            dependency(rootLibs.jakarta.jms.api.get().toString())
            dependency(rootLibs.jakarta.json.api.get().toString())
            dependency(rootLibs.jakarta.json.asProvider().get().toString())
            dependency(rootLibs.jakarta.persistence.api.asProvider().get().toString())
            dependency(rootLibs.jakarta.servlet.api.get().toString())
            dependency(rootLibs.jakarta.transaction.api.get().toString())
            dependency(rootLibs.jakarta.validation.api.get().toString())
            dependency(rootLibs.jakarta.ws.rs.api.get().toString())
            dependency(rootLibs.jakarta.xml.bind.get().toString())

            // Compressor
            dependency(rootLibs.snappy.java.get().toString())
            dependency(rootLibs.lz4.java.get().toString())
            dependency(rootLibs.zstd.jni.get().toString())

            // Java Money
            dependency(rootLibs.javax.money.api.get().toString())
            dependency(rootLibs.javamoney.moneta.get().toString())

            dependency(rootLibs.findbugs.get().toString())
            dependency(rootLibs.guava.get().toString())

            dependency(rootLibs.kryo.get().toString())

            // HINT: Jackson (이상하게 mavenBom 에 적용이 안되어서 강제로 추가하였다)
            dependency(rootLibs.jackson.bom.get().toString())
            dependency(rootLibs.jackson.annotations.get().toString())
            dependency(rootLibs.jackson.core.get().toString())
            dependency(rootLibs.jackson.databind.get().toString())
            dependency(rootLibs.jackson.datatype.jdk8.get().toString())
            dependency(rootLibs.jackson.datatype.jsr310.get().toString())
            dependency(rootLibs.jackson.datatype.jsr353.get().toString())
            dependency(rootLibs.jackson.module.kotlin.get().toString())
            dependency(rootLibs.jackson.module.paranamer.get().toString())
            dependency(rootLibs.jackson.module.parameter.names.get().toString())
            dependency(rootLibs.jackson.module.blackbird.get().toString())
            dependency(rootLibs.jackson.module.json.schema.get().toString())

            dependency(rootLibs.jackson.dataformat.avro.get().toString())
            dependency(rootLibs.jackson.dataformat.cbor.get().toString())
            dependency(rootLibs.jackson.dataformat.ion.get().toString())
            dependency(rootLibs.jackson.dataformat.protobuf.get().toString())
            dependency(rootLibs.jackson.dataformat.smile.get().toString())
            dependency(rootLibs.jackson.dataformat.csv.get().toString())
            dependency(rootLibs.jackson.dataformat.properties.get().toString())
            dependency(rootLibs.jackson.dataformat.yaml.get().toString())

            // Retrofit
            dependency(rootLibs.retrofit2.asProvider().get().toString())
            dependency(rootLibs.retrofit2.adapter.java8.get().toString())
            dependency(rootLibs.retrofit2.adapter.reactor.get().toString())
            dependency(rootLibs.retrofit2.adapter.rxjava2.get().toString())
            dependency(rootLibs.retrofit2.converter.jackson.get().toString())
            dependency(rootLibs.retrofit2.converter.moshi.get().toString())
            dependency(rootLibs.retrofit2.converter.protobuf.get().toString())
            dependency(rootLibs.retrofit2.converter.scalars.get().toString())
            dependency(rootLibs.retrofit2.mock.get().toString())


            dependency(rootLibs.httpclient5.asProvider().get().toString())

            dependency(rootLibs.grpc.kotlin.stub.get().toString())

            dependency(rootLibs.mongo.bson.asProvider().get().toString())
            dependency(rootLibs.mongo.bson.kotlin.get().toString())
            dependency(rootLibs.mongo.bson.kotlinx.get().toString())
            dependency(rootLibs.mongodb.driver.core.get().toString())
            dependency(rootLibs.mongodb.driver.reactivestreams.get().toString())
            dependency(rootLibs.mongodb.driver.sync.get().toString())
            dependency(rootLibs.mongodb.driver.kotlin.coroutine.get().toString())
            dependency(rootLibs.mongodb.driver.kotlin.extensions.get().toString())
            dependency(rootLibs.mongodb.driver.kotlin.sync.get().toString())

            // Kafka
            dependency(rootLibs.kafka.clients.get().toString())
            dependency(rootLibs.kafka.generator.get().toString())
            dependency(rootLibs.kafka.metadata.get().toString())
            dependency(rootLibs.kafka.raft.get().toString())
            dependency(rootLibs.kafka.server.common.get().toString())
            dependency(rootLibs.kafka.storage.asProvider().get().toString())
            dependency(rootLibs.kafka.storage.api.get().toString())
            dependency(rootLibs.kafka.streams.asProvider().get().toString())
            dependency(rootLibs.kafka.streams.test.utils.get().toString())
            dependency(rootLibs.kafka.scala213.get().toString())

            // Hibernate
            dependency(rootLibs.hibernate.core.get().toString())
            dependency(rootLibs.hibernate.jcache.get().toString())
            dependency(rootLibs.javassist.get().toString())

            dependency(rootLibs.antlr4.runtime.get().toString())  // https://github.com/spring-projects/spring-data-jpa/issues/3262
            dependency(rootLibs.antlr4.tool.get().toString())

            dependency(rootLibs.querydsl.apt.get().toString())
            dependency(rootLibs.querydsl.core.get().toString())
            dependency(rootLibs.querydsl.jpa.get().toString())

            // Validators
            dependency(rootLibs.jakarta.el.api.get().toString())
            dependency(rootLibs.jakarta.validation.api.get().toString())
            dependency(rootLibs.hibernate.validator.asProvider().get().toString())
            dependency(rootLibs.hibernate.validator.annotation.processor.get().toString())

            dependency(rootLibs.hikaricp.get().toString())
            dependency(rootLibs.mysql.connector.j.get().toString())
            dependency(rootLibs.mariadb.java.client.get().toString())

            dependency(rootLibs.caffeine.asProvider().get().toString())
            dependency(rootLibs.caffeine.jcache.get().toString())

            dependency(rootLibs.objenesis.get().toString())
            dependency(rootLibs.ow2.asm.asProvider().get().toString())

            dependency(rootLibs.reflectasm.get().toString())

            dependency(rootLibs.junit.bom.get().toString())
            dependency(rootLibs.junit.jupiter.asProvider().get().toString())
            dependency(rootLibs.junit.jupiter.api.get().toString())
            dependency(rootLibs.junit.jupiter.engine.get().toString())
            dependency(rootLibs.junit.jupiter.migrationsupport.get().toString())
            dependency(rootLibs.junit.jupiter.params.get().toString())
            dependency(rootLibs.junit.platform.commons.get().toString())
            dependency(rootLibs.junit.platform.engine.get().toString())
            dependency(rootLibs.junit.platform.launcher.get().toString())

            dependency(rootLibs.kluent.get().toString())
            dependency(rootLibs.assertj.core.get().toString())

            dependency(rootLibs.mockk.get().toString())
            dependency(rootLibs.datafaker.get().toString())
            dependency(rootLibs.random.beans.get().toString())

            dependency(rootLibs.jsonpath.get().toString())
            dependency(rootLibs.jsonassert.get().toString())

            dependency(rootLibs.bouncycastle.bcpkix.get().toString())
            dependency(rootLibs.bouncycastle.bcprov.get().toString())

            // Prometheus
            dependency(rootLibs.prometheus.simpleclient.asProvider().get().toString())
            dependency(rootLibs.prometheus.simpleclient.common.get().toString())
            dependency(rootLibs.prometheus.simpleclient.httpserver.get().toString())
            dependency(rootLibs.prometheus.simpleclient.pushgateway.get().toString())
            dependency(rootLibs.prometheus.simpleclient.spring.boot.get().toString())
            dependency(rootLibs.prometheus.simpleclient.tracer.common.get().toString())
            dependency(rootLibs.prometheus.simpleclient.tracer.otel.asProvider().get().toString())
            dependency(rootLibs.prometheus.simpleclient.tracer.otel.agent.get().toString())

            // OW2 ASM
            dependency(rootLibs.ow2.asm.asProvider().get().toString())
            dependency(rootLibs.ow2.asm.commons.get().toString())
            dependency(rootLibs.ow2.asm.util.get().toString())
            dependency(rootLibs.ow2.asm.tree.get().toString())

            dependency(rootLibs.snakeyaml.get().toString())
            dependency(rootLibs.jna.asProvider().get().toString())

            // ByteBuddy
            dependency(rootLibs.byte.buddy.asProvider().get().toString())
            dependency(rootLibs.byte.buddy.agent.get().toString())
        }
    }

    dependencies {
        val api by configurations
        val implementation by configurations
        val testImplementation by configurations

        val compileOnly by configurations
        val testCompileOnly by configurations
        val testRuntimeOnly by configurations

        api(rootLibs.jetbrains.annotations)

        implementation(rootLibs.kotlin.stdlib)
        implementation(rootLibs.kotlin.reflect)
        testImplementation(rootLibs.kotlin.test)
        testImplementation(rootLibs.kotlin.test.junit5)

        implementation(rootLibs.kotlinx.coroutines.core)
        implementation(rootLibs.kotlinx.atomicfu)

        // 개발 시에는 logback 이 검증하기에 더 좋고, Production에서 비동기 로깅은 log4j2 가 성능이 좋다고 합니다.
        api(rootLibs.slf4j.api)
        testImplementation(rootLibs.logback.classic)
        testImplementation(rootLibs.jcl.over.slf4j)
        testImplementation(rootLibs.jul.to.slf4j)
        testImplementation(rootLibs.log4j.over.slf4j)

        testImplementation(rootLibs.junit.jupiter)
        testRuntimeOnly(rootLibs.junit.platform.engine)
        testImplementation(rootLibs.junit.jupiter.migrationsupport)

        testImplementation(rootLibs.kluent)
        if (name != "bluetape4k-assertions") {
            testImplementation(project(":bluetape4k-assertions"))
        }
        testImplementation(rootLibs.awaitility.kotlin)
        testImplementation(rootLibs.mockk)

        // Property based test
        testImplementation(rootLibs.datafaker)
        testImplementation(rootLibs.random.beans)
    }

    tasks.withType<Jar> {
        manifest.attributes["Specification-Title"] = project.name
        manifest.attributes["Specification-Version"] = project.version
        manifest.attributes["Implementation-Title"] = project.name
        manifest.attributes["Implementation-Version"] = project.version
        manifest.attributes["Automatic-Module-Name"] = project.name.replace('-', '.')
        manifest.attributes["Created-By"] =
            "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})"
    }

    /*
        1. mavenLocal 에 publish 시에는 ./gradlew publishMavenPublicationToMavenLocalRepository 를 수행
        2. Maven Central 배포는 Central Portal Publisher API 기반의 aggregation task를 사용합니다.

        ```bash
        $ ./gradlew clean build
        $ ./gradlew publishAggregationToCentralPortal
        ```
     */
    publishing {
        publications {
            if (!project.path.contains("workshop") && !project.path.contains("examples") && !project.path.contains("-demo") && !project.path.endsWith("-benchmark")) {
                create<MavenPublication>("Bluetape4k") {
                    val binaryJar = components["java"]

                    val sourcesJar by tasks.registering(Jar::class) {
                        archiveClassifier.set("sources")
                        from(sourceSets["main"].allSource)
                    }

                    val javadocJar by tasks.registering(Jar::class) {
                        archiveClassifier.set("javadoc")
                        val javadocDir = layout.buildDirectory.asFile.get().resolve("javadoc")
                        from(javadocDir.path)
                    }

                    from(binaryJar)
                    artifact(sourcesJar)
                    artifact(javadocJar)

                    pom {
                        applyBluetape4kPomMetadata(
                            artifactDisplayName = project.name,
                            artifactDescription = "Common Library for Kotlin",
                        )
                    }
                }
            }
        }
        repositories {
            centralSnapshotsRepository(project)
            mavenLocal()
        }
    }

    configurePublishingSigning(
        publicationName = "Bluetape4k",
        enabled = !project.path.contains("workshop") &&
                !project.path.contains("examples") &&
                !project.path.contains("-demo") &&
                !project.path.endsWith("-benchmark"),
    )

    tasks.withType<GenerateMavenPom>().configureEach {
        notCompatibleWithConfigurationCache("publishing tasks are not cache-safe")
    }
    tasks.withType<PublishToMavenRepository>().configureEach {
        notCompatibleWithConfigurationCache("publishing tasks are not cache-safe")
        if (repository.name == "nmcp") {
            repository.url = uri(layout.buildDirectory.dir("nmcp/m2"))
        }
    }
    tasks.withType<PublishToMavenLocal>().configureEach {
        notCompatibleWithConfigurationCache("publishing tasks are not cache-safe")
    }
    tasks.matching { it.name.endsWith("ToNmcpRepository") }.configureEach {
        outputs.upToDateWhen { false }
    }
}

val publishableProjects = subprojects.filterNot { project ->
    project.path.contains("workshop") || project.path.contains("examples") || project.path.contains("-demo") || project.path.endsWith("-benchmark")
}

extensions.configure<NmcpAggregationExtension>("nmcpAggregation") {
    centralPortal {
        username.set(centralUser)
        password.set(centralPassword)
        publishingType.set("AUTOMATIC")
        uploadSnapshotsParallelism.set(centralSnapshotsParallelism)
    }
}

dependencies {
    publishableProjects.forEach { publishableProject ->
        add("nmcpAggregation", project(publishableProject.path))
    }
}

// ── OWASP Dependency Check ────────────────────────────────────────────────
dependencyCheck {
    // NVD API 키 (환경변수 NVD_API_KEY 또는 Gradle 프로퍼티로 전달)
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY")
        .orElse(providers.gradleProperty("nvdApiKey"))
        .orElse("")
        .get()
    // 취약점 점수 7.0 이상이면 빌드 실패 (CVSS High/Critical)
    failBuildOnCVSS = 7.0f
    // 분석 제외: 테스트, 컴파일 전용 의존성
    skipConfigurations = listOf("testRuntimeClasspath", "testCompileClasspath")
    formats = listOf("HTML", "SARIF")
    outputDirectory = layout.buildDirectory.dir("reports").get().asFile
    suppressionFile = "config/owasp-suppressions.xml"
}

// ─── Kover 집계 설정 ────────────────────────────────────────────────────
// 루트 프로젝트에서 모든 측정 대상 서브모듈을 `kover` 의존성으로 등록하여
// `./gradlew koverXmlReport` / `koverHtmlReport` 실행 시 집계된 리포트를 생성한다.
kover {
    reports {
        filters {
            excludes {
                // Kotlin 컴파일러 생성 interface bridge 클래스 — 집계 리포트에서도 제외
                classes("**\$DefaultImpls")
            }
        }
    }
}

dependencies {
    subprojects
        .filter { sub ->
            sub.name != "bluetape4k-bom" &&
                    !sub.path.contains("workshop") &&
                    !sub.path.contains("examples") &&
                    !sub.path.contains("-demo") &&
                    !sub.path.endsWith("-benchmark")
        }
        .forEach { sub -> kover(project(sub.path)) }
}

tasks.register("testDataExposedModules") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Run tests for exposed* modules under the data directory in a single Gradle invocation."

    val exposedTestTasks = provider {
        val dataRoot = rootDir.toPath().resolve("data")
        val exposedProjects = subprojects.filter { project ->
            project.projectDir.toPath().startsWith(dataRoot) &&
                    project.name.startsWith("bluetape4k-exposed")
        }

        exposedProjects.map { it.path + ":test" }
    }
    dependsOn(exposedTestTasks)
}
