# Issue #1351 NearJCache 운영 계측 교훈

## Configuration은 source와 exactness를 함께 기록한다

JCache provider가 노출하는 configuration은 항상 caller가 넘긴 configuration과 같지 않다. 타입은
actual front, supplied front, actual back 순서의 **완전한 key/value pair**로만 선택해야 한다.
서로 다른 source의 key와 value를 조합하면 실제로 존재하지 않는 configuration을 만들어 낸다.

fallback 값만 노출하면 운영자가 inferred type을 actual type으로 오해한다. 따라서
`typeResolutionSource`와 `typeResolutionExact`를 값과 함께 제공해야 한다. requested configuration
class 미지원 `IllegalArgumentException`만 fallback하고 lifecycle/security/provider failure는 숨기지
않는 것도 같은 원칙이다.

## Reset은 counter를 지우는 동작이 아니라 generation 교체다

동시 operation의 여러 `LongAdder`를 제자리에서 초기화하면 이전 count와 새 elapsed time을 섞을 수
있다. operation 시작 시 recorder generation을 한 번 capture하고, `clear()`는 `AtomicReference`의
generation을 교체해야 한다. 그러면 진행 중인 operation은 이전 또는 새 generation 한쪽에만
기록된다. 개별 attribute의 weak consistency는 남지만 서로 다른 generation을 한 계산에 섞지는
않는다.

## Collision ownership은 정상 반환된 registration만 뜻한다

`registerMBean` 호출을 시도했다는 사실은 ownership 증거가 아니다. 정상 반환된
`ObjectInstance.objectName`과 descriptor token만 owned set에 넣어야 한다. collision 이름이나 정상
반환 전에 실패한 이름을 rollback하면 다른 owner의 MBean을 삭제할 수 있다.

descriptor token은 cleanup 전에 완료된 foreign replacement를 탐지하지만 token 확인과 unregister를
원자적으로 만들지는 못한다. JMX API에 CAS가 없으므로 handle lifetime 동안 exact ObjectName을
exclusive namespace로 유지한다는 caller 계약이 필요하다.

## Close 실패는 terminal boolean이 아니라 resource별 retry 상태다

JMX handle, back listener, front cache를 하나의 `closed` boolean으로 합치면 첫 cleanup 실패 뒤 성공한
resource까지 다시 닫거나, 실패한 resource를 영구 누수시킬 수 있다. close attempt를 예약한 뒤 외부
호출은 내부 lock 밖에서 실행하고, 각 resource의 완료 상태를 따로 보존해야 한다. 첫 실패는 primary,
이후 실패는 resource 순서의 suppressed 예외로 유지하며 다음 close는 미완료 resource만 재시도한다.

registration과 close 경합에서는 pending reservation이 중요했다. 먼저 예약된 registration은 publish와
rollback을 끝내고, close는 그 completion을 기다린 뒤 registry를 drain해야 한다. `MBeanServer`
callback 재진입은 operation guard로 fail-fast해야 lifecycle lock을 건 채 외부 server를 호출하지 않는
경계를 지킬 수 있다.

## Caller-visible 통계와 remote completion을 합치지 않는다

비동기 write에서 wrapper가 정상 반환한 시점과 back provider가 commit한 시점은 다르다. 표준 put/remove
통계는 caller-visible 성공을 기록하고, remote 성공·실패·retry·timeout은 atomic
`BackCacheWriteCompletion`의 `operationId`, `operation`, `completion`으로 관찰해야 한다. MXBean count를
durable commit 성공으로 해석하면 장애 시 통계 의미가 바뀐다.

동시 mutation에서는 여러 `last*` property를 따로 읽어 correlation하지 않는다. listener가 전달한
단일 completion snapshot을 보존해야 같은 operation의 결과를 연결할 수 있다.

## Baseline-first benchmark가 드러낸 surprise

기능 test와 fake clock test만으로는 disabled path의 비용 계약을 충분히 증명하지 못했다. 첫 candidate는
모든 operation에서 NoOp recorder method를 호출했고 `getAndPut` throughput이 baseline의 약 91.5%까지
하락했다. disabled `get`을 원래 read-through 경로로 분리한 뒤에도 8-thread GC profiler allocation이
uncertainty budget을 각각 `0.000288 B/op`, `0.000083 B/op` 초과했다.

threshold를 완화하거나 candidate만 유리하게 반복하지 않고 매번 baseline/candidate pair 전체를
폐기했다. 마지막에는 disabled path가 새 recorder field를 읽지 않고 immutable configuration snapshot의
boolean으로 active 경로를 선택하게 했다. 새 전체 pair에서 10개 key가 모두 통과했고 최저 throughput
ratio는 96.23%였다.

future guard는 다음과 같다.

- benchmark source hash, JDK/JMH profile, fork/warmup/measurement를 먼저 대조한다.
- rawData median과 JMH score/error/allocation rawData를 함께 보존한다.
- allocation은 exact zero가 아니라 고정 uncertainty budget으로 판정한다.
- 한 key라도 실패하면 candidate만 반복하지 않고 pair 전체를 다시 측정한다.
- active statistics 결과는 contention 관찰로만 사용하고 production ranking으로 일반화하지 않는다.

## 문서 예제도 resource ownership test다

문서 실행 test에서 provider-managed shared `CacheManager`를 닫자 뒤에 실행된 cache test가
`CacheManagerImpl.requireNotClosed`로 실패했다. wrapper가 소유한 front cache와 provider가 소유한
manager의 lifecycle을 예제에서도 분리해야 한다. 문서 test는 front close를 검증하되 shared manager가
열려 있음을 확인하고, provider shutdown 시점의 manager close는 별도 owner에게 맡긴다.
