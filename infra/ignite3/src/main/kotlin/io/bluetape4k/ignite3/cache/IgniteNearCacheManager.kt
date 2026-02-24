package io.bluetape4k.ignite3.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView
import java.io.Closeable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ignite 3.x NearCache 팩토리 및 생명주기 관리 클래스입니다.
 *
 * 테이블이 존재하지 않으면 키/값 Java 타입에서 SQL 타입을 자동 추론하여
 * `CREATE TABLE IF NOT EXISTS` DDL을 실행하고, NearCache 구현체 또는 [KeyValueView]를 반환합니다.
 *
 * 동일한 이름의 캐시를 중복 생성하지 않고 재사용하는 getOrCreate 시멘틱을 제공하며,
 * [close] 호출 시 관리 중인 모든 캐시의 Front Cache(Caffeine)를 정리합니다.
 *
 * 지원하는 자동 매핑 타입: `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Boolean`,
 * `String`, `ByteArray`, `UUID`, `BigDecimal`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`
 *
 * 기타 타입은 `VARCHAR`로 매핑됩니다.
 *
 * ```kotlin
 * val manager = igniteClient.nearCacheManager()
 *
 * // Long→String NearCache (테이블 없으면 자동 생성)
 * val nearCache: IgniteNearCache<Long, String> = manager.nearCache(
 *     IgniteNearCacheConfig(tableName = "MY_CACHE")
 * )
 *
 * // Int→Int KeyValueView (Memorizer용)
 * val view: KeyValueView<Int, Int> = manager.keyValueView("MY_TABLE")
 * val memorizer = view.memorizer { x -> x * x }
 *
 * manager.close()
 * ```
 *
 * @property client Ignite 3.x 씬 클라이언트
 */
class IgniteNearCacheManager(val client: IgniteClient): Closeable {

    companion object: KLogging() {

        /**
         * Kotlin/Java 타입 → Ignite 3.x SQL 컬럼 타입 매핑 테이블
         * (boxed 타입 기준, `javaObjectType`으로 primitive → boxed 변환 후 조회)
         */
        private val SQL_TYPE_MAP: Map<Class<*>, String> = mapOf(
            Byte::class.javaObjectType to "TINYINT",
            Short::class.javaObjectType to "SMALLINT",
            Int::class.javaObjectType to "INTEGER",
            Long::class.javaObjectType to "BIGINT",
            Float::class.javaObjectType to "FLOAT",
            Double::class.javaObjectType to "DOUBLE",
            Boolean::class.javaObjectType to "BOOLEAN",
            String::class.java to "VARCHAR",
            ByteArray::class.java to "VARBINARY",
            UUID::class.java to "UUID",
            BigDecimal::class.java to "DECIMAL",
            LocalDate::class.java to "DATE",
            LocalTime::class.java to "TIME",
            LocalDateTime::class.java to "TIMESTAMP",
            Instant::class.java to "TIMESTAMP",
        )

        /**
         * Java/Kotlin 클래스를 Ignite 3.x SQL 컬럼 타입 문자열로 변환합니다.
         *
         * primitive(`int`, `long` 등)는 boxed 타입으로 변환하여 매핑합니다.
         * 매핑 테이블에 없는 타입은 `"VARCHAR"`를 반환합니다.
         */
        fun Class<*>.toIgniteSqlType(): String {
            // Kotlin 원시 타입(int, long 등)을 boxed 타입으로 변환하여 조회
            val boxed = this.kotlin.javaObjectType
            return SQL_TYPE_MAP[boxed] ?: "VARCHAR"
        }
    }

    private val nearCaches = ConcurrentHashMap<String, IgniteNearCache<*, *>>()
    private val suspendNearCaches = ConcurrentHashMap<String, IgniteSuspendNearCache<*, *>>()
    private val closed = AtomicBoolean(false)

    /** 관리 중인 모든 캐시 이름 목록 (sync + suspend 캐시 합산) */
    val cacheNames: Set<String>
        get() = nearCaches.keys + suspendNearCaches.keys

    /** Manager가 닫힌 상태인지 여부 */
    val isClosed: Boolean get() = closed.get()

    /**
     * closed 상태에서 작업을 수행하려 할 때 [IllegalStateException]을 발생시킵니다.
     */
    @PublishedApi
    internal fun checkNotClosed() {
        check(!closed.get()) { "IgniteNearCacheManager가 이미 닫혀 있습니다." }
    }

    /**
     * 테이블이 없으면 DDL을 자동 생성하여 실행합니다.
     *
     * 키/값 타입을 SQL 타입으로 자동 추론하며, 이미 테이블이 존재하면 아무 작업도 하지 않습니다.
     *
     * @param tableName Ignite 3.x 테이블 이름
     * @param keyType 키 컬럼 Java 타입
     * @param valueType 값 컬럼 Java 타입
     * @param keyColumn 키 컬럼 이름 (기본값: `"ID"`)
     * @param valueColumn 값 컬럼 이름 (기본값: `"DATA"`)
     */
    fun ensureTable(
        tableName: String,
        keyType: Class<*>,
        valueType: Class<*>,
        keyColumn: String = "ID",
        valueColumn: String = "DATA",
    ) {
        with(Companion) {
            val keySqlType = keyType.toIgniteSqlType()
            val valueSqlType = valueType.toIgniteSqlType()
            val ddl = "CREATE TABLE IF NOT EXISTS $tableName ($keyColumn $keySqlType PRIMARY KEY, $valueColumn $valueSqlType)"
            log.debug { "테이블 자동 생성 DDL 실행. tableName=$tableName, ddl=$ddl" }
            client.sql().execute(null, ddl).close()
        }
    }

    /**
     * 레지스트리에 [IgniteNearCache]를 등록하거나 기존 캐시를 반환합니다.
     * inline 함수에서 private 멤버 접근이 불가하므로 내부 위임 메서드로 분리합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <K: Any, V: Any> getOrCreateNearCache(
        config: IgniteNearCacheConfig,
        keyType: Class<K>,
        valueType: Class<V>,
    ): IgniteNearCache<K, V> {
        return nearCaches.getOrPut(config.tableName) {
            log.debug { "IgniteNearCache 생성. tableName=${config.tableName}" }
            IgniteNearCache(client, keyType, valueType, config)
        } as IgniteNearCache<K, V>
    }

    /**
     * 레지스트리에 [IgniteSuspendNearCache]를 등록하거나 기존 캐시를 반환합니다.
     * inline 함수에서 private 멤버 접근이 불가하므로 내부 위임 메서드로 분리합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <K: Any, V: Any> getOrCreateSuspendNearCache(
        config: IgniteNearCacheConfig,
        keyType: Class<K>,
        valueType: Class<V>,
    ): IgniteSuspendNearCache<K, V> {
        return suspendNearCaches.getOrPut(config.tableName) {
            log.debug { "IgniteSuspendNearCache 생성. tableName=${config.tableName}" }
            IgniteSuspendNearCache(client, keyType, valueType, config)
        } as IgniteSuspendNearCache<K, V>
    }

    /**
     * 테이블을 자동 생성하고 [IgniteNearCache]를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.tableName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config NearCache 설정 (테이블 이름 포함)
     * @param keyColumn 키 컬럼 이름 (기본값: `"ID"`)
     * @param valueColumn 값 컬럼 이름 (기본값: `"DATA"`)
     * @return [IgniteNearCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> nearCache(
        config: IgniteNearCacheConfig,
        keyColumn: String = "ID",
        valueColumn: String = "DATA",
    ): IgniteNearCache<K, V> {
        checkNotClosed()
        ensureTable(config.tableName, K::class.java, V::class.java, keyColumn, valueColumn)
        return getOrCreateNearCache(config, K::class.java, V::class.java)
    }

    /**
     * 테이블을 자동 생성하고 [IgniteSuspendNearCache]를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.tableName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config NearCache 설정 (테이블 이름 포함)
     * @param keyColumn 키 컬럼 이름 (기본값: `"ID"`)
     * @param valueColumn 값 컬럼 이름 (기본값: `"DATA"`)
     * @return [IgniteSuspendNearCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> suspendNearCache(
        config: IgniteNearCacheConfig,
        keyColumn: String = "ID",
        valueColumn: String = "DATA",
    ): IgniteSuspendNearCache<K, V> {
        checkNotClosed()
        ensureTable(config.tableName, K::class.java, V::class.java, keyColumn, valueColumn)
        return getOrCreateSuspendNearCache(config, K::class.java, V::class.java)
    }

    /**
     * 테이블을 자동 생성하고 [KeyValueView]를 반환합니다.
     *
     * Memorizer 등에서 [KeyValueView]를 직접 사용할 때 활용합니다.
     * Ignite 3.x API 호환성을 위해 boxed 타입(`javaObjectType`)으로 [KeyValueView]를 생성합니다.
     * [KeyValueView]는 경량 래퍼이므로 상태를 추적하지 않습니다.
     *
     * ```kotlin
     * val view = manager.keyValueView<Int, Int>("MY_TABLE")
     * val memorizer = view.memorizer { x -> x * x }
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param tableName 테이블 이름
     * @param keyColumn 키 컬럼 이름 (기본값: `"ID"`)
     * @param valueColumn 값 컬럼 이름 (기본값: `"DATA"`)
     * @return [KeyValueView] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> keyValueView(
        tableName: String,
        keyColumn: String = "ID",
        valueColumn: String = "DATA",
    ): KeyValueView<K, V> {
        checkNotClosed()
        ensureTable(tableName, K::class.java, V::class.java, keyColumn, valueColumn)
        val table = client.tables().table(tableName)
            ?: error("테이블 생성 후에도 조회에 실패했습니다. tableName=$tableName")
        // Ignite 3.x API는 boxed 타입을 요구하므로 javaObjectType 사용
        return table.keyValueView(K::class.javaObjectType, V::class.javaObjectType)
    }

    /**
     * 지정한 이름의 캐시를 레지스트리에서 제거하고 Front Cache(Caffeine)를 초기화합니다.
     *
     * Back Cache(Ignite)는 건드리지 않습니다.
     *
     * @param tableName 제거할 캐시의 테이블 이름
     */
    fun destroyCache(tableName: String) {
        nearCaches.remove(tableName)?.let { cache ->
            log.debug { "IgniteNearCache Front Cache 초기화. tableName=$tableName" }
            runCatching { cache.clearFrontCache() }
                .onFailure { log.warn(it) { "IgniteNearCache Front Cache 초기화 중 오류 발생. tableName=$tableName" } }
        }
        suspendNearCaches.remove(tableName)?.let { cache ->
            log.debug { "IgniteSuspendNearCache Front Cache 초기화. tableName=$tableName" }
            runCatching { cache.clearFrontCache() }
                .onFailure { log.warn(it) { "IgniteSuspendNearCache Front Cache 초기화 중 오류 발생. tableName=$tableName" } }
        }
    }

    /**
     * 관리 중인 모든 캐시의 Front Cache(Caffeine)를 초기화하고 Manager를 닫습니다.
     *
     * Back Cache(Ignite)는 건드리지 않습니다.
     * 레지스트리를 비우고 closed 상태로 전환합니다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.debug { "IgniteNearCacheManager 종료. nearCaches=${nearCaches.size}, suspendNearCaches=${suspendNearCaches.size}" }
            nearCaches.values.forEach { cache ->
                runCatching { cache.clearFrontCache() }
                    .onFailure { log.warn(it) { "IgniteNearCache Front Cache 초기화 중 오류 발생." } }
            }
            nearCaches.clear()

            suspendNearCaches.values.forEach { cache ->
                runCatching { cache.clearFrontCache() }
                    .onFailure { log.warn(it) { "IgniteSuspendNearCache Front Cache 초기화 중 오류 발생." } }
            }
            suspendNearCaches.clear()
        }
    }
}

/**
 * [IgniteClient]에서 [IgniteNearCacheManager]를 생성합니다.
 *
 * ```kotlin
 * val manager = igniteClient.nearCacheManager()
 *
 * val nearCache = manager.nearCache<Long, String>(
 *     IgniteNearCacheConfig(tableName = "MY_CACHE")
 * )
 * val view = manager.keyValueView<Int, Int>("MY_TABLE")
 * ```
 *
 * @return [IgniteNearCacheManager] 인스턴스
 */
fun IgniteClient.nearCacheManager(): IgniteNearCacheManager = IgniteNearCacheManager(this)
