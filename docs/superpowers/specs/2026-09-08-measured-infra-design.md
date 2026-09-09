# measured 단위 및 코루틴 인프라 확장 설계

## 요청과 범위

projects #1712–#1717 구현 요청을 수행한다. #1712–#1714는 `utils/measured`, #1715는 `infra/openfga`, #1716은 `infra/qdrant`, #1717은 `infra/temporal`이다. Exposed #857과 기존 Flow tail 브랜치는 제외한다. 각 이슈는 독립적으로 검증하며 한 이슈의 성공으로 다른 이슈를 완료 처리하지 않는다.

## 현재 근거

- 기준 커밋: `6dcfa0b50`. measured 기준 테스트 181개 통과.
- `Units.kt`의 Measure 변환·표시·제네릭 연산, `EnergyPower.kt`의 의미 타입 및 JVM 이름 패턴을 재사용한다.
- 질량 기준은 g, 시간 기준은 ms다. #850의 혼합 스케일 정규화를 유지한다.
- 공식 SDK는 중앙 dependencies 카탈로그에 없으므로 원본에 먼저 추가한다. 검증은 기존 `bluetape4kDependenciesCatalogPath` 옵션으로 연결한다.

## 대안과 선택

단위는 제네릭 비율 별칭과 독립 의미 타입을 비교했다. 전기·힘·토크·전송률 모두 표시 정책을 갖는 독립 Units 타입을 선택한다. 기존 단위 추론 엔진을 변경하지 않는다.

SDK 연동은 범용 저장소/워크플로/권한 인터페이스와 공식 타입 기반 확장을 비교했다. 공식 요청·응답 타입을 보존하는 확장 함수를 선택한다. 숨겨진 재시도·캐시·자동 자원 종료를 추가하지 않는다. 서버와 클라이언트 소유권은 호출자에게 둔다. OpenFGA v0.10.0은 close API가 없으며 추가하지 않는다. Temporal v1.38.0의 공식 Kotlin DSL을 재사용한다. OpenFGA 확장은 멤버 함수에 가려지지 않도록 checkSuspending 등 별도 이름을 사용한다. Qdrant scroll 반환 항목은 RetrievedPoint다.

## 보안 책임

세 모듈은 범용 SDK 편의 계층이며 테넌트 인가 경계가 아니다. 이미 구성된 클라이언트를 받으며 인증 토큰/헤더를 별도 보관하거나 로깅하지 않는다. 호출자가 OpenFGA store/model, Qdrant collection/filter, Temporal namespace/task queue 접근 권한을 검증한다. Qdrant 필터 없는 조회는 전체 컬렉션 조회다. 확장은 공식 필터·옵션을 그대로 전달하며 테스트에서 요청 A/B의 설정이 교차 오염되지 않음을 검증한다.

운영 로그는 작업 이름과 성공/실패 상태에 한정한다. 인가 tuple, 벡터/payload, workflow 입력/결과, 인증 정보 또는 이를 포함할 수 있는 SDK 예외 메시지/스택을 기록하지 않는다. 원래 예외는 호출자에게 그대로 전달한다. 서버/SDK가 정한 개별 payload 제한을 임의의 래퍼 직렬화로 재구현하지 않는다.

## measured 계약

- Current(A, mA, kA), Charge(C, mC, μC), Voltage(V, mV, kV), Resistance(Ω, mΩ, kΩ, MΩ)를 추가한다.
- 전류×시간, 전하÷시간, 전력÷전류, 전압÷전류는 각각 C, A, V, Ω로 환원한다. 계산 전에 s·A·W로 정규화한다. 역연산은 동일 기준 단위를 사용한다.
- DataRate 기준은 B/s다. SI bit·byte 및 IEC byte 단위를 제공한다. 기본 toHuman은 decimal byte를 선택하고, 별도 명시적 포맷 API로 SI bit/IEC byte를 선택한다. 숫자 생성 단위가 달라도 기본 정책은 동일하다.
- DataRate×Time은 byte로 환원한다. 기존 `BinarySize / Time` 제네릭 연산자의
  `UnitsRatio<BinarySize, Time>` 반환형을 보존하며 전송률 환원은 명명된
  `toDataRate(duration)` API를 사용한다. 시간은 반드시 초로 환산한다.
