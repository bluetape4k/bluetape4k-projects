# Test Execution Log

기능 추가/수정 후 테스트 수행 결과를 기록합니다.

---

## 2026-04-04

### KDoc/README 프로퍼티 키 `.` → `-` 잔여 수정 (MemgraphServer, RedisClusterServer)

| 대상 | 테스트 항목 | 결과 | 소요 | 비고 |
|------|------------|------|------|------|
| `MemgraphServerTest.kt` | MemgraphServerTest (6 tests) | ✅ | 14.1s | `bolt-port`, `log-port`, `bolt-url` KDoc 수정 확인 |
| `RedisClusterServerTest.kt` | RedisClusterServerTest (3 tests) | ✅ | 14.1s | `redis-cluster` namespace KDoc 수정 확인 |

### ZooKeeperServer Curator 연결 안정화 (connectionTimeout/blockUntilConnected)

| 대상 | 테스트 항목 | 결과 | 소요 | 비고 |
|------|------------|------|------|------|
| `ZooKeeperServerTest.kt` | `ZooKeeperServerTest` (4 tests) | ✅ | 8s | UseDockerPort 5.2s — IPv6→IPv4 폴백 안정화 확인 |

**변경 파일:**
- `ZooKeeperServer.kt:112-113` — `RetryOneTime(100)` → `RetryOneTime(1000)`, `connectionTimeoutMs(3000)` → `connectionTimeoutMs(10_000)`
- `ZookeeperServerSupport.kt:20` — `curator.start()` 후 `blockUntilConnected(10, SECONDS)` 추가

---

## 2026-04-03

### testcontainers property key `.` → `-` 마이그레이션 (테스트 코드 동기화)

| 대상 | 테스트 항목 | 결과 | 소요 | 비고 |
|------|------------|------|------|------|
| `Neo4jServerTest.kt` | `compileTestKotlin` | ✅ | 9s | 컴파일 검증 |
| `Neo4jServerTest.kt` | `Neo4jServerTest` (7 tests) | ✅ | 3.9s | `bolt-url` 프로퍼티 검증 통과 |
| `KeycloakServerTest.kt` | `KeycloakServerTest` (4 tests) | ✅ | 2.6s | `auth-url`, `admin-username`, `admin-password` 검증 통과 |
| `InfluxDBServerTest.kt` | `InfluxDBServerTest` (2 tests) | ✅ | 3.8s | `admin-token` 프로퍼티 검증 통과 |

**변경 파일:**
- `Neo4jServerTest.kt:58` — `bolt.url` → `bolt-url`
- `InfluxDBServerTest.kt:35` — `admin.token` → `admin-token`
- `KeycloakServerTest.kt:37-39` — `auth.url` → `auth-url`, `admin.username` → `admin-username`, `admin.password` → `admin-password`
