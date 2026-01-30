import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    base
    `maven-publish`
    // jacoco
    kotlin("jvm") version Versions.kotlin

    // see: https://kotlinlang.org/docs/reference/compiler-plugins.html
    kotlin("plugin.spring") version Versions.kotlin apply false
    kotlin("plugin.allopen") version Versions.kotlin apply false
    kotlin("plugin.noarg") version Versions.kotlin apply false
    kotlin("plugin.jpa") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    kotlin("kapt") version Versions.kotlin apply false
    id("org.jetbrains.kotlinx.atomicfu") version Versions.kotlinx_atomicfu
    id(Plugins.kotlinx_benchmark) version Plugins.Versions.kotlinx_benchmark apply false

    id(Plugins.detekt) version Plugins.Versions.detekt

    id(Plugins.dependency_management) version Plugins.Versions.dependency_management
    id(Plugins.spring_boot) version Plugins.Versions.spring_boot apply false

    id(Plugins.quarkus) version Plugins.Versions.quarkus apply false

    id(Plugins.dokka) version Plugins.Versions.dokka
    id(Plugins.testLogger) version Plugins.Versions.testLogger
    id(Plugins.shadow) version Plugins.Versions.shadow apply false

    id(Plugins.graalvm_native) version Plugins.Versions.graalvm_native apply false
    id(Plugins.kosogor) version Plugins.Versions.kosogor
}

// NOTE: Nexus 에 등록된 것 때문에 사용한다
// NOTE: .zshrc 에 정의하던가, ~/.gradle/gradle.properties 에 정의해주셔야 합니다.
fun getEnvOrProjectProperty(propertyKey: String, envKey: String): String {
    return project.findProperty(propertyKey) as? String ?: System.getenv()[envKey] ?: ""
}

val bluetape4kGprUser: String = getEnvOrProjectProperty("gpr.user", "BLUETAPE4K_GITHUB_USERNAME")
val bluetape4kGprKey: String = getEnvOrProjectProperty("gpr.key", "BLUETAPE4K_GITHUB_TOKEN")
val bluetape4kGprPublishKey: String = getEnvOrProjectProperty("gpr.publish.key", "BLUETAPE4K_GITHUB_PUBLISH_TOKEN")

val projectGroup: String by project
val baseVersion: String by project
val snapshotVersion: String by project