- Force(N, mN, kN, MN)는 kg×m/s²로 정규화한다. Torque(N·m, kN·m)는 별도 의미 타입이다.
- 토크는 `torqueAt(perpendicularArm)`처럼 수직 모멘트암을 명시하는 API로 계산한다. 일반 Force×Length를 Torque로 덮어쓰지 않는다.
- 기존 Measure의 nullable·비유한 값·0·음수 계약과 기존 연산을 변경하지 않는다. 각 의미 연산에 고유한 JVM 이름을 부여한다.

세부 API는 `DataRateFormat.DECIMAL_BYTES`, `DECIMAL_BITS`, `BINARY_BYTES`와 `Measure<DataRate>.toHuman(format)`으로 확정한다. `1.megaBytes() / 500.milliseconds()`는 고유 JVM 이름의 특수 연산자로 `Measure<DataRate>`를 반환한다. `Force / Acceleration`은 kg 단위 Mass를 반환하며 혼합 스케일을 검증한다. nullable 생성 API는 기존 Measure에 없으므로 새로 도입하지 않는다.

Energy/Torque 분리는 정적 타입·연산·표시 계약이다. 기존 Measure의 wildcard equals는 물리량 종류를 구분하지 않는 제약이 있으며 이번 변경에서 차원 엔진/equals를 변경하지 않는다. 이 제약을 문서화하고 서로 다른 물리량을 Any 키로 혼용하지 않도록 안내한다.

## 인프라 계약

### OpenFGA

공식 비동기 SDK의 check·batch check·tuple write/read를 suspend로 연결한다. 요청마다 storeId·modelId를 명시하고 공유 클라이언트 설정을 변경하지 않는다. 허용/거부/원격 오류 및 batch 항목 오류를 공식 응답 형태로 보존한다. tuple 기본 필드 검증과 model/store 격리 테스트를 둔다. 페이지 읽기는 cold Flow이며 pageSize는 1..100으로 제한하고 한 페이지씩 읽고 소비한다. batch check와 tuple 쓰기는 공식 서버 제한에 맞는 요청 항목 상한을 검증한다. 요청 DTO 자체의 크기 제한은 공식 SDK/서버 계약을 따르며, 래퍼가 이미 만들어진 DTO의 메모리 상한을 보장한다고 주장하지 않는다. 취소 시 요청 Future 취소를 전달하되 이미 반영된 원격 쓰기의 롤백을 보장하지 않는다.

### Qdrant

공식 protobuf 요청을 받는 query·upsert·delete 확장과 cold scroll Flow를 제공한다. 옵션을 새 DTO로 축소하지 않는다. batch 입력은 항목 수와 직렬화 바이트 상한을 모두 검증하며 단일 초과 항목은 요청 전 거부한다. 기본 동시성은 1로 순차 처리한다. scroll은 page limit를 1..1000으로 제한하고 cursor를 수집별로 관리한다. 같은 cursor 반복은 탐지하되 모든 과거 cursor를 무제한 보관하지 않는다. 한 페이지의 응답 byte 크기는 payload에 따라 달라지므로 gRPC 최대 수신 크기는 호출자의 채널 설정을 따른다. 테스트에서 maxInFlight=1, 조기 종료 후 추가 요청=0, 설정된 batch 상한 초과 요청=0을 단언한다. 기존 공식 payload/filter 빌더는 그대로 사용한다.

### Temporal

공식 temporal-kotlin DSL은 그대로 사용한다. 외부 클라이언트의 start·signal·query 등 블로킹 호출은 IO dispatcher에서 실행하고, 결과 Future는 스레드를 점유하지 않는 await로 대기한다. 각 클라이언트 호출의 RPC deadline은 공식 SDK 옵션을 보존하고, 결과 대기는 호출자가 withTimeout으로 제한할 수 있음을 예제로 제공한다. withContext(IO) 안의 suspend await도 suspension 중 worker를 점유하지 않으므로 이를 스레드 누수로 오인하지 않는다. 로컬 대기 취소로 원격 workflow를 자동 취소하지 않는다. remote cancel/terminate는 명시적인 호출이다. Workflow 정의 내부에는 일반 코루틴을 도입하지 않는다. WorkerFactory 종료는 명시적 opt-in이며 기한과 강제 종료 여부를 노출한다. 주문 Activity 재시도·멱등성·보상 예제를 SDK 테스트 환경에서 검증하고 이력을 replay한다.

