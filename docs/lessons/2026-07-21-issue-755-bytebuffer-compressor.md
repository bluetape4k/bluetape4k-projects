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

## Deflate slice에서 고정한 lifecycle과 상태 우선순위

Deflate caller-owned 경로는 매 호출마다 JDK `Deflater` 또는 `Inflater`를 새로 만들고
`ByteBuffer` API에 직접 위임한다. codec을 singleton에 공유하지 않으므로 mutable native state는
호출 사이에 남지 않는다. heap/direct/read-only/slice source와 heap/direct/slice target의 모든
조합은 기존 allocating API와 같은 zlib wire를 교환한다.

압축과 복원 loop는 codec의 `finished`, 입력·출력 byte count, target 잔여량을 매 반복마다
검사한다. target이 소진되면 raw `BufferOverflowException`을 먼저 던진다. 그다음 복원 상태를
사전 요구, 입력 중단, no-progress 순으로 분류해 각각 안정된 `ZipException`으로 보고한다.
`DataFormatException`은 원인을 보존한 `ZipException("Invalid Deflate payload")`로 변환한다.
이 순서 덕분에 zero-capacity target과 손상 상태가 겹쳐도 caller가 먼저 해결해야 할 용량
실패가 결정적으로 유지된다.

codec 정리는 성공과 실패 모두에서 정확히 한 번 실행한다. 연산과 정리가 함께 실패하면 연산
throwable identity를 primary로 유지하고 정리 실패만 suppressed로 추가한다. 연산이 성공했을
때는 정리 실패 자체를 전파한다. 테스트는 per-call codec 생성·정리 횟수, operation-primary
identity, cleanup-only identity, compression/decompression no-progress, overflow 후 같은 target
재시도, singleton 동시 호출을 고정한다. README의 `optimized`는 JDK `ByteBuffer` 직접 dispatch와
storage coverage만 뜻하며 allocation 개선은 아직 측정하지 않았으므로
`eligible, not yet measured`로 유지한다.

## Snappy slice에서 고정한 출력 상한과 validation 경계

snappy-java 1.1.10.8은 direct `ByteBuffer` 쌍과 array offset API를 모두 제공하지만 두 API의
안전성 계약은 같지 않다. array compression API는 target offset만 받고 caller
`ByteBuffer.remaining()`을 출력 길이로 받지 않는다. backing array에 limit 뒤 여유 공간이
있으면 caller가 공개하지 않은 범위까지 쓸 수 있으므로 heap→heap을 optimized로 분류하지
않았다. direct→direct만 native 경로를 사용하고 heap/mixed 조합은 compatibility fallback을
유지한다.

direct compression도 `Snappy.maxCompressedLength(source.remaining())` 전체를 target이 수용할
때만 native 경로를 선택한다. 다만 이 안전 상한보다 작다는 이유만으로 곧바로 overflow를
던지면 실제 압축 결과가 target에 들어가던 기존 fallback 성공을 깨뜨린다. 따라서 상한이
부족하면 allocating fallback으로 내려가고, 실제 결과가 target을 넘을 때만
`BufferOverflowException`을 유지한다.

direct decompression은 정확한 caller-visible source range를 먼저 검증한 뒤 복원 크기와 기존
256 MiB 한도, target remaining을 순서대로 확인한다. invalid payload는
`IllegalArgumentException`으로 거부하고 native decode를 호출하지 않는다. duplicate view에서
native 연산을 실행해 원본 source/target limit을 보존하며, singleton 동시 호출 테스트로
호출 간 mutable codec state가 없음을 고정한다. README의 `optimized`는 이 제한된 native
dispatch capability만 뜻하며 allocation 개선은 아직 측정하지 않았다.

## Zstd slice에서 고정한 declared-size 출력 경계

zstd-jni 1.5.7-11의 heap/direct offset API는 오류 코드를 반환한 뒤 호출자가 검사하는 흐름만
제공하지 않는다. 내부 context가 native 오류를 `ZstdException`으로 바꿔 반환 전에 던질 수
있으므로, 운영 경로는 반환값에 `Zstd.isError`를 다시 적용하지 않고 예외의 `errorCode`를
연산별로 해석한다. 압축 중 `errDstSizeTooSmall`만 raw `BufferOverflowException`으로 바꾸며,
압축 해제 중 같은 코드는 wire header가 실제 payload보다 작은 의미이므로 원인 없는
`IllegalStateException`으로 분류한다. 그 밖의 `ZstdException`은 identity를 그대로 보존한다.

압축은 writable heap끼리와 direct끼리 조합에서 `compressBound + 4` 안전 상한을 만족할 때
caller target의 header 뒤 잔여량을 native destination 길이로 그대로 전달한다. zstd-jni는 실제
결과가 들어갈 만큼의 공간이 있어도 이 안전 상한보다 작은 destination을 거부할 수 있으므로,
그보다 작은 matched-storage target은 기존 성공 가능성을 보존하는 compatibility fallback으로
처리한다. 압축 해제는 target에 더 많은 공간이 있어도 native
destination 길이를 반드시 header의 declared size로 제한한다. 성공 반환값은 declared size와
정확히 같아야 하며 `Long` 상태에서 검증한 뒤에만 `Int`로 축소한다. 이로써 음수,
`Long.MAX_VALUE`, 32-bit 범위를 넘는 합성 반환값도 caller position을 commit하기 전에 거부한다.
mixed-storage와 read-only heap source는 compatibility fallback을 유지한다. README의
`optimized`는 이 offset API dispatch만 뜻하고 allocation 개선은 마지막 evidence slice 전까지
주장하지 않는다.

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
