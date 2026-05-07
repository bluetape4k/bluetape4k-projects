val exposedVersion: String by project

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    // javax.media:jai_core 는 Maven Central에 없으므로 모든 classpath에서 제외
    all {
        exclude(group = "javax.media", module = "jai_core")
    }
}

// JUnit 5 태그 필터링 — CI 기본은 slow-netcdf 제외, nightly 는 -PincludeTags 로 활성화
// include 가 명시되면 default exclude 적용 안 함 (충돌 방지 — Codex Plan v2.1 Critical#1)
tasks.test {
    useJUnitPlatform {
        val include = (project.findProperty("includeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val excludeProp = (project.findProperty("excludeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        val exclude = when {
            excludeProp != null -> excludeProp
            include.isNotEmpty() -> emptyList()
            else -> listOf("slow-netcdf")
        }
        include.forEach { includeTags(it) }
        exclude.forEach { excludeTags(it) }
    }
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-logging"))
    api(libs.jts.core)

    // GIS / 좌표 변환 (LGPL — compileOnly로만 선언, JAI 제외)
    compileOnly(libs.proj4j)
    compileOnly(libs.proj4j.epsg)
    compileOnly(libs.esri.geometry.api)

    // GeoTools (LGPL — compileOnly, javax.media:jai_core 제외 설정됨)
    compileOnly(libs.geotools.shapefile)
    compileOnly(libs.geotools.referencing)
    compileOnly(libs.geotools.epsg.hsql)

    // NetCDF (UCAR netCDF-Java 5.9.1 — compileOnly, BSD-3-Clause)
    // 저장소: 루트 build.gradle.kts:71 에 Unidata Nexus 선언됨
    compileOnly(libs.ucar.cdm.core)
    compileOnly(libs.ucar.netcdf4)
    // cdm-core 가 ImmutableList<Variable> 등 Guava 컬렉션을 API 표면에 노출 → 컴파일 시 필요
    compileOnly(libs.guava)

    // Micrometer — MeterRegistry 선택 주입 (compileOnly)
    compileOnly(libs.micrometer.core)

    // Coroutines (compileOnly)
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)

    // Exposed / DB (선택적, compileOnly)
    compileOnly("io.bluetape4k.exposed:bluetape4k-exposed-jdbc:${exposedVersion}")
    compileOnly("io.bluetape4k.exposed:bluetape4k-exposed-postgresql:${exposedVersion}")
    compileOnly("io.bluetape4k.exposed:bluetape4k-exposed-jackson3:${exposedVersion}")
    compileOnly(libs.postgis.jdbc)
    compileOnly(libs.exposed.java.time)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation("io.bluetape4k.exposed:bluetape4k-exposed-jdbc:${exposedVersion}")
    testImplementation("io.bluetape4k.exposed:bluetape4k-exposed-postgresql:${exposedVersion}")
    testImplementation("io.bluetape4k.exposed:bluetape4k-exposed-jackson3:${exposedVersion}")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.hikaricp)
}