allprojects {
    group = projectGroup
    version = baseVersion + snapshotVersion

    repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven {
            name = "bluetape4k"
            url = uri("https://maven.pkg.github.com/bluetape4k/bluetape4k-projects")
            credentials {
                username = bluetape4kGprUser
                password = bluetape4kGprKey
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

        // plugin("jacoco")
        plugin("maven-publish")

        plugin(Plugins.dependency_management)

        plugin(Plugins.dokka)
        plugin(Plugins.testLogger)
        plugin(Plugins.kosogor)
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_3)
            apiVersion.set(KotlinVersion.KOTLIN_2_3)
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-jvm-default=enable",
                "-Xinline-classes",
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

        @Suppress("OPT_IN_USAGE")
        kotlinDaemonJvmArgs = listOf(
            "-Xmx2G",
            "-XX:MaxMetaspaceSize=512m",
            "-XX:+UseZGC",
            "-XX:+UseStringDeduplication",
            "-XX:+EnableDynamicAgentLoading"
        )
    }

    atomicfu {
        transformJvm = true
        jvmVariant = "VH"     //  FU, VH, BOTH
    }

    tasks {
        compileJava {
            options.isIncremental = true
        }

        test {
            useJUnitPlatform()

            // 테스트 시 아래와 같은 예외 메시지를 제거하기 위해서 
            // OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
            jvmArgs(
                "-Xshare:off",
                "-Xmx8G",
                "-XX:+UseZGC",
                "-XX:-MaxFDLimit",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableDynamicAgentLoading",
            )

            if (project.name.contains("quarkus")) {
                // [Quarkus Logging](https://quarkus.io/guides/logging)
                systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
            }

            if (project.name.contains("quarkus")) {
                // [Quarkus Logging](https://quarkus.io/guides/logging)
                systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
            }

            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true

                events("failed")
            }
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            showFullStackTraces = true
        }

        val reportMerge by registering(ReportMergeTask::class) {
            val file = rootProject.layout.buildDirectory.asFile.get().resolve("reports/detekt/exposed.xml")
            output.set(file)
            // output.set(rootProject.buildDir.resolve("reports/detekt/exposed.xml"))
        }
        withType<Detekt>().configureEach detekt@{
            enabled = this@subprojects.name !== "exposed-tests"
            finalizedBy(reportMerge)
            reportMerge.configure {
                input.from(this@detekt.xmlReportFile)
            }
        }

//        jacoco {
//            toolVersion = Plugins.Versions.jacoco
//        }
//
//        jacocoTestReport {
//            reports {
//                html.required.set(true)
//                xml.required.set(true)
//            }
//        }
//
//        jacocoTestCoverageVerification {
//            dependsOn(jacocoTestReport)
//
//            violationRules {
//                rule {
//                    // 룰 검증 수행 여부
//                    enabled = true
//
//                    // 룰을 검증할 단위를 클래스 단위로 한다
//                    element = "CLASS"         // BUNDLE|PACKAGE|CLASS|SOURCEFILE|METHOD
//
//                    // 브랜치 커버리지를 최소한 10% 를 만족시켜야 한다
//                    limit {
//                        counter =
//                            "INSTRUCTION"       // INSTRUCTION, LINE, BRANCH, COMPLEXITY, METHOD and CLASS. Defaults to INSTRUCTION.
//                        value =
//                            "COVEREDRATIO"   // TOTALCOUNT, MISSEDCOUNT, COVEREDCOUNT, MISSEDRATIO and COVEREDRATIO. Defaults to COVEREDRATIO
//                        minimum = 0.10.toBigDecimal()
//                    }
//                }
//            }
//        }
//
//        jacocoTestCoverageVerification {
//            dependsOn(jacocoTestReport)
//
//            violationRules {
//                rule {
//                    // 룰 검증 수행 여부
//                    enabled = true
//
//                    // 룰을 검증할 단위를 클래스 단위로 한다
//                    element = "CLASS"         // BUNDLE|PACKAGE|CLASS|SOURCEFILE|METHOD
//
//                    // 브랜치 커버리지를 최소한 10% 를 만족시켜야 한다
//                    limit {
//                        counter =
//                            "INSTRUCTION"       // INSTRUCTION, LINE, BRANCH, COMPLEXITY, METHOD and CLASS. Defaults to INSTRUCTION.
//                        value =
//                            "COVEREDRATIO"   // TOTALCOUNT, MISSEDCOUNT, COVEREDCOUNT, MISSEDRATIO and COVEREDRATIO. Defaults to COVEREDRATIO
//                        minimum = 0.10.toBigDecimal()
//                    }
//                }
//            }
//        }

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
            mavenBom(Libs.spring_integration_bom)
            mavenBom(Libs.spring_cloud_dependencies)
            mavenBom(Libs.spring_boot_dependencies)

            mavenBom(Libs.feign_bom)
            mavenBom(Libs.micrometer_bom)
            mavenBom(Libs.micrometer_tracing_bom)
            mavenBom(Libs.opentelemetry_bom)
            mavenBom(Libs.opentelemetry_alpha_bom)
            mavenBom(Libs.opentelemetry_instrumentation_bom_alpha)
            mavenBom(Libs.log4j_bom)
            mavenBom(Libs.testcontainers_bom)
            mavenBom(Libs.junit_bom)
            mavenBom(Libs.aws_bom)
            mavenBom(Libs.aws2_bom)
            mavenBom(Libs.okhttp3_bom)
            mavenBom(Libs.grpc_bom)
            mavenBom(Libs.protobuf_bom)
            mavenBom(Libs.metrics_bom)
            mavenBom(Libs.fabric8_kubernetes_client_bom)
            mavenBom(Libs.resilience4j_bom)
            mavenBom(Libs.netty_bom)
            mavenBom(Libs.jackson_bom)
            mavenBom(Libs.pods4k_bom)

            mavenBom(Libs.kotlinx_coroutines_bom)
            mavenBom(Libs.kotlin_bom)
        }
        dependencies {
            dependency(Libs.jetbrains_annotations)

            // Kotlinx Coroutines (mavenBom 이 적용이 안되어서 추가로 명시했습니다)
            dependency(Libs.kotlinx_coroutines_bom)
            dependency(Libs.kotlinx_coroutines_core)
            dependency(Libs.kotlinx_coroutines_core_jvm)
            dependency(Libs.kotlinx_coroutines_reactive)
            dependency(Libs.kotlinx_coroutines_reactor)
            dependency(Libs.kotlinx_coroutines_rx2)
            dependency(Libs.kotlinx_coroutines_rx3)
            dependency(Libs.kotlinx_coroutines_slf4j)
            dependency(Libs.kotlinx_coroutines_debug)
            dependency(Libs.kotlinx_coroutines_test)
            dependency(Libs.kotlinx_coroutines_test_jvm)

            // Apache Commons
            dependency(Libs.commons_beanutils)
            dependency(Libs.commons_collections4)
            dependency(Libs.commons_compress)
            dependency(Libs.commons_codec)
            dependency(Libs.commons_csv)
            dependency(Libs.commons_lang3)
            dependency(Libs.commons_logging)
            dependency(Libs.commons_math3)
            dependency(Libs.commons_pool2)
            dependency(Libs.commons_text)
            dependency(Libs.commons_exec)
            dependency(Libs.commons_io)

            dependency(Libs.slf4j_api)
            dependency(Libs.jcl_over_slf4j)
            dependency(Libs.jul_to_slf4j)
            dependency(Libs.log4j_over_slf4j)
            dependency(Libs.logback)
            dependency(Libs.logback_core)

            // jakarta
            dependency(Libs.jakarta_activation_api)
            dependency(Libs.jakarta_annotation_api)
            dependency(Libs.jakarta_el_api)
            dependency(Libs.jakarta_inject_api)
            dependency(Libs.jakarta_interceptor_api)
            dependency(Libs.jakarta_jms_api)
            dependency(Libs.jakarta_json_api)
            dependency(Libs.jakarta_json)
            dependency(Libs.jakarta_persistence_api)
            dependency(Libs.jakarta_servlet_api)
            dependency(Libs.jakarta_transaction_api)
            dependency(Libs.jakarta_validation_api)
            dependency(Libs.jakarta_ws_rs_api)
            dependency(Libs.jakarta_xml_bind)

            // Compressor
            dependency(Libs.snappy_java)
            dependency(Libs.lz4_java)
            dependency(Libs.zstd_jni)

            // Java Money
            dependency(Libs.javax_money_api)
            dependency(Libs.javamoney_moneta)

            dependency(Libs.findbugs)
            dependency(Libs.guava)

            dependency(Libs.kryo)

            // Jackson (이상하게 mavenBom 에 적용이 안되어서 강제로 추가하였다)
            dependency(Libs.jackson_bom)
            dependency(Libs.jackson_core)
            dependency(Libs.jackson_databind)
            dependency(Libs.jackson_datatype_jdk8)
            dependency(Libs.jackson_datatype_jsr310)
            dependency(Libs.jackson_datatype_jsr353)
            dependency(Libs.jackson_module_kotlin)
            dependency(Libs.jackson_module_paranamer)
            dependency(Libs.jackson_module_parameter_names)
            dependency(Libs.jackson_module_blackbird)
            dependency(Libs.jackson_module_jsonSchema)

            // Retrofit
            dependency(Libs.retrofit2)
            dependency(Libs.retrofit2_adapter_java8)
            dependency(Libs.retrofit2_adapter_reactor)
            dependency(Libs.retrofit2_adapter_rxjava2)
            dependency(Libs.retrofit2_converter_jackson)
            dependency(Libs.retrofit2_converter_moshi)
            dependency(Libs.retrofit2_converter_protobuf)
            dependency(Libs.retrofit2_converter_scalars)
            dependency(Libs.retrofit2_mock)

            // Http
            dependency(Libs.async_http_client)
            dependency(Libs.async_http_client_extras_retrofit2)
            dependency(Libs.async_http_client_extras_rxjava2)

            dependency(Libs.httpclient5)
            dependency(Libs.httpcore5)
            dependency(Libs.httpcore5_h2)
            dependency(Libs.httpcore5_reactive)

            dependency(Libs.grpc_kotlin_stub)

            dependency(Libs.mongo_bson)
            dependency(Libs.mongo_bson_kotlin)
            dependency(Libs.mongo_bson_kotlinx)
            dependency(Libs.mongodb_driver_core)
            dependency(Libs.mongodb_driver_reactivestreams)
            dependency(Libs.mongodb_driver_sync)
            dependency(Libs.mongodb_driver_kotlin_coroutine)
            dependency(Libs.mongodb_driver_kotlin_extensions)
            dependency(Libs.mongodb_driver_kotlin_sync)

            // Kafka
            dependency(Libs.kafka_clients)
            dependency(Libs.kafka_generator)
            dependency(Libs.kafka_metadata)
            dependency(Libs.kafka_raft)
            dependency(Libs.kafka_server_common)
            dependency(Libs.kafka_storage)
            dependency(Libs.kafka_storage_api)
            dependency(Libs.kafka_streams)
            dependency(Libs.kafka_streams_test_utils)
            dependency(Libs.kafka_2_13)

            // Hibernate
            dependency(Libs.hibernate_core)
            dependency(Libs.hibernate_jcache)
            dependency(Libs.javassist)

            dependency(Libs.antlr4_runtime)  // https://github.com/spring-projects/spring-data-jpa/issues/3262
            dependency(Libs.antlr4_tool)

            dependency(Libs.querydsl_apt)
            dependency(Libs.querydsl_core)
            dependency(Libs.querydsl_jpa)

            // Validators
            dependency(Libs.jakarta_el_api)
            dependency(Libs.jakarta_validation_api)
            dependency(Libs.hibernate_validator)
            dependency(Libs.hibernate_validator_annotation_processor)

            dependency(Libs.hikaricp)
            dependency(Libs.mysql_connector_j)
            dependency(Libs.mariadb_java_client)

            dependency(Libs.caffeine)
            dependency(Libs.caffeine_jcache)

            dependency(Libs.objenesis)
            dependency(Libs.ow2_asm)

            dependency(Libs.reflectasm)

            dependency(Libs.junit_bom)
            dependency(Libs.junit_jupiter)
            dependency(Libs.junit_jupiter_api)
            dependency(Libs.junit_jupiter_engine)
            dependency(Libs.junit_jupiter_migrationsupport)
            dependency(Libs.junit_jupiter_params)
            dependency(Libs.junit_platform_commons)
            dependency(Libs.junit_platform_engine)
            dependency(Libs.junit_platform_launcher)

            dependency(Libs.kluent)
            dependency(Libs.assertj_core)

            dependency(Libs.mockk)
            dependency(Libs.datafaker)
            dependency(Libs.random_beans)

            dependency(Libs.jsonpath)
            dependency(Libs.jsonassert)

            dependency(Libs.bouncycastle_bcpkix)
            dependency(Libs.bouncycastle_bcprov)

            // Prometheus
            dependency(Libs.prometheus_simpleclient)
            dependency(Libs.prometheus_simpleclient_common)
            dependency(Libs.prometheus_simpleclient_httpserver)
            dependency(Libs.prometheus_simpleclient_pushgateway)
            dependency(Libs.prometheus_simpleclient_spring_boot)
            dependency(Libs.prometheus_simpleclient_tracer_common)
            dependency(Libs.prometheus_simpleclient_tracer_otel)
            dependency(Libs.prometheus_simpleclient_tracer_otel_agent)

            // OW2 ASM
            dependency(Libs.ow2_asm)
            dependency(Libs.ow2_asm_commons)
            dependency(Libs.ow2_asm_util)
            dependency(Libs.ow2_asm_tree)

            dependency(Libs.snakeyaml)
            dependency(Libs.jna)

            // ByteBuddy
            dependency(Libs.byte_buddy)
            dependency(Libs.byte_buddy_agent)
        }
    }

    dependencies {
        val api by configurations
        val testApi by configurations
        val implementation by configurations
        val testImplementation by configurations

        val compileOnly by configurations
        val testCompileOnly by configurations
        val testRuntimeOnly by configurations

        api(Libs.jetbrains_annotations)

        implementation(Libs.kotlin_stdlib)
        implementation(Libs.kotlin_reflect)
        testImplementation(Libs.kotlin_test)
        testImplementation(Libs.kotlin_test_junit5)

        implementation(Libs.kotlinx_coroutines_core)
        implementation(Libs.kotlinx_atomicfu)

        // 개발 시에는 logback 이 검증하기에 더 좋고, Production에서 비동기 로깅은 log4j2 가 성능이 좋다고 합니다.
        api(Libs.slf4j_api)
        testImplementation(Libs.logback)
        testImplementation(Libs.jcl_over_slf4j)
        testImplementation(Libs.jul_to_slf4j)
        testImplementation(Libs.log4j_over_slf4j)

        testImplementation(Libs.junit_jupiter)
        testRuntimeOnly(Libs.junit_platform_engine)
        testImplementation(Libs.junit_jupiter_migrationsupport)

        testImplementation(Libs.kluent)
        testImplementation(Libs.awaitility_kotlin)
        testImplementation(Libs.mockk)

        // Property baesd test
        testImplementation(Libs.datafaker)
        testImplementation(Libs.random_beans)
    }

