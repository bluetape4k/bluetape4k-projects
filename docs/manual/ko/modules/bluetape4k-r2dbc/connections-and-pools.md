---
title: Connection과 pool
description: R2DBC connection option과 pool을 구성하고 종료·과부하 책임을 정하는 방법을 설명합니다.
manualId: bluetape4k-r2dbc
chapterId: connections-and-pools
---

# Connection과 pool

## pool은 애플리케이션 자원이다

`r2dbcConnectionPool`은 `ConnectionPool`을 만들어 반환할 뿐, 종료 hook을 설치하지 않습니다. 직접 만든 pool은 애플리케이션 시작·종료 lifecycle에 연결하고 반드시 `close()`합니다. pool에서 직접 얻은 connection도 사용 후 닫아야 슬롯이 반환됩니다.

```kotlin
import io.bluetape4k.r2dbc.pool.r2dbcConnectionPool
import java.time.Duration

val pool = r2dbcConnectionPool(
    "r2dbc:postgresql://app:secret@db.example.com:5432/app"
) {
    maxSize = 32
    initialSize = 8
    minIdle = 8
    maxAcquireTime = Duration.ofSeconds(3)
    maxPendingAcquire = 128
    poolName = "app-r2dbc"
}

try {
    // application work
} finally {
    pool.close()
}
```

credential을 URL에 직접 넣는 방식은 예제를 짧게 만들기 위한 것입니다. 실제 애플리케이션에서는 secret provider와 framework 설정을 사용해 log나 진단 정보에 password가 노출되지 않게 합니다.

## 연결 option DSL

`R2dbcConnectionConfig`는 driver, protocol, host, port, database, user, password, SSL과 timeout을 `ConnectionFactoryOptions`로 옮깁니다. driver는 필수이며 공백이면 즉시 실패합니다. driver별 option은 `option`으로 추가할 수 있습니다.

```kotlin
val pool = r2dbcConnectionPool {
    connection {
        driver = "postgresql"
        host = "db.example.com"
        port = 5432
        database = "app"
        user = "app"
        password = databasePassword
        ssl = true
    }
    pool {
        maxSize = 32
        initialSize = 8
        minIdle = 8
    }
}
```

`ssl` 기본값은 `false`입니다. 운영 환경에서는 driver가 요구하는 인증서·hostname 검증 option까지 함께 확인합니다.

## pool 검증 규칙

- `maxSize`는 양수여야 합니다.
- `initialSize`와 `minIdle`은 0 이상이며 `maxSize`를 넘을 수 없습니다.
- `acquireRetry`는 0 이상입니다.
- `maxPendingAcquire`는 `-1` 또는 0 이상입니다.
- `registerJmx = true`이면 공백이 아닌 `poolName`이 필요합니다.
- `validationQuery`는 공백일 수 없습니다.

생성 후 mutable property를 바꿔도 `toConnectionPoolConfiguration`이 다시 `validate()`를 호출합니다. 잘못된 값을 늦게라도 pool 생성 전에 차단합니다.

## overload를 숨기지 않는다

`maxPendingAcquire = -1`은 무제한 queue입니다. 짧은 부하는 흡수할 수 있지만 DB가 느려진 동안 요청이 계속 쌓이면 memory와 tail latency가 함께 증가합니다. 사용자 요청을 처리하는 서비스에서는 유한한 queue와 acquire timeout을 두고 실패율을 지표로 관찰합니다.

`R2dbcPoolConfig.highThroughput(maxSize)`는 warmup connection, `maxSize * 4` pending queue, 3초 acquire timeout과 LOCAL validation을 기본으로 둡니다. 이 값은 출발점이지 자동 capacity 계산이 아닙니다. 전체 DB connection 한도에서 운영·복제용 예약분을 빼고 인스턴스 수로 나눈 뒤 부하 테스트로 조정합니다.

## validation 비용

driver의 LOCAL validation이 충분하면 `validationQuery = null`을 우선합니다. `SELECT 1` 같은 query를 지정하면 connection을 빌릴 때마다 DB 왕복이 추가됩니다. 반대로 network 단절을 LOCAL validation이 감지하지 못하는 driver라면 REMOTE validation 비용을 받아들일 수 있습니다.

## Source와 tests

- [`R2dbcConnectionConfig.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionConfig.kt)
- [`R2dbcPoolConfig.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt)
- [`ConnectionPoolSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt)
- [`R2dbcConnectionConfigTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionConfigTest.kt)
- [`ConnectionPoolSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupportTest.kt)

## 다음 읽을 장

pool 소유권을 정했다면 [SQL 실행과 parameter binding](./sql-and-binding.md)에서 query 실행과 typed null 계약을 확인합니다.
