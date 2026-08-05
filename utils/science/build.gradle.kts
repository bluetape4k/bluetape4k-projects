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
    api(bt4k.jts.core)

    // GIS / 좌표 변환 (LGPL — compileOnly로만 선언, JAI 제외)
    compileOnly(bt4k.proj4j)
    compileOnly(bt4k.proj4j.epsg)
    compileOnly(bt4k.esri.geometry.api)

    // GeoTools (LGPL — compileOnly, javax.media:jai_core 제외 설정됨)
    compileOnly(bt4k.geotools34.shapefile)
    compileOnly(bt4k.geotools34.referencing)
    compileOnly(bt4k.geotools34.epsg.hsql)

    // NetCDF (UCAR netCDF-Java 5.9.1 — compileOnly, BSD-3-Clause)
    // 저장소: 루트 build.gradle.kts:71 에 Unidata Nexus 선언됨
    compileOnly(bt4k.ucar.cdm.core)
    compileOnly(bt4k.ucar.netcdf4)
    // cdm-core 가 ImmutableList<Variable> 등 Guava 컬렉션을 API 표면에 노출 → 컴파일 시 필요
    compileOnly(bt4k.guava)

    // Micrometer — MeterRegistry 선택 주입 (compileOnly)
    compileOnly(libs.micrometer.core)

    // Coroutines (compileOnly)
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)

    // Exposed / DB (선택적, compileOnly) — raw JetBrains Exposed 직접 참조
    compileOnly(bt4k.exposed.core)
    compileOnly(libs.exposed.dao)
    compileOnly(bt4k.exposed.jdbc)
    compileOnly(bt4k.exposed.java.time)
    compileOnly(libs.exposed.json)
    compileOnly(bt4k.postgis.y2024)
    // JSONB 직렬화용 Jackson, PGobject (compileOnly)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(bt4k.postgresql)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(bt4k.exposed.core)
    testImplementation(libs.exposed.dao)
    testImplementation(bt4k.exposed.jdbc)
    testImplementation(bt4k.exposed.java.time)
    testImplementation(libs.exposed.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    testRuntimeOnly(bt4k.postgresql)
    testRuntimeOnly(bt4k.hikaricp)
}
