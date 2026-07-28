# 이슈 #755: 호출자 소유 ByteBuffer 압축 경계

## 배경

`Compressor`는 `ByteArray`와 one-argument `ByteBuffer` API를 제공했지만, 호출자가 준비한
`ByteBuffer`에 결과를 쓰는 공통 계약은 없었다. 처음에는 공개 default API와 LZ4, Deflate,
Snappy, Zstd 최적화를 한 번에 전달할 수 있다고 보았다. 그러나 공개 API의 source/target 상태
계약과 각 dependency의 실제 buffer 경계를 함께 검증하자 backend별 실패 의미와 rollback 단위가
서로 달랐다.

## dependency source에서 확인한 두 가지 함정

첫 승인 명세를 고정한 commit
`0ef9f67aa1b743eea2787ad5b8b8ec9f4e6ff343` 이후 resolved dependency source와 실행 동작을
다시 확인했다.

1. lz4-java 1.11.0의 `LZ4FastDecompressor` ByteBuffer 경로는 caller의 `limit`이 아니라
   `capacity - offset`을 입력 길이로 사용할 수 있었다. 원본 caller view를 그대로 넘기면
   `limit` 뒤 capacity tail을 압축 payload로 읽을 수 있다. 따라서 payload view는
   `position=0`, `limit=capacity=remaining`인 bounded slice로 격리해야 한다.
2. zstd-jni 1.5.7-11의 offset API는 native error code를 호출자에게 반환한 다음 검사하게 하지
   않고 내부에서 `ZstdException`을 던질 수 있다. 반환값 기반 분기만 설계하면 실제 실패
   precedence와 exception identity를 잘못 모델링한다.

이 확인 결과를 반영한 증거 commit은
`26313f9b9abe710f4d4c518269f477c4b9e42508`이다. 이 commit은 LZ4 payload capacity bound와
Zstd throw-before-return 계약을 명세에 추가하고, 독립 6관점 재검토를 다시 통과했다.

## 결정

- core PR은 executable JVM default 두 개, allocating compatibility fallback, 공통 상태 계약,
  Java/Kotlin source 및 classfile ABI fixture만 전달한다.
- LZ4, Deflate, Snappy, Zstd는 각각 별도 PR로 전달한다. 한 backend의 native/JNI failure나
  rollback이 다른 backend와 public API delivery를 함께 되돌리지 않게 한다.
- source의 `position`, `limit`, mark, byte order는 성공과 실패 모두 보존한다. target은 성공할
  때만 반환량만큼 `position`을 commit하고 실패 시 원래 position으로 rollback한다.
- fallback decompression target은 final-write bound일 뿐 resource bound가 아니다. 신뢰할 수
  없는 입력의 decompressed-size 제한은 별도 정책으로 적용한다.
- allocation 개선은 backend/storage별 반복 측정 전에는 주장하지 않는다. core matrix의 모든
  경로는 compatibility fallback이며 correctness-only다.

## LZ4 slice에서 고정한 경계

LZ4 caller-owned 경로는 heap/direct/slice/read-only source와 heap/direct/slice target의 모든
조합에서 lz4-java의 `ByteBuffer` API를 직접 사용한다. decompression에는 caller가 노출한
payload만 별도 `slice()`로 전달한다. 이 view의 `position=0`,
`limit=capacity=caller-visible remaining`이므로 `LZ4FastDecompressor`가 capacity를 입력
경계로 사용하더라도 caller limit 뒤의 tail에는 접근할 수 없다. codec이 반환한 consumed byte
수가 bounded payload 전체 길이와 다르면 trailing 또는 truncated wire로 분류해
`LZ4Exception`을 전파한다.

검증은 capacity에 완전한 wire가 남아 있지만 caller limit가 마지막 byte를 가린 direct source,
유효 wire 뒤 trailing byte, 기존/new API wire 상호 운용, big-endian header, target preflight,
overflow retry, pre-created failure identity를 포함한다. compression codec의 반환값도
`1..payloadCapacity` 범위에서만 target position을 commit한다. README의 `optimized`는 이 native
dispatch와 storage coverage만 뜻하며, allocation 개선은 아직 측정하지 않았으므로
`eligible, not yet measured`로 유지한다.

## 왜 broad backend slice를 중단했는가

LZ4의 capacity-tail read는 source isolation 문제이고, zstd-jni의 throw-before-return은 예외
분류 문제다. Deflate는 JDK loop cleanup, Snappy는 storage pairing과 validation-first 순서라는 또
다른 경계를 가진다. 이를 한 PR에서 구현하면 한 backend의 수정이 공통 helper나 다른 codec의
검증 결과를 흐릴 수 있다. 따라서 명세를 재승인하고 core/API/ABI 뒤에 네 개 backend PR을
순차 배치했다. 각 PR은 이전 PR merge와 local `develop` sync가 끝난 뒤 시작한다.

## 검증 기준

- frozen baseline jar hash와 legacy Java/Kotlin caller 및 implementor classfile을 current jar에서
  실행한다.
- 신규 `(ByteBuffer, ByteBuffer) -> int` method가 abstract가 아닌 JVM default인지 확인한다.
- heap/direct/read-only/slice source와 heap/direct/slice target에서 source 상태, target commit,
  overflow rollback, overlap 거부, failure identity, retry를 검증한다.
- README의 영문/한글 storage matrix와 migration/rollback 문구는 marker checker로 동기화한다.
- backend allocation claim은 마지막 evidence slice의 canonical JMH 결과가 수렴한 뒤에만
  승격한다.

## 다음 작업자를 위한 경계

caller-visible `limit`과 dependency가 실제로 읽는 bound를 같은 것으로 가정하지 않는다. JNI
wrapper가 error code를 반환한다고 추측하지 말고 resolved version의 source와 실행 예외를 확인한다.
native override에 문제가 생기면 public default와 wire format은 유지하고 해당 override만 fallback으로
되돌린다. core fallback의 PASS를 저할당 근거로 사용하지 않는다.