## 실패 모드와 검증

| 위험 | 방지와 검증 |
| --- | --- |
| g/ms 기준 혼동 | kg·s 정규화 및 혼합 접두어 수치 회귀 |
| Flow 무제한 적재 | 한 페이지·한 batch 단위 읽기, 느린 소비자·조기 취소 테스트 |
| 인가 오류를 허용으로 처리 | allow/deny/exception과 batch 부분 오류를 별개 테스트 |
| 클라이언트 공유 상태 오염 | 요청 옵션을 독립 생성하고 store/model 동시 호출 검사 |
| 로컬 취소가 원격 실행을 종료 | Temporal 실제 SDK 환경에서 취소 후 계속 실행 검증 |
| 작업자/HTTP/gRPC 누수 | 소유권, 종료, 취소, timeout 경로 테스트 |
| 새 모듈이 CI/발행에서 누락 | projects, CI/Nightly, Kover, BOM/POM, 카탈로그 검사 |

## 완료 기준

### 검토 후 확정한 실행 세부 계약

- OpenFGA의 request timeout은 공식 ClientConfiguration 설정을 보존한다. 전체 operation/page 수집 기한은 호출자의 withTimeout을 사용하며 timeout은 CancellationException 계열로 전파한다. Future 취소는 이미 반영된 write를 되돌리지 않는다. 빈 continuation token에서 종료하고 같은 token 반복은 실패시킨다.
- Qdrant Duration timeout은 각 RPC에 그대로 전달한다. in-flight future 취소·deadline 오류·주입 클라이언트 미종료를 각각 테스트한다. 배치는 성공한 각 요청의 UpdateResult를 Flow로 전달하며 후속 오류는 원래 예외로 전파한다. 이미 성공한 batch를 롤백하거나 자동 재시도하지 않는다.
- Qdrant/OpenFGA 페이지 Flow는 maxPages 기본 10000을 두고 다음 페이지가 남아 있는데 상한에 도달하면 IllegalStateException을 발생시킨다. 동일 cursor의 즉시 반복은 조기 거부하며 A→B→A 같은 장주기는 페이지 상한으로 종료한다. 상한은 양수 검증하고 호출자가 조정할 수 있다.
- Temporal 결과 대기는 명시적 job 취소와 withTimeout을 각각 테스트하고 두 경우 모두 remote cancel 호출이 없음을 검증한다.
- WorkerFactory.shutdownSuspending(timeout, force=false)은 호출자가 종료를 명시적으로 요청하는 확장이다. shutdown 후 timeout까지 awaitTermination하고 isTerminated를 Boolean으로 반환한다. force=true이면 shutdownNow를 요청하되 추가 무제한 대기는 하지 않는다. 반복 호출은 SDK의 멱등성을 유지한다. 클라이언트/service stubs는 종료하지 않는다.
- 클라이언트·Worker 운영 로그는 KLoggingChannel 기반으로 동작 이름·상태만 기록한다. 파라미터와 SDK 예외 원문을 로깅하지 않는다.
- 서버 fixture는 SDK-neutral endpoint를 노출하고 singleton을 재사용한다. 각 테스트의 임시 store/collection은 finally에서 정리하며 실패 잔여물을 진단한다.
- rollback은 consumer 코드 → CI/README 등록 → 중앙 alias 순서다. Git revert는 이미 실행된 원격 쓰기/Workflow를 되돌리지 않는다.

각 이슈 원문의 모든 수용 기준을 테스트·문서·검사 명령에 대응시킨다. 새 API는 한국어 KDoc와 README 두 언어를 함께 제공한다. 실제 서버 테스트는 OpenFGA → Qdrant 순차 실행하고 Temporal은 공식 인메모리 테스트 환경을 사용한다. 테스트·detekt·독립 리뷰를 통과하기 전 완료를 주장하지 않는다. 중앙 카탈로그가 로컬 상태이면 upstream 통합 전 제약을 명시한다. PR 생성·병합·발행은 별도 전달 권한 범위에서 처리한다.
