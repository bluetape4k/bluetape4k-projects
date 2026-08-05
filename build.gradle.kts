import groovy.json.JsonOutput
import io.bluetape4k.gradle.applyBluetape4kPomMetadata
import io.bluetape4k.gradle.centralSnapshotsRepository
import io.bluetape4k.gradle.configurePublishingSigning
import io.bluetape4k.gradle.DisabledTestReportTask
import io.bluetape4k.gradle.isPublishableLibraryProject
import io.bluetape4k.gradle.isPublishedProject
import io.bluetape4k.gradle.resolveCentralPublishingConfig
import io.bluetape4k.gradle.resolvePublishingSigningConfig
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.report.ReportMergeTask
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.File

plugins {
    base
    `maven-publish`
    signing
    // jacoco
    alias(bt4k.plugins.kotlin.jvm)

    // see: https://kotlinlang.org/docs/reference/compiler-plugins.html
    alias(bt4k.plugins.kotlin.spring) apply false
    alias(bt4k.plugins.kotlin.allopen) apply false
    alias(bt4k.plugins.kotlin.noarg) apply false
    alias(bt4k.plugins.kotlin.jpa) apply false
    alias(bt4k.plugins.kotlin.serialization) apply false
    alias(bt4k.plugins.kotlin.kapt) apply false
    alias(bt4k.plugins.kotlinx.atomicfu)
    alias(bt4k.plugins.kotlinx.benchmark) apply false

    alias(bt4k.plugins.detekt.dev)

    alias(bt4k.plugins.dependency.management)
    alias(bt4k.plugins.spring.boot) apply false

    alias(bt4k.plugins.dokka)
    alias(bt4k.plugins.test.logger)
    alias(bt4k.plugins.shadow) apply false

    alias(bt4k.plugins.graalvm.native) apply false
    alias(bt4k.plugins.nmcp.aggregation)
    alias(bt4k.plugins.nmcp) apply false

    alias(bt4k.plugins.dependency.check)

    // 테스트 커버리지 (Kotlin inline/suspend 정확 지원)
    alias(bt4k.plugins.kover)
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
val rootBt4k = bt4k
val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()
fun bt4kVersion(alias: String): String {
    val version = bt4kCatalog.findVersion(alias).get()
    return version.requiredVersion
        .ifBlank { version.preferredVersion }
        .ifBlank { version.strictVersion }
}


fun Project.isSampleOrBenchmarkProject(): Boolean {
    val relativeProjectDir = rootDir.toPath()
        .relativize(projectDir.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    return relativeProjectDir == "workshop" ||
            relativeProjectDir.startsWith("workshop/") ||
            relativeProjectDir == "examples" ||
            relativeProjectDir.startsWith("examples/") ||
            name.contains("-demo") ||
            name.endsWith("-benchmark")
}

private val detektExplicitExclusionReasons = linkedMapOf(
    "bluetape4k-bom" to "BOM metadata-only project has no Kotlin source.",
    "bluetape4k-redis" to "Umbrella project delegates to Lettuce and Redisson and has no Kotlin source.",
    "exposed-jdbc-tests" to "Documented Testcontainers-backed test exception; this project is not registered in this repository.",
)

fun Project.detektExclusionReason(): String? = when {
    name in detektExplicitExclusionReasons -> detektExplicitExclusionReasons.getValue(name)
    isSampleOrBenchmarkProject() -> "Examples, demos, benchmarks, and workshop sources are excluded from library analysis."
    else -> null
}

fun Project.isDetektTargetProject(): Boolean = detektExclusionReason() == null

fun Project.detektKotlinSourceFiles(): FileTree = fileTree(projectDir) {
    include("src/main/kotlin/**/*.kt")
    include("src/test/kotlin/**/*.kt")
    exclude("**/build/**")
    exclude("**/generated/**")
}

subprojects {
    if (isPublishedProject()) {
        apply(plugin = "com.gradleup.nmcp")
    }

    configurations.matching { it.name.startsWith("nmcp") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
                useVersion("1.9.0")
                because("nmcp runtime compatibility (avoid serialization ABI mismatch)")
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

        // Detekt — 실제 Kotlin 소스가 있는 library subproject만 분석 대상으로 등록한다.
        if (isDetektTargetProject()) {
            plugin("dev.detekt")
        }

        // Atomicfu
        plugin("org.jetbrains.kotlinx.atomicfu")

        // Kover — Kotlin 코드 커버리지 (examples/workshop/-demo/-benchmark 는 별도 필터링)
        if (!isSampleOrBenchmarkProject()) {
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

        abstract class SigningMutexService: BuildService<BuildServiceParameters.None>
        abstract class NmcpPublishMutexService: BuildService<BuildServiceParameters.None>

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

        withType<Detekt>().configureEach detekt@{
            exclude("**/build/**")
            exclude("**/generated/**")
            if (name == "detekt" && project.isDetektTargetProject()) {
                setSource(project.detektKotlinSourceFiles())
                reports {
                    checkstyle.required.set(true)
                    checkstyle.outputLocation.set(project.layout.buildDirectory.file("reports/detekt/detekt.xml"))
                }
                // This issue establishes trustworthy source coverage; rule cleanup is tracked separately.
                ignoreFailures.set(true)
                doFirst {
                    val sourceFiles = source.files
                    check(sourceFiles.isNotEmpty()) {
                        "Detekt source scope is empty for $path; refusing a false-green analysis."
                    }
                    logger.lifecycle("Detekt source scope for $path: ${sourceFiles.size} Kotlin files")
                }
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
            // spring_integration_bom, spring_cloud_dependencies, spring_boot_dependencies 는
            // 각 모듈에서 implementation(platform(...)) 으로 직접 선언합니다.
            // (spring-boot/ 모듈과 mock-server 는 Spring Boot 4 BOM을 사용)

            // Reactor BOM: SB BOM에서 분리됐으므로 전역 관리 유지
            mavenBom("io.projectreactor:reactor-bom:${bt4kVersion("reactor-bom")}")

            mavenBom(rootBt4k.feign.bom.get().toString())
            mavenBom(rootBt4k.micrometer.bom.get().toString())
            mavenBom(rootBt4k.micrometer.tracing.bom.get().toString())
            mavenBom(bt4kLibrary("opentelemetry-bom").get().toString())
            mavenBom(rootBt4k.opentelemetry.alpha.bom.get().toString())
            mavenBom(bt4kLibrary("opentelemetry-instrumentation-bom-alpha").get().toString())
            mavenBom(bt4kLibrary("log4j-bom").get().toString())
            mavenBom("org.testcontainers:testcontainers-bom:${bt4kVersion("testcontainers")}")
            mavenBom(rootBt4k.junit.bom.get().toString())
            mavenBom(bt4kLibrary("aws2-bom").get().toString())
            mavenBom(rootBt4k.okhttp3.bom.get().toString())
            mavenBom(rootBt4k.grpc.bom.get().toString())
            mavenBom(bt4kLibrary("protobuf-bom").get().toString())
            mavenBom(bt4kLibrary("fabric8-kubernetes-client-bom").get().toString())
            mavenBom(rootBt4k.resilience4j.bom.get().toString())
            mavenBom(bt4kLibrary("netty-bom").get().toString())
            mavenBom("com.fasterxml.jackson:jackson-bom:${bt4kVersion("jackson")}")

            mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4kVersion("kotlinx-coroutines")}")
            mavenBom("org.jetbrains.kotlin:kotlin-bom:${bt4kVersion("kotlin")}")
        }
        dependencies {
            // <central-catalog-local-aliases>
            dependency("ai.timefold.solver:timefold-solver-benchmark:${bt4kVersion("timefold-solver")}")
            dependency("ai.timefold.solver:timefold-solver-core:${bt4kVersion("timefold-solver")}")
            dependency("ai.timefold.solver:timefold-solver-jackson:${bt4kVersion("timefold-solver")}")
            dependency("ai.timefold.solver:timefold-solver-spring-boot-starter:${bt4kVersion("timefold-solver")}")
            dependency("aws.sdk.kotlin:aws-config:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:aws-endpoint:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:aws-http:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:batch:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:cloudwatch:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:cloudwatchlogs:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:dynamodb:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:dynamodbstreams:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:http-client-engine-crt:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:kafka:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:kinesis:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:kms:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:lambda:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:rds:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:s3:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:ses:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:sesv2:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:sns:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:sqs:${bt4kVersion("aws-kotlin")}")
            dependency("aws.sdk.kotlin:sts:${bt4kVersion("aws-kotlin")}")
            dependency("com.esotericsoftware:reflectasm:${bt4kVersion("reflectasm")}")
            dependency("com.fasterxml.jackson.core:jackson-core:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.core:jackson-databind:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-avro:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-ion:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-protobuf:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-guava:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-joda:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr353:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-blackbird:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-jsonSchema:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-kotlin:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-parameter:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-parameter-names:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson:jackson-bom:${bt4kVersion("jackson")}")
            dependency("com.google.protobuf:protobuf-java-util:${bt4kVersion("protobuf")}")
            dependency("com.google.protobuf:protobuf-kotlin:${bt4kVersion("protobuf")}")
            dependency("com.google.protobuf:protoc:${bt4kVersion("protobuf")}")
            dependency("com.hazelcast:hazelcast-spring:${bt4kVersion("hazelcast")}")
            dependency("com.sksamuel.scrimage:scrimage-filters:${bt4kVersion("scrimage")}")
            dependency("com.sksamuel.scrimage:scrimage-webp:${bt4kVersion("scrimage")}")
            dependency("io.agroal:agroal-api:${bt4kVersion("agroal")}")
            dependency("io.agroal:agroal-narayana:${bt4kVersion("agroal")}")
            dependency("io.agroal:agroal-spring-boot-starter:${bt4kVersion("agroal")}")
            dependency("io.github.benas:random-beans:${bt4kVersion("random-beans")}")
            dependency("io.github.openfeign.querydsl:querydsl-kotlin:${bt4kVersion("querydsl")}")
            dependency("io.github.openfeign.querydsl:querydsl-kotlin-codegen:${bt4kVersion("querydsl")}")
            dependency("io.ktor:ktor-client-cio:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-client-content-negotiation:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-client-core:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-client-mock:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-serialization-kotlinx-json:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-call-id:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-call-logging:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-cio:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-content-negotiation:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-core:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-metrics-micrometer:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-openapi:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-routing-openapi:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-status-pages:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-swagger:${bt4kVersion("ktor")}")
            dependency("io.ktor:ktor-server-test-host:${bt4kVersion("ktor")}")
            dependency("io.lettuce:lettuce-core:${bt4kVersion("lettuce")}")
            dependency("io.netty:netty-all:${bt4kVersion("netty")}")
            dependency("io.netty:netty-buffer:${bt4kVersion("netty")}")
            dependency("io.netty:netty-codec:${bt4kVersion("netty")}")
            dependency("io.netty:netty-codec-dns:${bt4kVersion("netty")}")
            dependency("io.netty:netty-codec-protobuf:${bt4kVersion("netty")}")
            dependency("io.netty:netty-common:${bt4kVersion("netty")}")
            dependency("io.netty:netty-handler:${bt4kVersion("netty")}")
            dependency("io.netty:netty-handler-proxy:${bt4kVersion("netty")}")
            dependency("io.netty:netty-resolver:${bt4kVersion("netty")}")
            dependency("io.netty:netty-resolver-dns:${bt4kVersion("netty")}")
            dependency("io.netty:netty-resolver-dns-classes-macos:${bt4kVersion("netty")}")
            dependency("io.netty:netty-resolver-dns-native-macos:${bt4kVersion("netty")}")
            dependency("io.netty:netty-transport:${bt4kVersion("netty")}")
            dependency("io.netty:netty-transport-classes-epoll:${bt4kVersion("netty")}")
            dependency("io.netty:netty-transport-classes-kqueue:${bt4kVersion("netty")}")
            dependency("io.netty:netty-transport-native-epoll:${bt4kVersion("netty")}")
            dependency("io.netty:netty-transport-native-kqueue:${bt4kVersion("netty")}")
            dependency("io.vertx:vertx-jdbc-client:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-junit5:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-lang-kotlin:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-lang-kotlin-coroutines:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-mysql-client:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-pg-client:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-sql-client:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-sql-client-templates:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-web:${bt4kVersion("vertx")}")
            dependency("io.vertx:vertx-web-client:${bt4kVersion("vertx")}")
            dependency("org.apache.avro:avro-ipc:${bt4kVersion("avro")}")
            dependency("org.apache.avro:avro-ipc-netty:${bt4kVersion("avro")}")
            dependency("org.apache.avro:avro-protobuf:${bt4kVersion("avro")}")
            dependency("org.apache.httpcomponents.client5:httpclient5-cache:${bt4kVersion("httpclient5")}")
            dependency("org.apache.httpcomponents.client5:httpclient5-fluent:${bt4kVersion("httpclient5")}")
            dependency("org.apache.httpcomponents.client5:httpclient5-testing:${bt4kVersion("httpclient5")}")
            dependency("org.apache.httpcomponents.core5:httpcore5-testing:${bt4kVersion("httpcore5")}")
            dependency("org.apache.ignite:ignite-aop:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-aws:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-clients:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-compress:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-indexing:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-slf4j:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-spring:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-tools:${bt4kVersion("ignite")}")
            dependency("org.apache.ignite:ignite-zookeeper:${bt4kVersion("ignite")}")
            dependency("org.apache.kafka:kafka-server:${bt4kVersion("kafka3")}")
            dependency("org.apache.logging.log4j:log4j-api:${bt4kVersion("log4j")}")
            dependency("org.apache.logging.log4j:log4j-jcl:${bt4kVersion("log4j")}")
            dependency("org.apache.logging.log4j:log4j-jul:${bt4kVersion("log4j")}")
            dependency("org.apache.logging.log4j:log4j-slf4j-impl:${bt4kVersion("log4j")}")
            dependency("org.apache.logging.log4j:log4j-web:${bt4kVersion("log4j")}")
            dependency("org.assertj:assertj-core:${bt4kVersion("assertj-core")}")
            dependency("org.awaitility:awaitility-kotlin:${bt4kVersion("awaitility")}")
            dependency("org.hibernate.orm:hibernate-envers:${bt4kVersion("hibernate")}")
            dependency("org.hibernate.orm:hibernate-hikaricp:${bt4kVersion("hibernate")}")
            dependency("org.hibernate.orm:hibernate-jpamodelgen:${bt4kVersion("hibernate")}")
            dependency("org.hibernate.orm:hibernate-micrometer:${bt4kVersion("hibernate")}")
            dependency("org.hibernate.orm:hibernate-spatial:${bt4kVersion("hibernate")}")
            dependency("org.hibernate.orm:hibernate-testing:${bt4kVersion("hibernate")}")
            dependency("org.jetbrains.exposed:exposed-bom:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-crypt:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-dao:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-json:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-kotlin-datetime:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-migration-core:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-migration-r2dbc:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:exposed-money:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.exposed:spring-transaction:${bt4kVersion("exposed")}")
            dependency("org.jetbrains.kotlin:kotlin-bom:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-compiler:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-compiler-embeddable:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-daemon-client:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-reflect:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-script-runtime:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-common:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-dependencies:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-jsr223:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-jvm:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib-common:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-test:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-test-common:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlin:kotlin-test-junit5:${bt4kVersion("kotlin")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${bt4kVersion("kotlinx-serialization")}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-properties:${bt4kVersion("kotlinx-serialization")}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${bt4kVersion("kotlinx-serialization")}")
            dependency("org.ow2.asm:asm-commons:${bt4kVersion("ow2-asm")}")
            dependency("org.ow2.asm:asm-tree:${bt4kVersion("ow2-asm")}")
            dependency("org.ow2.asm:asm-util:${bt4kVersion("ow2-asm")}")
            dependency("org.redisson:redisson-spring-boot-starter:${bt4kVersion("redisson")}")
            dependency("org.redisson:redisson-spring-data-34:${bt4kVersion("redisson")}")
            dependency("org.redisson:redisson-spring-data-35:${bt4kVersion("redisson")}")
            dependency("org.redisson:redisson-spring-data-40:${bt4kVersion("redisson")}")
            dependency("org.slf4j:jcl-over-slf4j:${bt4kVersion("slf4j")}")
            dependency("org.slf4j:jul-to-slf4j:${bt4kVersion("slf4j")}")
            dependency("org.slf4j:log4j-over-slf4j:${bt4kVersion("slf4j")}")
            dependency("org.slf4j:slf4j-simple:${bt4kVersion("slf4j")}")
            dependency("org.springdoc:springdoc-openapi-starter-webflux-ui:${bt4kVersion("springdoc-openapi")}")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-api:${bt4kVersion("springdoc-openapi")}")
            dependency("org.springframework.boot:spring-boot-dependencies:${bt4kVersion("spring-boot")}")
            dependency("org.testcontainers:testcontainers:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-bom:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-cassandra:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-chromadb:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-clickhouse:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-cockroachdb:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-elasticsearch:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-gcloud:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-influxdb:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-junit-jupiter:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-k3s:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-kafka:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-localstack:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-mariadb:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-minio:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-mockserver:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-mongodb:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-mysql:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-neo4j:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-nginx:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-ollama:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-oracle-xe:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-postgresql:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-pulsar:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-r2dbc:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-rabbitmq:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-redpanda:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-toxiproxy:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-trino:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-vault:${bt4kVersion("testcontainers")}")
            dependency("org.testcontainers:testcontainers-weaviate:${bt4kVersion("testcontainers")}")
            dependency("software.amazon.awssdk:apache-client:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:applicationautoscaling:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:auth:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:aws-core:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:aws-crt-client:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:cloudwatch:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:cloudwatchevents:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:cloudwatchlogs:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:dynamodb-enhanced:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:ec2:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:elasticache:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:kafka:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:kinesis:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:kms:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:lambda:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:netty-nio-client:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:s3:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:s3-transfer-manager:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:sdk-core:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:ses:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:sesv2:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:sns:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:sqs:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:sts:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:test-utils:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:url-connection-client:${bt4kVersion("aws2")}")
            dependency("software.amazon.awssdk:utils:${bt4kVersion("aws2")}")
            dependency("tools.jackson.core:jackson-core:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.core:jackson-databind:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-avro:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-cbor:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-csv:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-ion:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-properties:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-protobuf:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-smile:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-toml:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.dataformat:jackson-dataformat-yaml:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-eclipse-collections:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-guava:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-javax-money:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-json-org:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-jsr353:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.datatype:jackson-datatype-moneta:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.module:jackson-module-blackbird:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.module:jackson-module-kotlin:${bt4kVersion("jackson3")}")
            dependency("tools.jackson.module:jackson-module-no-ctor-deser:${bt4kVersion("jackson3")}")
            dependency("tools.jackson:jackson-bom:${bt4kVersion("jackson3")}")
            // </central-catalog-local-aliases>
            dependency(rootBt4k.jetbrains.annotations.get().toString())

            // Kotlinx Coroutines (mavenBom 이 적용이 안되어서 추가로 명시했습니다)
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:${bt4kVersion("kotlinx-coroutines")}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:${bt4kVersion("kotlinx-coroutines")}")

            // Apache Commons
            dependency(rootBt4k.commons.beanutils.get().toString())
            dependency(rootBt4k.commons.collections4.get().toString())
            dependency(bt4kLibrary("commons-compress").get().toString())
            dependency(bt4kLibrary("commons-codec").get().toString())
            dependency(bt4kLibrary("commons-csv").get().toString())
            dependency(bt4kLibrary("commons-lang3").get().toString())
            dependency(bt4kLibrary("commons-logging").get().toString())
            dependency(rootBt4k.commons.math3.get().toString())
            dependency(bt4kLibrary("commons-pool2").get().toString())
            dependency(rootBt4k.commons.text.get().toString())
            dependency(bt4kLibrary("commons-exec").get().toString())
            dependency(bt4kLibrary("commons-io").get().toString())

            dependency(bt4kLibrary("slf4j-api").get().toString())
            dependency("org.slf4j:jcl-over-slf4j:${bt4kVersion("slf4j")}")
            dependency("org.slf4j:jul-to-slf4j:${bt4kVersion("slf4j")}")
            dependency("org.slf4j:log4j-over-slf4j:${bt4kVersion("slf4j")}")
            dependency(rootBt4k.logback.asProvider().get().toString())
            dependency(rootBt4k.logback.core.get().toString())

            // jakarta
            dependency(bt4kLibrary("jakarta-activation-api").get().toString())
            dependency(rootBt4k.jakarta.annotation.api.get().toString())
            dependency(rootBt4k.jakarta.el.api.get().toString())
            dependency(rootBt4k.jakarta.inject.api.get().toString())
            dependency(rootBt4k.jakarta.interceptor.api.get().toString())
            dependency(rootBt4k.jakarta.jms.api.get().toString())
            dependency(rootBt4k.jakarta.json.api.get().toString())
            dependency(rootBt4k.jakarta.json.asProvider().get().toString())
            dependency(rootBt4k.jakarta.persistence.v31.get().toString())
            dependency(rootBt4k.jakarta.servlet.api.get().toString())
            dependency(rootBt4k.jakarta.transaction.api.get().toString())
            dependency(rootBt4k.jakarta.validation.api.get().toString())
            dependency(rootBt4k.jakarta.ws.rs.api.get().toString())
            dependency(bt4kLibrary("jakarta-xml-bind").get().toString())

            // Compressor
            dependency(rootBt4k.snappy.java.get().toString())
            dependency(rootBt4k.at.yawk.lz4.java.get().toString())
            dependency(bt4kLibrary("zstd-jni").get().toString())

            // Netty tcnative uses its own 2.0.x line; Spring Boot 4.1 currently pushes it onto Netty core 4.2.x.
            dependency(rootBt4k.netty.tcnative.classes.get().toString())

            // Shared security overrides are sourced from the imported bluetape4k-dependencies catalog.
            dependency("org.apache.tomcat.embed:tomcat-embed-core:${bt4kVersion("tomcat")}")
            dependency("io.opentelemetry:opentelemetry-api:${bt4kVersion("opentelemetry")}")
            dependency("io.opentelemetry:opentelemetry-extension-trace-propagators:${bt4kVersion("opentelemetry")}")

            // Java Money
            dependency(rootBt4k.javax.money.api.get().toString())
            dependency(bt4kLibrary("javamoney-moneta").get().toString())

            dependency(rootBt4k.findbugs.get().toString())
            dependency(bt4kLibrary("guava").get().toString())

            dependency(rootBt4k.kryo.get().toString())
            dependency("org.apache.fory:fory-kotlin:${bt4kVersion("fory-kotlin")}")

            // HINT: Jackson (이상하게 mavenBom 에 적용이 안되어서 강제로 추가하였다)
            dependency("com.fasterxml.jackson:jackson-bom:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.core:jackson-annotations:${bt4kVersion("jackson-annotations")}")
            dependency("com.fasterxml.jackson.core:jackson-core:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.core:jackson-databind:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr353:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-kotlin:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-parameter:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-parameter-names:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-blackbird:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.module:jackson-module-jsonSchema:${bt4kVersion("jackson")}")

            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-avro:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-ion:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-protobuf:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:${bt4kVersion("jackson")}")
            dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${bt4kVersion("jackson")}")

            // Retrofit
            dependency(rootBt4k.retrofit2.asProvider().get().toString())
            dependency(rootBt4k.retrofit2.adapter.java8.get().toString())
            dependency(rootBt4k.retrofit2.adapter.reactor.get().toString())
            dependency(rootBt4k.retrofit2.adapter.rxjava2.get().toString())
            dependency(rootBt4k.retrofit2.converter.jackson.get().toString())
            dependency(rootBt4k.retrofit2.converter.moshi.get().toString())
            dependency(rootBt4k.retrofit2.converter.protobuf.get().toString())
            dependency(rootBt4k.retrofit2.converter.scalars.get().toString())
            dependency(rootBt4k.retrofit2.mock.get().toString())


            dependency(bt4kLibrary("httpclient5").get().toString())

            dependency(rootBt4k.grpc.kotlin.stub.get().toString())

            dependency(rootBt4k.mongo.bson.asProvider().get().toString())
            dependency(rootBt4k.mongo.bson.kotlin.get().toString())
            dependency(rootBt4k.mongo.bson.kotlinx.get().toString())
            dependency(rootBt4k.mongodb.driver.core.get().toString())
            dependency(rootBt4k.mongodb.driver.reactivestreams.get().toString())
            dependency(rootBt4k.mongodb.driver.sync.get().toString())
            dependency(rootBt4k.mongodb.driver.kotlin.coroutine.get().toString())
            dependency(rootBt4k.mongodb.driver.kotlin.extensions.get().toString())
            dependency(rootBt4k.mongodb.driver.kotlin.sync.get().toString())

            // Kafka
            dependency(bt4kLibrary("kafka-clients").get().toString())
            dependency(bt4kLibrary("kafka-generator").get().toString())
            dependency(bt4kLibrary("kafka-metadata").get().toString())
            dependency(bt4kLibrary("kafka-raft").get().toString())
            dependency(bt4kLibrary("kafka-server-common").get().toString())
            dependency(bt4kLibrary("kafka-storage").get().toString())
            dependency(bt4kLibrary("kafka-storage-api").get().toString())
            dependency(bt4kLibrary("kafka-streams").get().toString())
            dependency(bt4kLibrary("kafka-streams-test-utils").get().toString())
            dependency(bt4kLibrary("kafka-scala213").get().toString())

            // Hibernate
            dependency(bt4kLibrary("hibernate-core").get().toString())
            dependency(bt4kLibrary("hibernate-jcache").get().toString())
            dependency(rootBt4k.javassist.get().toString())

            dependency(rootBt4k.antlr4.runtime.get().toString())  // https://github.com/spring-projects/spring-data-jpa/issues/3262
            dependency(rootBt4k.antlr4.tool.get().toString())

            dependency(bt4kLibrary("querydsl-apt").get().toString())
            dependency(bt4kLibrary("querydsl-core").get().toString())
            dependency(bt4kLibrary("querydsl-jpa").get().toString())

            // Validators
            dependency(rootBt4k.jakarta.el.api.get().toString())
            dependency(rootBt4k.jakarta.validation.api.get().toString())
            dependency(rootBt4k.hibernate.validator.asProvider().get().toString())
            dependency(rootBt4k.hibernate.validator.annotation.processor.get().toString())

            dependency(bt4kLibrary("hikaricp").get().toString())
            dependency(bt4kLibrary("mysql-connector-j").get().toString())
            dependency(rootBt4k.mariadb.java.client.get().toString())

            dependency(rootBt4k.caffeine.asProvider().get().toString())
            dependency(rootBt4k.caffeine.jcache.get().toString())

            dependency(rootBt4k.objenesis.get().toString())
            dependency(bt4kLibrary("ow2-asm").get().toString())

            dependency("com.esotericsoftware:reflectasm:${bt4kVersion("reflectasm")}")

            dependency(rootBt4k.junit.bom.get().toString())
            dependency(rootBt4k.junit.jupiter.asProvider().get().toString())
            dependency(rootBt4k.junit.jupiter.api.get().toString())
            dependency(rootBt4k.junit.jupiter.engine.get().toString())
            dependency(rootBt4k.junit.jupiter.migrationsupport.get().toString())
            dependency(rootBt4k.junit.jupiter.params.get().toString())
            dependency(rootBt4k.junit.platform.commons.get().toString())
            dependency(rootBt4k.junit.platform.engine.get().toString())
            dependency(rootBt4k.junit.platform.launcher.get().toString())

            dependency("org.assertj:assertj-core:${bt4kVersion("assertj-core")}")

            dependency(rootBt4k.mockk.get().toString())
            dependency(rootBt4k.datafaker.get().toString())
            dependency("io.github.benas:random-beans:${bt4kVersion("random-beans")}")

            dependency(rootBt4k.jsonpath.v3.get().toString())
            dependency(rootBt4k.jsonassert.v1.get().toString())

            dependency(bt4kLibrary("bouncycastle-bcpkix").get().toString())
            dependency(bt4kLibrary("bouncycastle-bcprov").get().toString())

            // Prometheus
            dependency(rootBt4k.prometheus.simpleclient.asProvider().get().toString())
            dependency(rootBt4k.prometheus.simpleclient.common.get().toString())
            dependency(rootBt4k.prometheus.simpleclient.httpserver.get().toString())
            dependency(rootBt4k.prometheus.simpleclient.pushgateway.get().toString())
            dependency(rootBt4k.prometheus.simpleclient.spring.boot.get().toString())
            dependency(rootBt4k.prometheus.simpleclient.tracer.common.get().toString())
            dependency(rootBt4k.prometheus.simpleclient.tracer.otel.asProvider().get().toString())
            dependency(rootBt4k.prometheus.simpleclient.tracer.otel.agent.get().toString())

            // OW2 ASM
            dependency(bt4kLibrary("ow2-asm").get().toString())
            dependency("org.ow2.asm:asm-commons:${bt4kVersion("ow2-asm")}")
            dependency("org.ow2.asm:asm-util:${bt4kVersion("ow2-asm")}")
            dependency("org.ow2.asm:asm-tree:${bt4kVersion("ow2-asm")}")

            dependency(rootBt4k.snakeyaml.get().toString())
            dependency(rootBt4k.jna.asProvider().get().toString())

            // ByteBuddy
            dependency(rootBt4k.byte.buddy.asProvider().get().toString())
            dependency(rootBt4k.byte.buddy.agent.get().toString())
        }
    }

    if (path == ":bluetape4k-testcontainers") {
        dependencyManagement {
            imports {
                mavenBom("io.netty:netty-bom:${bt4kVersion("netty4")}")
                mavenBom(bt4kLibrary("vertx4-dependencies").get().toString())
            }
            dependencies {
                dependency("io.vertx:vertx-core:${bt4kVersion("vertx4")}")
                dependency("io.vertx:vertx-web-client:${bt4kVersion("vertx4")}")
            }
        }
    }

    dependencies {
        val api by configurations
        val implementation by configurations
        val testImplementation by configurations

        val compileOnly by configurations
        val testCompileOnly by configurations
        val testRuntimeOnly by configurations

        api(rootBt4k.jetbrains.annotations)

        implementation(rootLibs.kotlin.stdlib)
        implementation(rootLibs.kotlin.reflect)
        testImplementation(rootLibs.kotlin.test)
        testImplementation(rootLibs.kotlin.test.junit5)

        implementation(rootLibs.kotlinx.coroutines.core)
        implementation(rootBt4k.kotlinx.atomicfu)
        implementation(platform(bt4kLibrary("reactor-bom").get()))

        // 개발 시에는 logback 이 검증하기에 더 좋고, Production에서 비동기 로깅은 log4j2 가 성능이 좋다고 합니다.
        api(bt4kLibrary("slf4j-api").get())
        testImplementation(rootBt4k.logback.asProvider())
        testImplementation(rootLibs.jcl.over.slf4j)
        testImplementation(rootLibs.jul.to.slf4j)
        testImplementation(rootLibs.log4j.over.slf4j)

        testImplementation(rootBt4k.junit.jupiter.asProvider())
        testRuntimeOnly(rootBt4k.junit.platform.engine)
        testImplementation(rootBt4k.junit.jupiter.migrationsupport)

        testImplementation(rootLibs.awaitility.kotlin)
        testImplementation(rootBt4k.mockk)

        // Property based test
        testImplementation(rootBt4k.datafaker)
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
        $ ./gradlew nmcpPublishAggregationToCentralPortal
        ```
     */
    publishing {
        publications {
            if (project.isPublishableLibraryProject()) {
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

                    versionMapping {
                        usage("java-api") {
                            fromResolutionOf("runtimeClasspath")
                        }
                        usage("java-runtime") {
                            fromResolutionResult()
                        }
                    }

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
        enabled = project.isPublishableLibraryProject(),
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

val detektTargetProjects = subprojects
    .filter(Project::isDetektTargetProject)
    .sortedBy(Project::getPath)

val detektSourceCoverageReport = layout.buildDirectory.file("reports/detekt/source-coverage.md")
val detektSourceFilesByProject = detektTargetProjects.associateWith { it.detektKotlinSourceFiles() }

val detektSourceCoverage by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies and reports the Kotlin source scope analyzed by Detekt."

    inputs.files(detektSourceFilesByProject.values)
    outputs.file(detektSourceCoverageReport)

    doLast {
        val rows = detektSourceFilesByProject.map { (module, sourceFiles) ->
            val files = sourceFiles.files
            val mainRoot = module.projectDir.toPath().resolve("src/main/kotlin")
            val testRoot = module.projectDir.toPath().resolve("src/test/kotlin")
            val mainCount = files.count { it.toPath().startsWith(mainRoot) }
            val testCount = files.count { it.toPath().startsWith(testRoot) }
            module to Triple(mainCount, testCount, files.size)
        }
        val emptyModules = rows.filter { (_, counts) -> counts.third == 0 }
        val totalMain = rows.sumOf { it.second.first }
        val totalTest = rows.sumOf { it.second.second }
        val totalFiles = rows.sumOf { it.second.third }
        val exclusions = (
            subprojects
                .mapNotNull { module -> module.detektExclusionReason()?.let { module.path to it } } +
                detektExplicitExclusionReasons
                    .filterKeys { moduleName -> subprojects.none { it.name == moduleName } }
                    .map { (moduleName, reason) -> ":$moduleName" to reason }
            )
            .sortedBy { it.first }

        val report = detektSourceCoverageReport.get().asFile
        report.parentFile.mkdirs()
        report.writeText(buildString {
            appendLine("# Detekt source coverage")
            appendLine()
            appendLine("- Included modules: ${rows.size}")
            appendLine("- Kotlin source files: $totalFiles (main: $totalMain, test: $totalTest)")
            appendLine("- Empty included modules: ${emptyModules.size}")
            appendLine()
            appendLine("## Included modules")
            appendLine()
            appendLine("| Project | Main Kotlin | Test Kotlin | Total |")
            appendLine("| --- | ---: | ---: | ---: |")
            rows.forEach { (module, counts) ->
                appendLine("| `${module.path}` | ${counts.first} | ${counts.second} | ${counts.third} |")
            }
            appendLine()
            appendLine("## Explicit exclusions")
            appendLine()
            if (exclusions.isEmpty()) {
                appendLine("No explicit exclusions.")
            } else {
                exclusions.forEach { (modulePath, reason) ->
                    appendLine("- `$modulePath` — $reason")
                }
            }
        })

        check(emptyModules.isEmpty()) {
            "Detekt source coverage is empty for included modules: ${emptyModules.joinToString { it.first.path }}"
        }
        logger.lifecycle(
            "Detekt source coverage: ${rows.size} modules, $totalFiles Kotlin files " +
                "(main: $totalMain, test: $totalTest)",
        )
    }
}

val detektModuleTasks = detektTargetProjects.map { module ->
    module.tasks.named<Detekt>("detekt")
}

val detektReportMerge by tasks.registering(ReportMergeTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Merges XML reports from all Detekt Kotlin subprojects."
    output.set(layout.buildDirectory.file("reports/detekt/merged.xml"))
    dependsOn(detektSourceCoverage)
    detektModuleTasks.forEach { moduleTask ->
        dependsOn(moduleTask)
        input.from(moduleTask.flatMap { it.reports.checkstyle.outputLocation })
    }
}

detektModuleTasks.forEach { moduleTask ->
    moduleTask.configure {
        mustRunAfter(detektSourceCoverage)
    }
}

tasks.named<Detekt>("detekt") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Analyzes all intended Kotlin library subprojects with Detekt."
    dependsOn(detektSourceCoverage)
    dependsOn(detektReportMerge)
    // The root project is an orchestration boundary; analysis runs in module tasks.
    onlyIf { false }
}

val publishedProjects = subprojects.filter(Project::isPublishedProject)

extensions.configure<NmcpAggregationExtension>("nmcpAggregation") {
    centralPortal {
        username.set(centralUser)
        password.set(centralPassword)
        publishingType.set("AUTOMATIC")
        uploadSnapshotsParallelism.set(centralSnapshotsParallelism)
    }
}

dependencies {
    publishedProjects.forEach { publishedProject ->
        add("nmcpAggregation", project(publishedProject.path))
    }
}

val manualModuleInventory = layout.buildDirectory.file("manual/module-inventory.json")

tasks.register("exportManualModuleInventory") {
    group = "documentation"
    description = "Exports the registered Gradle module inventory for manual generation."

    val repositoryRoot = project.rootDir.toPath()
    val modules = project.subprojects
        .sortedBy(Project::getPath)
        .map { module ->
            val sourceDir = repositoryRoot
                .relativize(module.projectDir.toPath())
                .toString()
                .replace(File.separatorChar, '/')
            val kind = when {
                sourceDir.startsWith("examples/") -> "example"
                sourceDir.startsWith("benchmark/") -> "benchmark"
                else -> "library"
            }

            linkedMapOf(
                "gradlePath" to module.path,
                "projectName" to module.name,
                "sourceDir" to sourceDir,
                "kind" to kind,
            )
        }
    val inventoryJson = JsonOutput.prettyPrint(JsonOutput.toJson(modules)) + "\n"
    val inventoryFile = manualModuleInventory.get().asFile

    inputs.property("inventoryJson", inventoryJson)
    outputs.file(inventoryFile)

    doLast {
        inventoryFile.parentFile.mkdirs()
        inventoryFile.writeText(inventoryJson)
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
                    !sub.isSampleOrBenchmarkProject()
        }
        .forEach { sub -> kover(project(sub.path)) }
}

// ── Disabled Test Release Gate ──────────────────────────────────────────────
val checkDisabledTests by tasks.registering(DisabledTestReportTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Scans JUnit disabled tests and fails known-bug skips without GitHub issue references."
    sourceRoot.set(layout.projectDirectory)
    sourceFiles.from(
        fileTree(layout.projectDirectory) {
            include("**/src/test/**/*.kt", "**/src/test/**/*.java")
            exclude("**/build/**", ".gradle/**", ".git/**", ".worktrees/**", "buildSrc/**")
        },
    )
    reportFile.set(layout.buildDirectory.file("reports/disabled-tests/disabled-tests.md"))
}

tasks.named("check") {
    dependsOn(checkDisabledTests)
}
