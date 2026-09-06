# Issue #1643: HTTP 본문 byte 상한 초과 거부 API 설계

## 상태와 범위

- 대상 저장소: `bluetape4k-projects`
- 대상 모듈: `bluetape4k-io`, `bluetape4k-http`
- 대상 버전: `2.1.0`
- 작업 유형: Type A 공개 API 추가
- 기준 ref: `origin/develop` / `6a198fa8461637d02da34fee2d2c4df28a6b7b6e`
- 연결 이슈: [#1643](https://github.com/bluetape4k/bluetape4k-projects/issues/1643)
- 소비자 추적: [bluetape4k-workshop#939](https://github.com/bluetape4k/bluetape4k-workshop/issues/939), [clinic-appointment#451](https://github.com/bluetape4k/clinic-appointment/issues/451)
- 상태: 공개 API와 자원 소유권 설계 승인, 6개 관점 검토 통합

이번 변경은 HTTP 응답 본문이 허용 byte 수를 넘으면 일부 결과를 반환하지 않고
명시적으로 실패하는 공통 기능을 추가한다. 기존 `HttpEntity.toByteArrayOrNull()`과
`toStringOrNull()`의 반환 길이 제한 및 잘림 동작은 변경하지 않는다.

## 1. 문제와 목표

현재 두 소비자가 같은 보안·안정성 경계를 서로 다르게 구현한다.

- Workshop의 HC5 호출자는 `Content-Length`를 먼저 검사한 뒤 `maxBytes + 1`을 읽어
  초과를 판정한다.
- Clinic의 JDK `HttpClient` 호출자는 `InputStream`을 직접 반복해서 읽고 64 KiB 상한을
  검사한다.
- `bluetape4k-http`의 기존 `EntityUtils` adapter는 제한을 넘은 입력을 거부하는 대신
  최대 반환 길이에서 결과를 자를 수 있으므로, JSON 같은 구조화 본문에서 정상 EOF와
  초과 입력을 구분할 수 없다.

목표는 다음과 같다.

1. `InputStream`에 대해 정확한 byte 상한을 적용하는 단일 primitive를 제공한다.
2. HC5 `HttpEntity`와 JDK `HttpResponse<InputStream>` adapter가 같은 primitive를
   재사용한다.
3. `maxBytes` 이하일 때만 전체 결과를 반환하고, 초과 시 부분 payload 없는 전용
   예외를 발생시킨다.
4. adapter가 소유한 본문 스트림을 성공·초과·읽기 실패 모두에서 닫고, 원래 실패와
   close 실패를 함께 보존한다.
5. 실제 읽기량을 항상 검사해 없거나 부정확한 `Content-Length`를 신뢰 경계로 사용하지
   않는다.
6. 기존 잘림 API의 ABI와 동작을 보존하고 두 API 계열의 선택 기준을 KDoc과 README에
   기록한다.

## 2. 범위와 비범위

### 2.1 포함 범위

- `bluetape4k-io`의 bounded `InputStream` 읽기 primitive와 전용 예외
- `bluetape4k-http`의 HC5 nullable entity adapter
- `bluetape4k-http`의 JDK `HttpResponse<InputStream>` adapter
- byte 읽기 완료 후 명시적 `Charset`으로 변환하는 문자열 편의 함수
- 상한 경계, 길이 헤더, read-ahead, 메모리, close, 예외 결합 계약 테스트
- 공개 KDoc, 모듈 README, ABI/API 검증

### 2.2 제외 범위

- 허용 HTTP 상태 코드, 인증·인가, retry, redirect 정책
- JSON 또는 다른 payload 형식의 decode와 schema 검증
- 요청·연결·응답 timeout 설정
- `HttpClient`, `CloseableHttpClient`, 전체 response 객체 종료
- streaming parser, 파일 spill, 압축 해제 후 크기 제한 정책
- 문자 수 제한 또는 malformed charset 입력 거부 정책
- 기존 `toByteArrayOrNull(maxResultLength)`와 `toStringOrNull(maxResultLength)`의 의미 변경
- 소비자 저장소의 실제 전환과 PR. 공통 API 배포 후 각 연결 이슈에서 수행한다.

## 3. 현재 근거와 제약

- `bluetape4k-http`는 `bluetape4k-io`를 `api` dependency로 이미 사용하므로 새 module이나
  dependency가 필요하지 않다.
- 로컬 httpcore5 `5.4.3` 검증에서 기존 `EntityUtils` 기반 함수는 상한을 넘은 입력을
  예외로 거부하지 않고 결과를 잘랐다. 이 계약을 새 기능으로 바꾸면 기존 호출자의
  동작과 호환성을 깨뜨린다.
- JDK `BodyHandlers.ofInputStream()`은 본문을 스트림으로 넘기며 호출자가 읽기 또는
  close를 완료해야 한다. JDK 25의 `BodyHandlers.limiting()`도 사용할 수 있지만 HC5와
  같은 primitive·예외·소유권 계약을 제공하지 않는다.
- `Content-Length`는 없거나 실제 body와 다를 수 있으므로 최적화와 조기 거부에만 쓰고,
  최종 허용 여부는 읽은 byte 수로 판정한다.
- `maxBytes`는 결과의 문자 수가 아니라 adapter에 전달된 body stream이 내놓은 byte
  수다. transport가 투명한 압축 해제를 적용했다면 해제된 byte에, 적용하지 않았다면
  압축된 byte에 상한이 걸린다. 문자열 변환은 byte 제한을 통과한 뒤에만 수행한다.

참고 자료:

- [JDK 25 `HttpResponse.BodyHandlers`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpResponse.BodyHandlers.html)
- [Apache HttpCore `EntityUtils` 공식 소스](https://hc.apache.org/httpcomponents-core-5.3.x/current/httpcore5/xref/org/apache/hc/core5/http/io/entity/EntityUtils.html)

## 4. 대안 비교와 결정

| 대안 | 장점 | 단점 | 결정 |
|---|---|---|---|
| 각 HTTP adapter에서 독립 구현 | 모듈별 코드는 바로 작성 가능 | 경계·예외·read-ahead 동작이 다시 갈라지고 소비자가 HTTP 라이브러리에 종속됨 | 채택하지 않음 |
| 기존 `EntityUtils` adapter 의미 변경 | 공개 함수 수가 늘지 않음 | truncation ABI와 기존 호출자 동작을 깨뜨리고 JDK 경로를 해결하지 못함 | 채택하지 않음 |
| JDK 25 `BodyHandlers.limiting()` 사용 | JDK client 경로의 코드가 짧음 | HC5와 primitive·예외가 분리되고 공통 모듈의 지원 범위를 JDK 25 API에 결합함 | 채택하지 않음 |
| `InputStream` primitive와 얇은 HC5/JDK adapter 추가 | byte 판정과 예외를 한 곳에서 재사용하고 transport별 소유권만 분리 가능 | additive public API와 명시적 테스트가 필요함 | **채택** |

사용자는 2026-09-06에 마지막 대안을 승인했다.

## 5. 공개 API 계약

### 5.1 `bluetape4k-io`

`io.bluetape4k.io` package에 다음 API를 추가한다.

```kotlin
class ByteLimitExceededException(
    val maxBytes: Int,
) : IOException

fun InputStream.readAllBytes(maxBytes: Int): ByteArray
```

계약은 다음과 같다.

- `maxBytes`는 0 이상이어야 한다. 음수이면 읽기 전에 `IllegalArgumentException`을
  발생시킨다. 모든 HTTP adapter도 body와 metadata에 접근하기 전에 같은 검증을 먼저
  수행한다.
- EOF까지 읽은 실제 byte 수가 `maxBytes` 이하이면 정확한 길이의 `ByteArray`를 반환한다.
- 실제 byte 수가 `maxBytes`보다 크면 `ByteLimitExceededException`을 발생시키며, 이미
  읽은 부분 payload를 반환하거나 예외 메시지에 포함하지 않는다.
- 초과 여부를 판정하기 위해 상한 이후 최대 1 byte만 추가로 읽는다. 초과가 확인된 뒤
  남은 stream은 소비하지 않는다.
- 함수는 caller가 소유한 `InputStream`을 닫지 않는다. 성공, 초과, 읽기 실패 모두 같다.
- 초과 판정에 사용한 1 byte는 이미 소비되며 원래 stream으로 되돌리지 않는다. 예외가
  발생한 뒤 stream은 최대 `maxBytes + 1` byte를 소비한 위치에 열린 채로 남는다.
- 원본 stream의 읽기 예외는 감싸지 않고 그대로 전달한다.
- `maxBytes == 0`이면 첫 읽기가 EOF일 때 빈 배열을 반환하고, 1 byte라도 있으면 전용
  예외를 발생시킨다.
- accumulator는 `maxBytes` 크기를 선할당하거나 자동 배수 확장
  `ByteArrayOutputStream`을 사용하지 않는다. 작은 segment에서 시작해 실제로 읽은 만큼만
  점진적으로 늘리고, 모든 segment capacity의 합은 `maxBytes`를 넘지 않는다. 초과 확인
  byte는 단일 정수로만 보관한다. 성공 시 정확한 길이의 결과 배열을 만들기 위한 복사
  때문에 순간 최대 payload 메모리는 `2 * actualBytes + O(segmentSize)` 이내이며 항상
  `O(maxBytes)`다. 입력 크기에 비례해 상한 없이 증가하지 않는다.

`Int.MAX_VALUE`를 포함한 모든 non-negative `Int`는 계약상 허용한다. 그러나 이 함수는
호출자가 지정한 상한만큼의 메모리 가용성을 보장하지 않으며, 실제 body가 JVM의 배열 또는
heap 한계에 가까우면 다른 `ByteArray` 생성 API와 마찬가지로 `OutOfMemoryError`가 발생할
수 있다. adapter에는 안전하지 않은 기본 상한을 두지 않고 caller가 요청 종류와 최대
동시성에 맞는 유한한 `maxBytes`를 반드시 전달하게 한다. 내부 남은 용량 계산은
`maxBytes + 1`을 만들지 않고 subtraction 또는 `Long` 계수로 수행해 정수 overflow를
방지한다. `Int.MAX_VALUE`와 작은 body 조합은 큰 배열을 선할당하지 않아야 한다.

`ByteLimitExceededException`은 허용 상한인 `maxBytes`만 안정된 공개 정보로 제공한다.
생성자는 `maxBytes >= 0` 불변조건을 검사한다. 실제 body 내용, 부분 결과, 마지막 byte는
보관하거나 메시지로 노출하지 않는다. `message`는 사람이 읽을 수 있는 설명이지만 정확한
문구는 호환성 계약이 아니며 caller는 type과 `maxBytes`만 사용한다.

### 5.2 HC5 adapter

`io.bluetape4k.http.hc5.entity` package에 다음 API를 추가한다.

```kotlin
fun HttpEntity?.readBodyBytes(maxBytes: Int): ByteArray

fun HttpEntity?.readBodyString(
    maxBytes: Int,
    charset: Charset = Charsets.UTF_8,
): String
```

계약은 다음과 같다.

- null entity와 콘텐츠가 없는 entity는 빈 `ByteArray` 또는 빈 문자열을 반환한다. 이는
  새 strict adapter가 “읽을 body 없음”을 성공한 0-byte body로 정규화하는 의도된 계약이다.
  body 부재와 빈 body를 구분해야 하는 caller는
  `entity?.let { it.readBodyBytes(maxBytes) }`를 사용해 null을 보존한다.
- 음수 `maxBytes`는 entity metadata나 content에 접근하기 전에 거부한다. 이 경우
  adapter가 body 소유권을 얻지 않았으므로 닫지 않는다.
- `contentLength >= 0`이고 `contentLength > maxBytes`이면 body를 읽지 않고 전용 예외로
  조기 거부한다. overflow 예외를 primary로 만든 뒤 content stream 획득과 close를
  시도하며, 획득 또는 close 실패는 overflow 예외의 `suppressed`로 보존한다. content
  accessor가 null을 반환하거나 실패해 stream을 얻지 못하면 adapter가 더 닫을 자원은
  없다.
- 길이가 unknown이거나 `maxBytes` 이하로 선언됐어도 공통 primitive로 실제 byte 수를
  검사한다. 헤더보다 실제 body가 길면 전용 예외로 실패한다.
- adapter는 entity content stream을 소유하며 성공·초과·읽기 실패에서 항상 닫는다.
- 읽기 또는 초과 예외가 primary failure이고 close도 실패하면 close 예외를
  `suppressed`로 보존한다. 읽기는 성공했지만 close만 실패하면 close 예외를 전달한다.
- 문자열 함수는 bounded byte 읽기가 성공한 뒤 지정된 `Charset`으로 변환한다. 기본값은
  UTF-8이며 문자 수를 별도로 제한하지 않는다.
- 함수 전체는 blocking이며 coroutine cancellation을 직접 지원하지 않는다. caller는
  transport의 connect/response/read timeout을 설정하고, 취소 시 해당 response 또는
  stream을 닫을 수 있는 수명주기를 구성해야 한다. `Dispatchers.IO` 사용은 호출 스레드
  격리일 뿐 underlying I/O 중단을 보장하지 않는다.
- known oversize의 “읽지 않음”은 content byte에 대한 보장이다. content accessor와
  close는 lazy transport 동작에 따라 blocking될 수 있으며 adapter는 비차단 조기 거부를
  보장하지 않는다.

### 5.3 JDK `HttpResponse<InputStream>` adapter

`io.bluetape4k.http.jdk` package에 다음 API를 추가한다.

```kotlin
fun HttpResponse<InputStream>.readBodyBytes(maxBytes: Int): ByteArray

fun HttpResponse<InputStream>.readBodyString(
    maxBytes: Int,
    charset: Charset = Charsets.UTF_8,
): String
```

계약은 다음과 같다.

- 음수 `maxBytes`는 response header나 body에 접근하기 전에 거부한다. 이 경우 adapter가
  body 소유권을 얻지 않았으므로 닫지 않는다.
- `Content-Length` header가 정확히 하나이고 overflow 없이 non-negative `Long`으로
  해석되며 `maxBytes`보다 크면 body를 읽지 않고 전용 예외로 조기 거부한다. header가
  없거나, 여러 개이거나, malformed·negative·`Long` overflow이면 metadata를 unknown으로
  취급하고 실제 body를 읽어 판정한다. HTTP framing 자체의 유효성·smuggling 방어는 JDK
  client와 앞단 proxy의 책임이다.
- header가 없거나 실제 길이와 달라도 공통 primitive로 실제 byte 수를 검사한다.
- adapter는 `response.body()`가 반환한 `InputStream`을 소유하며 성공·초과·읽기 실패에서
  항상 닫는다.
- primary failure와 close failure의 결합 규칙은 HC5 adapter와 같다.
- adapter는 응답 status를 해석하지 않는다. 2xx 외 body도 caller가 호출하면 같은 규칙으로
  읽는다.
- adapter는 `HttpClient`, caller의 executor, request 또는 response 객체 자체를 닫지 않는다.
- 문자열 함수는 byte 제한 성공 후 지정된 `Charset`으로 변환한다.
- 함수 전체는 blocking/non-cancellable이며 caller timeout과 취소 책임은 HC5 adapter와
  같다. body accessor가 stream을 반환하기 전에 실패하면 그 예외가 primary이며 adapter가
  닫을 stream은 없다. known oversize에서는 overflow 예외를 primary로 만든 뒤 body
  accessor와 close 실패를 `suppressed`로 보존한다.

## 6. 내부 구현 경계

1. `InputStream.readAllBytes(maxBytes)`가 유일한 byte-limit 판정 primitive다.
2. primitive는 상한 안에서만 segment를 점진 할당한다. 누적 byte가 `maxBytes`에 도달하면
   `read()`를 정확히 한 번 호출해 EOF와 초과를 구분하며 `maxBytes + 1` 산술을 사용하지
   않는다. 양의 길이 bulk read가 0을 반환하면 즉시 단일 `read()`로 전환한다. 단일
   `read()`는 EOF, 예외, 또는 한 byte 중 하나를 반환하므로 0-byte retry loop를 만들지
   않는다.
3. HC5/JDK adapter는 길이 metadata를 조기 거부에만 사용하고, 허용 후보는 반드시
   primitive에 전달한다.
4. adapter의 close는 Kotlin `use`와 동일한 primary/suppressed 규칙을 따른다. 조기 거부는
   overflow 예외를 먼저 만든 뒤 body accessor와 close를 시도해 cleanup 실패가 거부
   원인을 가리지 않게 한다. body accessor 실패 전에는 stream 소유권을 얻지 못하므로
   transport 전체 abort를 보장하지 않는다. caller는 response/client별 상위 cleanup
   경계를 별도로 소유한다.
5. 문자열 함수는 byte 함수에만 위임한다. 별도의 reader 기반 제한 로직을 만들지 않는다.
6. 기존 `toByteArrayOrNull`, `toStringOrNull`, `consume`, `consumeQuietly`는 수정하지 않는다.
7. 이 helper는 그 자체로 decompression bomb 방어 경계가 아니다. caller는 transport의
   투명 압축 해제 여부를 확인하고, helper 뒤에서 압축을 해제한다면 해제 결과에도 별도
   byte 상한을 적용한다. 압축 정책을 알 수 없는 untrusted 응답은 decode 전에 거부한다.
8. 정확히 상한만큼 읽은 뒤 EOF 또는 다음 byte를 확인하는 마지막 `read()`도 blocking될
   수 있다. helper 내부 deadline은 없으며 caller가 transport timeout과 외부 수명주기로
   제한한다.

### 6.1 Adapter 자원 상태 전이

| 단계 | 동작 | primary failure | cleanup |
|---|---|---|---|
| 상한 검증 실패 | metadata/body accessor 미호출 | `IllegalArgumentException` | adapter가 얻은 자원 없음 |
| known oversize | `ByteLimitExceededException`을 먼저 생성하고 body accessor 호출, content byte는 읽지 않음 | overflow 예외 | 획득한 stream close, accessor/close 실패는 suppressed |
| 허용 또는 unknown 길이 | body accessor 뒤 공통 primitive 호출 | accessor/read/overflow 중 먼저 발생한 실패 | 획득한 stream close, close 실패는 suppressed |
| 읽기 성공 | 정확한 결과 생성 | close가 실패하면 close 예외 | stream close 후에만 결과 반환 |
| accessor가 stream 반환 전 실패 | body 읽기 미시작 | accessor 예외. known oversize이면 overflow가 primary | adapter가 닫을 stream 없음, caller의 상위 response cleanup 필요 |

소유권은 body accessor가 stream을 반환한 시점에 adapter로 이전된다. adapter는 반환받은
stream의 close를 정확히 한 번 시도한다. HC5 response나 JDK `HttpClient` 전체 수명주기는
이전되지 않는다.

### 6.2 Bounded completion 운영 계약

이 API는 동기·blocking helper이므로 자체적으로 완료 시간을 제한할 수 없다. bounded
completion이 필요한 application은 다음 조건을 모두 충족해야 한다.

1. HTTP client가 제공하는 connect/response/socket/read timeout을 적용한다.
2. helper 호출을 event-loop가 아닌 blocking I/O 실행 경계에 둔다.
3. 외부 deadline이 만료되면 supervisor가 body stream 또는 상위 response를 닫아 진행 중
   read를 중단할 수 있게 수명주기를 소유한다. coroutine job 취소만으로는 충분하지 않다.
4. close 자체가 멈출 수 있는 transport라면 client pool의 eviction/abort 기능과 운영
   timeout을 사용한다. 이 범용 adapter는 transport별 강제 abort API를 호출하지 않는다.

단위 테스트는 close될 때까지 block하는 fake stream을 별도 task에서 읽고 supervisor
close로 종료되는지 검증한다. 실제 client timeout 설정 예시는 README에 제공하되, 특정
client 버전의 timeout이 body EOF까지 포함한다고 근거 없이 보장하지 않는다.

## 7. 실패 모드와 예상 동작

| 실패 모드 | 예상 동작 | 자원 상태 |
|---|---|---|
| 입력이 `maxBytes - 1` 또는 `maxBytes` | 전체 byte 반환 | primitive는 열어 둠, adapter는 닫음 |
| 입력이 `maxBytes + 1` 이상 | 1 byte read-ahead 후 `ByteLimitExceededException` | primitive는 판정 byte까지 소비한 위치에 열어 둠, adapter는 닫음 |
| known `Content-Length > maxBytes` | body를 읽지 않고 조기 거부 | adapter가 body stream을 닫음 |
| known `Content-Length <= maxBytes`, 실제 body 초과 | 실제 읽기에서 전용 예외 | adapter가 body stream을 닫음 |
| unknown `Content-Length`, 실제 body 초과 | 실제 읽기에서 전용 예외 | adapter가 body stream을 닫음 |
| null HC5 entity 또는 빈 body | 빈 결과 | 열 자원 없음 또는 열린 body를 닫음 |
| stream read 실패 | 원래 read 예외 전달 | adapter는 close 시도 |
| 초과/read 실패 뒤 close도 실패 | 원래 예외에 close 예외를 suppressed로 추가 | close 시도 완료 |
| 읽기 성공 뒤 close만 실패 | close 예외 전달, 성공 결과 미반환 | close 시도 완료 |
| UTF-8 다중 byte 문자의 byte 상한 초과 | 문자 수와 무관하게 전용 예외 | adapter는 닫음 |
| 음수 `maxBytes` | body/metadata 접근 전 `IllegalArgumentException` | primitive는 열어 둠, adapter는 body 소유권을 얻지 않음 |
| malformed·중복·overflow `Content-Length` | metadata를 unknown으로 취급하고 실제 byte 검사 | adapter는 body를 닫음 |
| body accessor 실패 | accessor 예외 전달. known oversize이면 overflow 예외에 suppressed | 획득한 stream이 없으므로 상위 cleanup 필요 |
| 상한 byte 뒤 peer가 멈춤 | 마지막 판정 read가 transport timeout까지 blocking | caller가 timeout·취소·상위 cleanup 소유 |
| helper 뒤 압축 해제 결과가 팽창 | 이 helper만으로 차단되지 않음 | caller가 decoded byte 상한 적용 |

## 8. 테스트 전략

### 8.1 공통 primitive

`InputStreamSupportTest` 또는 전용 `BoundedInputStreamSupportTest`에서 다음을 검증한다.

- `1 <= maxBytes < Int.MAX_VALUE`에서 `maxBytes - 1`, `maxBytes`, `maxBytes + 1`을,
  별도 사례에서 `maxBytes == 0`을 검증
- 생성형 stream이 초과 시 정확히 `maxBytes + 1`까지만 제공했는지 추적
- `Int.MAX_VALUE` 상한과 작은 body에서 큰 배열을 선할당하지 않고 정수 overflow 없이
  완료하는지 확인
- 작은 body/큰 상한과 상한에 가까운 body에서 segment capacity 합이 상한을 넘지 않는지
  구현 불변식과 코드 검토로 확인한다. tracking stream은 read 수와 결과 크기만 검증하며
  JVM 배열 allocation을 관찰할 수 있다고 주장하지 않는다.
- 성공·초과·read 실패에서 primitive가 stream을 닫지 않음
- 초과 후 stream이 판정 byte까지 소비된 위치에 있고 나머지는 읽지 않았는지 확인
- read 실패 인스턴스와 stack/cause를 그대로 보존
- 전용 예외가 부분 payload를 필드나 메시지로 노출하지 않음
- `read(byteArray)`가 반복적으로 0을 반환해도 단일 `read()` 전환으로 진행하며 bulk retry
  loop를 만들지 않음

### 8.2 HC5 adapter

`HttpEntitySupportTest`에서 다음을 검증한다.

- known/unknown `Content-Length`의 경계값
- header보다 실제 body가 짧거나 긴 mismatch
- null entity, empty entity
- 음수 상한이 metadata/content 접근 전에 실패함
- 조기 거부 시 read 0회와 close 1회
- 조기 거부 시 body accessor/close 실패가 overflow 예외에 suppressed로 보존됨
- 성공·초과·read 실패 시 close 1회
- read/overflow failure가 primary이고 close failure가 suppressed인지 확인
- UTF-8 다중 byte 입력을 byte 기준으로 거부
- 기존 truncation 함수가 이전처럼 잘린 결과를 반환하는 회귀

### 8.3 JDK adapter

네트워크나 container 없이 동작하는 fake `HttpResponse<InputStream>`을 사용한
`JdkHttpResponseSupportTest`를 추가한다.

- header 없음, 정상 길이, 실제 길이 mismatch
- malformed·negative·overflow·중복 `Content-Length`를 unknown으로 처리
- 경계값과 조기 거부 read/close 추적
- 성공·초과·read 실패·close 실패 결합
- status code와 무관하게 같은 body 규칙 적용
- adapter가 body stream 외의 client/executor 수명주기를 건드리지 않음
- UTF-8 다중 byte 입력의 byte 기준 판정
- 정확히 상한까지 전달한 뒤 멈춘 stream이 caller가 설정한 transport timeout으로 종료됨
- close될 때까지 block하는 fake stream이 외부 supervisor close로 종료되고 adapter가
  double-close하지 않음

### 8.4 공개 문서 예제

KDoc와 README는 소유권 차이가 드러나는 다음 형태를 컴파일 가능한 import와 함께 제공한다.

```kotlin
// raw InputStream은 caller가 소유한다.
val bytes = inputStream.use { it.readAllBytes(maxBytes = 64 * 1024) }

// HC5 adapter는 entity content만 닫는다. response 자체는 caller가 닫는다.
httpClient.execute(request).use { response ->
    val body = response.entity.readBodyBytes(maxBytes = 64 * 1024)
}

// JDK adapter는 BodyHandlers.ofInputStream()이 만든 body stream을 닫는다.
val response = jdkClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
val body = response.readBodyBytes(maxBytes = 64 * 1024)
```

호출자는 필요하면 `ByteLimitExceededException`을 별도로 catch하고, 예외 `message`가 아닌
type과 `maxBytes`로 분기한다. coroutine/event-loop 예제는 직접 호출을 금지하고 blocking
I/O dispatcher, client timeout, 외부 close 수명주기를 함께 보여 준다.

### 8.5 검증 순서

1. 새 계약 테스트를 먼저 작성해 RED를 확인한다.
2. `bluetape4k-io` primitive를 구현하고 해당 테스트를 GREEN으로 만든다.
3. HC5/JDK adapter 테스트를 RED로 확인한 뒤 공통 primitive 위임으로 GREEN을 만든다.
4. module-scoped test와 check를 실행한다.
5. public ABI/API dump와 문서 검사, `git diff --check`를 실행한다.
6. Docker image가 필요한 기존 JDK 통합 테스트 실패는 변경 코드와 분리해 기록하되,
   신규 adapter 검증은 container 없는 단위 테스트로 완결한다.

## 9. 호환성과 소비자 전환

- 모든 API는 additive다. 기존 public JVM descriptor와 기본 동작을 변경하지 않는다.
- `toByteArrayOrNull(maxResultLength)`와 `toStringOrNull(maxResultLength)`는 반환 길이를
  제한하거나 자를 수 있는 기존 용도로 유지한다. 입력 초과를 오류로 다뤄야 하는
  caller만 새 `readBody*` API를 사용한다.
- `InputStream.readAllBytes()`라는 JDK member와 새 `readAllBytes(maxBytes)` extension은
  인자 수가 달라 source 호출이 명확하다. 기존 무인자 호출은 영향을 받지 않는다.
- 새 예외는 `IOException` 하위 타입이므로 기존 I/O 실패 처리와 결합할 수 있다. 초과를
  구분해야 하는 caller만 구체 타입을 먼저 처리한다.
- Workshop과 Clinic 전환은 공통 라이브러리 버전이 제공된 뒤 각 이슈에서 수행한다.
  두 소비자는 수동 loop를 제거하되 status/auth/retry/decode 정책은 그대로 소유한다.
- JDK 25 전용 handler를 사용하지 않으므로 모듈의 기존 target과 binary 계약을 별도로
  높이지 않는다.
- `maxBytes`는 신뢰되지 않은 원격 입력에서 직접 정하지 않는다. application이 요청 종류,
  동시성, heap 예산에 맞춘 유한한 상한을 설정하고 외부 사용자가 이를 늘릴 수 없게 한다.
- application의 최악 payload 예산은 최소한
  `동시 bounded read 수 * (2 * maxBytes + segment overhead)`를 기준으로 잡고, 허용 heap
  예산을 넘는 설정은 startup validation에서 거부한다. library는 배포 환경의 동시성과
  heap을 알 수 없으므로 임의의 전역 최대값을 강제하지 않는다.
- 제한 계층은 adapter에 실제 전달된 stream이다. transparent content decoding과 후속
  decompression을 사용하는 application은 decoded 결과 상한을 별도로 검증한다.

호출자 선택 기준은 다음과 같다.

| 요구 | 사용할 API | 결과 |
|---|---|---|
| 미리보기·진단처럼 의도적으로 앞부분만 필요 | 기존 `toByteArrayOrNull(maxResultLength)` 또는 `toStringOrNull(...)` | 상한에서 잘릴 수 있음 |
| JSON/schema처럼 전체 body가 상한 안에 있어야 유효 | 새 `readBodyBytes` 또는 `readBodyString` | 초과 시 전체 실패 |
| null HC5 entity와 빈 body 구분 필요 | `entity?.let { it.readBodyBytes(maxBytes) }` | null 보존 |
| caller-owned 일반 stream | `input.use { it.readAllBytes(maxBytes) }` | caller가 close |

Workshop #939는 수동 `readNBytes(maxBytes + 1)`을 HC5 strict adapter로 바꾸되 status와
JSON decode는 기존 계층에 남긴다. Clinic #451은 수동 JDK stream loop를 JDK strict
adapter로 바꾸되 status/auth/schema decode 정책은 기존 계층에 남긴다.

### 9.1 배포와 rollback 순서

1. 공통 라이브러리 `2.1.0` API와 ABI 검증을 완료하고 publish한다.
2. 각 예제 저장소가 중앙 catalog 또는 허용된 repository-local dependency override로 새
   버전을 선택하고 compile·targeted smoke test를 수행한다.
3. Workshop #939와 Clinic #451을 각각 독립 PR로 전환한다.
4. 소비자 회귀가 발생하면 dependency와 수동 strict loop를 이전 버전으로 되돌린다.
   기존 truncation API는 strict 거부 semantics가 다르므로 rollback 대체재로 사용하지
   않는다.
5. 두 소비자 전환이 완료될 때까지 기존 수동 구현 이슈를 닫지 않는다.

library helper는 log, metric, tracing side effect를 만들지 않는다. application은 payload를
기록하지 않고 endpoint/operation과 설정된 상한 같은 low-cardinality 정보만 사용해
`ByteLimitExceededException`, read failure, suppressed close failure를 구분 집계한다. 정상
트래픽 대비 초과율 또는 연속 close 실패가 운영 임계치를 넘으면 upstream 계약 위반,
공격, 잘못된 상한 설정을 조사한다.

## 10. 수용 기준과 DoD

- [ ] 공개 API signature와 KDoc가 이 문서의 byte·소유권 계약과 일치한다.
- [ ] `1 <= maxBytes < Int.MAX_VALUE`에서 `maxBytes - 1`, `maxBytes`, `maxBytes + 1`을,
  별도 사례에서 0과 음수 경계를 검증한다.
- [ ] `Int.MAX_VALUE` 상한과 작은 body가 선할당·정수 overflow 없이 처리된다.
- [ ] known/unknown/mismatch `Content-Length`, empty body, null HC5 entity가 검증된다.
- [ ] malformed·negative·overflow·중복 `Content-Length`가 unknown metadata로 처리된다.
- [ ] 초과 판정의 최대 read-ahead가 1 byte임이 검증된다.
- [ ] accumulator가 `maxBytes`를 넘지 않고 전체 메모리가 `O(maxBytes)`임이 검증된다.
- [ ] 성공·초과·read 실패·close 실패의 자원 및 예외 결합 규칙이 검증된다.
- [ ] body accessor 실패, blocking read timeout, 압축 계층의 호출자 책임이 문서와 테스트에
  반영된다.
- [ ] read-ahead 후 primitive stream 위치와 null/empty 정규화의 migration 계약이
  검증된다.
- [ ] UTF-8 다중 byte 입력이 문자 수가 아닌 byte 수로 제한된다.
- [ ] HC5와 JDK adapter가 같은 `InputStream.readAllBytes(maxBytes)`를 재사용한다.
- [ ] 기존 truncation API의 ABI와 동작이 유지된다.
- [ ] KDoc와 `bluetape4k-io`/`bluetape4k-http` README가 사용 선택 기준을 설명한다.
- [ ] public KDoc/README 예제가 소유권, blocking 실행 경계, 예외 처리와 strict/truncation
  선택을 컴파일 가능한 형태로 설명한다.
- [ ] targeted test, module test/check, ABI/API 검사, `git diff --check`가 통과한다.
- [ ] 독립 검토의 P0/P1 finding이 없고 P2/P3은 해결하거나 명시적으로 처분한다.
- [ ] Workshop #939와 Clinic #451에 공통 API 가용성과 후속 전환 근거를 남긴다.
- [ ] library publish, 소비자 smoke, 독립 전환, rollback 순서와 payload 없는 관측 지침이
  확인된다.
- [ ] PR 본문과 이슈가 수용 기준·검증 증거를 연결하며 CI가 통과한다.

## 11. 결정되지 않은 항목

없음. status 정책, JSON decode, streaming parser, 소비자 전환은 의도적으로 별도 책임과
후속 이슈에 남긴다.
