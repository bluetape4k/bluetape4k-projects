import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.JavaExec

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
}

// Compile a consumer with the pre-fix operator, then load only that compiled
// consumer against the current main output. Keeping the fixture in test sources
// makes this binary check independent from the checkout's Git history.
val measuredCompatSourceDir = layout.buildDirectory.dir("compat/issue-1724/source")
val measuredCompatCompileDir = layout.buildDirectory.dir("compat/issue-1724/compiled")
val measuredCompatFixtureDir = layout.buildDirectory.dir("compat/issue-1724/fixture")
val measuredCompatBaseSource = layout.projectDirectory.file("src/test/resources/compat/issue-1724/DataRateOperations.kt")
val measuredCompatDataRateSource = measuredCompatSourceDir.map {
    it.file("DataRateOperations.kt")
}
val measuredCompatConsumerSource = measuredCompatSourceDir.map {
    it.file("MeasuredLegacyBinaryConsumer.kt")
}

val generateMeasuredCompatSources = tasks.register("generateMeasuredCompatSources") {
    description = "Generates the BASE DataRate operator and its legacy consumer fixture."
    outputs.dir(measuredCompatSourceDir)

    doLast {
        val sourceDir = measuredCompatSourceDir.get().asFile.apply { mkdirs() }
        measuredCompatBaseSource.asFile.copyTo(measuredCompatDataRateSource.get().asFile, overwrite = true)
        measuredCompatConsumerSource.get().asFile.writeText(
            """
            package io.bluetape4k.measured

            fun legacyBinaryRate(): Measure<DataRate> =
                1.megabytes10() / 1.seconds()
            """.trimIndent() + "\n",
        )
    }
}

val compileMeasuredCompatConsumer = tasks.register<JavaExec>("compileMeasuredCompatConsumer") {
    description = "Compiles the legacy consumer against the pre-fix DataRate operator fixture."
    dependsOn(generateMeasuredCompatSources, tasks.named("compileKotlin"))
    classpath = configurations.getByName("kotlinCompilerClasspath")
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    val outputDir = measuredCompatCompileDir.get().asFile
    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
    }
    args(
        "-jvm-target",
        "25",
        "-classpath",
        (sourceSets.main.get().output + configurations.getByName("compileClasspath")).asPath,
        "-d",
        outputDir.absolutePath,
        measuredCompatDataRateSource.get().asFile.absolutePath,
        measuredCompatConsumerSource.get().asFile.absolutePath,
    )
    outputs.dir(measuredCompatCompileDir)
}

val prepareMeasuredCompatFixture = tasks.register<Sync>("prepareMeasuredCompatFixture") {
    description = "Keeps only the BASE-compiled consumer for HEAD binary-link verification."
    dependsOn(compileMeasuredCompatConsumer)
    from(measuredCompatCompileDir) {
        include("io/bluetape4k/measured/MeasuredLegacyBinaryConsumerKt.class")
    }
    into(measuredCompatFixtureDir)
}

tasks.named<Test>("test") {
    dependsOn(prepareMeasuredCompatFixture)
    systemProperty("bluetape4k.measured.compat.fixture", measuredCompatFixtureDir.get().asFile.absolutePath)
}