//    tasks.withType<Jar> {
//        manifest.attributes["Specification-Title"] = project.name
//        manifest.attributes["Specification-Version"] = project.version
//        manifest.attributes["Implementation-Title"] = project.name
//        manifest.attributes["Implementation-Version"] = project.version
//        manifest.attributes["Automatic-Module-Name"] = project.name.replace('-', '.')
//        manifest.attributes["Created-By"] =
//            "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})"
//    }

    /*
        1. mavenLocal 에 publish 시에는 ./gradlew publishMavenPublicationToMavenLocalRepository 를 수행
        2. nexus에 publish 시에는 ./gradlew publish -PnexusDeployPassword=디플로이 로 비밀번호를 넣어줘야 배포가 됩니다.

        Release 를 위해서는 아래와 같이 `nexusDeployPassword`에 비밀번호를 넣고, `snapshotVersion`에 아무 것도 지정하지 않으면
        nexus server의 releases 에 등록됩니다.
        
        ```bash
        $ ./gradlew clean build
        $ ./gradlew publish -PnexusDeployPassword=elvmffhdl -PsnapshotVersion=
        ```
     */
    publishing {
        publications {
            if (!project.path.contains("workshop") && !project.path.contains("examples") && !project.path.contains("-demo")) {
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
                        name.set("bluetape4k")
                        description.set("Common Library for Kotlin")
                        url.set("https://github.com/bluetape4k/bluetape4k-projects")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("debop")
                                name.set("Sunghyouk Bae")
                                email.set("sunghyouk.bae@gmail.com")
                            }
                        }
                        scm {
                            url.set("https://www.github.com/bluetape4k/bluetape4k-projects")
                            connection.set("scm:git:https://www.github.com/bluetape4k/bluetape4k-projects")
                            developerConnection.set("scm:git:https://www.github.com/bluetape4k/bluetape4k-projects")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                name = "Bluetape4k"
                url = uri("https://maven.pkg.github.com/bluetape4k/bluetape4k-projects")
                credentials {
                    username = bluetape4kGprUser
                    password = bluetape4kGprPublishKey
                }
            }
            mavenLocal()
        }
    }
}
