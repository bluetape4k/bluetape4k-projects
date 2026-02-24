package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
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
 * Ignite 3.x [NearCache] / [NearSuspendCache] 팩토리 및 생명주기 관리 클래스입니다.
 *
 * 동일한 이름의 캐시를 중복 생성하지 않고 재사용하는 getOrCreate 시멘틱을 제공하며,
 * [close] 호출 시 관리 중인 모든 캐시의 Front Cache(Caffeine)를 정리합니다.
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
            val boxed = this.kotlin.javaObjectType
            return SQL_TYPE_MAP[boxed] ?: "VARCHAR"
        }
    }

    private val nearCaches = ConcurrentHashMap<String, NearCache<*, *>>()
    private val suspendNearCaches = ConcurrentHashMap<String, NearSuspendCache<*, *>>()
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
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <K: Any, V: Any> getOrCreateNearCache(
        config: IgniteNearCacheConfig<K, V>,
    ): NearCache<K, V> {
        return nearCaches.getOrPut(config.tableName) {
            log.debug { "IgniteNearCache(NearCache) 생성. tableName=${config.tableName}" }
            igniteNearCache(client, config)
        } as NearCache<K, V>
    }

    /**
     * 레지스트리에 [IgniteSuspendNearCache]를 등록하거나 기존 캐시를 반환합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <K: Any, V: Any> getOrCreateSuspendNearCache(
        config: IgniteNearCacheConfig<K, V>,
    ): NearSuspendCache<K, V> {
        return suspendNearCaches.getOrPut(config.tableName) {
            log.debug { "IgniteSuspendNearCache(NearSuspendCache) 생성. tableName=${config.tableName}" }
            igniteNearSuspendCache(client, config)
        } as NearSuspendCache<K, V>
    }

    /**
     * 테이블을 자동 생성하고 [IgniteNearCache] ([NearCache])를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.tableName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config NearCache 설정 (테이블 이름, 키/값 타입 포함)
     * @param keyColumn 키 컬럼 이름 (기본값: config 값)
     * @param valueColumn 값 컬럼 이름 (기본값: config 값)
     * @return [IgniteNearCache] ([NearCache]) 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> nearCache(
        config: IgniteNearCacheConfig<K, V>,
        keyColumn: String = config.keyColumn,
        valueColumn: String = config.valueColumn,
    ): IgniteNearCache<K, V> {
        checkNotClosed()
        ensureTable(config.tableName, K::class.javaObjectType, V::class.java, keyColumn, valueColumn)
        return getOrCreateNearCache(config)
    }

    /**
     * 테이블을 자동 생성하고 [IgniteSuspendNearCache] ([NearSuspendCache])를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.tableName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config NearCache 설정 (테이블 이름, 키/값 타입 포함)
     * @param keyColumn 키 컬럼 이름 (기본값: config 값)
     * @param valueColumn 값 컬럼 이름 (기본값: config 값)
     * @return [IgniteSuspendNearCache] ([NearSuspendCache]) 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> suspendNearCache(
        config: IgniteNearCacheConfig<K, V>,
        keyColumn: String = config.keyColumn,
        valueColumn: String = config.valueColumn,
    ): IgniteSuspendNearCache<K, V> {
        checkNotClosed()
        ensureTable(config.tableName, K::class.javaObjectType, V::class.java, keyColumn, valueColumn)
        return getOrCreateSuspendNearCache(config)
    }

    /**
     * 테이블을 자동 생성하고 [KeyValueView]를 반환합니다.
     *
     * Memorizer 등에서 [KeyValueView]를 직접 사용할 때 활용합니다.
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
            runCatching { cache.clear() }
                .onFailure { log.warn(it) { "IgniteNearCache Front Cache 초기화 중 오류 발생. tableName=$tableName" } }
        }
        suspendNearCaches.remove(tableName)?.let {
            log.debug { "IgniteSuspendNearCache Front Cache 초기화. tableName=$tableName" }
            // NearSuspendCache.clear()는 suspend 함수이므로 블로킹 제거만 수행
        }
    }

    /**
     * 관리 중인 모든 캐시의 Front Cache(Caffeine)를 초기화하고 Manager를 닫습니다.
     *
     * Back Cache(Ignite)는 건드리지 않습니다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.debug { "IgniteNearCacheManager 종료. nearCaches=${nearCaches.size}, suspendNearCaches=${suspendNearCaches.size}" }
            nearCaches.values.forEach { cache ->
                runCatching { cache.close() }
                    .onFailure { log.warn(it) { "IgniteNearCache 종료 중 오류 발생." } }
            }
            nearCaches.clear()
            suspendNearCaches.clear()
        }
    }
}

/**
 * [IgniteClient]에서 [IgniteNearCacheManager]를 생성합니다.
 *
 * @return [IgniteNearCacheManager] 인스턴스
 */
fun IgniteClient.nearCacheManager(): IgniteNearCacheManager = IgniteNearCacheManager(this)
