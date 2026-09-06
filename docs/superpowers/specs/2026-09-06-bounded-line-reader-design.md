# Issue #1642 길이 제한 Reader API 설계

## 1. 상태와 범위

- 대상 저장소: `bluetape4k-projects`
- 대상 모듈: `bluetape4k-io`
- 대상 버전: `2.1.0-SNAPSHOT`
- 작업 유형: Type A 공개 API 추가
- 기준 ref: `origin/develop@9811932427aac4c6cfba7b16ae3def215e93a2fe`
- 연결 이슈: [#1642](https://github.com/bluetape4k/bluetape4k-projects/issues/1642)
- 후속 소비자: [bluetape4k-graph#615](https://github.com/bluetape4k/bluetape4k-graph/issues/615)
- 이번 PR: 공통 provider API와 단위 테스트·문서만 제공한다. Graph consumer 코드는
  수정하지 않는다.

Graph의 Jackson2·Jackson3 NDJSON 입력기는 `Reader.readLine()`으로 한 줄을 먼저
문자열로 만든다. 신뢰할 수 없는 입력이 길어지면 JSON parsing 전에 한 줄 전체를
할당하므로, 호출자가 선택한 UTF-16 문자 수 상한을 읽는 동안 적용하는 재사용 가능한
Reader 경계가 필요하다.

이번 API는 JSON, NDJSON, 행 번호, codec, source lifecycle을 알지 못한다. 소비자는
자신이 소유한 `Reader`를 wrapper에 제공하고, 반환된 줄을 domain parser에 전달하며,
wrapper가 닫히지 않으므로 원본 `Reader`의 close를 직접 책임진다.

## 2. 목표와 성공 조건

다음 계약을 만족하는 공개 API를 추가한다.

1. `Reader`를 감싼 `BoundedLineReader`가 반복해서 한 줄씩 반환한다.
2. `maxLineChars`는 UTF-8 byte나 Unicode code point가 아니라 UTF-16 code unit 수로
   계산한다. 따라서 supplementary character 하나는 두 code unit으로 센다.
3. `maxLineChars` 이하의 줄은 `String`으로 반환한다. `maxLineChars`를 넘는
   첫 code unit을 읽는 즉시 `LineLimitExceededException`을 발생시킨다.
4. line terminator는 LF(`\n`), CRLF(`\r\n`), CR(`\r`)을 모두 지원한다. terminator는
   길이 상한에 포함하지 않는다.
5. 빈 입력은 첫 호출에서 `null`, 빈 줄은 `""`, 마지막 개행이 없는 마지막 줄은
   문자열을 반환하고 다음 호출에서 `null`을 반환한다.
6. 내부 char buffer는 고정 크기이고, 한 번의 underlying `Reader.read` 요청은 현재
   줄의 허용량과 buffer 크기 중 작은 값으로 제한한다. 줄 초과 판정은 최대
   `maxLineChars + 1` code unit을 읽은 시점에 끝나며, CR terminator 판별을 위한
   추가 read-ahead도 한 code unit으로 제한한다.
7. wrapper는 원본 `Reader`를 닫지 않고, 원본 읽기 예외를 감싸지 않고 그대로 전파한다.
8. public KDoc, 양 언어 module README 예제, API 색인과 테스트가 이 계약을 설명하고
   고정한다.

성공 조건은 API가 긴 입력을 전체 할당하지 않고 거부하며, 여러 줄의 경계와 UTF-16
   단위가 정확하고, underlying Reader의 close 책임과 예외 identity가 보존되는 것을
   테스트로 증명하는 것이다.

## 3. 현재 근거

### 3.1 저장소와 기존 helper

- `io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt`는
  `ByteLimitExceededException`과 `InputStream.readAllBytes(maxBytes)`를 제공한다.
  입력 stream을 닫지 않고, 고정 segment와 최대 1 byte의 overflow 판정 read를 사용한다.
  새 Reader API는 이 caller-owned와 bounded read-ahead 원칙을 차용하되, 문자 단위와
  줄 terminator가 필요한 별도 상태 machine으로 둔다.
- `io/io/src/main/kotlin/io/bluetape4k/io/InputStreamSupport.kt`의
  `DEFAULT_BUFFER_SIZE`와 `requirePositiveNumber`는 기존 buffer/검증 규칙이다.
- `io/io/README.md`와 `io/io/README.ko.md`는 같은 구조의 사용 예제를 가진다.
- Okio의 `BufferedSuspendedSource.readUtf8LineStrict(limit)`는 UTF-8 byte 단위이고
  CR 단독 및 개행 없는 EOF 계약이 달라 이번 Reader helper를 대체하지 못한다.

### 3.2 소비자와 경계

- Graph의 `Jackson2RecordParser`와 `Jackson3RecordParser`는 각각 표준
  `reader.readLine()`을 사용한다. 공통 IO 모듈이 제공할 수 있는 것은 줄 반환과
  상한뿐이며, parser의 JSON 오류·행 번호·입력 source 소유권은 후속 consumer PR이
  유지한다.
- [Projects #1642](https://github.com/bluetape4k/bluetape4k-projects/issues/1642)는
  기존 unbounded API를 호환성 없이 교체하지 않는 opt-in API를 요구한다.
- [Graph #615](https://github.com/bluetape4k/bluetape4k-graph/issues/615)는 provider
  merge와 artifact 가용성 이후 Jackson2·Jackson3 양쪽에 같은 helper를 적용한다.

## 4. 대안 비교와 결정

### 4.1 `Reader.readLine(maxLineChars)` extension

호출이 짧고 기존 `readLine()`과 비슷하지만, 호출마다 새로운 상태를 만들면 CR 뒤의
다음 code unit을 안전하게 보관할 수 없다. mutable state를 `Reader`에 저장할 수도
없으므로 반복 호출 API로는 lifecycle과 read-ahead 계약이 불명확하다.

결정: 채택하지 않는다.

### 4.2 `Sequence<String>` 또는 callback 소비 API

한 번의 생성으로 상태를 유지할 수 있지만, lazy sequence의 iterator 종료·예외·원본
Reader close 시점을 함께 정의해야 한다. Graph 소비자는 기존 `while` loop와 행별
parser 오류 흐름을 유지해야 하므로 새로운 iterator lifecycle이 불필요하게 커진다.

결정: 채택하지 않는다.

### 4.3 `BoundedLineReader` 상태 보유 wrapper

Reader와 상한을 한 번만 검증하고 반복 `readLine()` 호출을 제공한다. 고정 buffer,
CR lookahead, caller-owned close를 한 객체 안에서 명시할 수 있으며, JSON domain을
추가하지 않는다. `bufferSize`를 선택 인자로 노출하면 소비자는 read-ahead를 작은
값으로 조정할 수 있고 테스트는 실제 읽기량을 측정할 수 있다.

결정: **채택**. public class와 factory extension을 additive API로 제공한다.

## 5. 선택한 공개 API

```kotlin
package io.bluetape4k.io

class LineLimitExceededException(
    val maxLineChars: Int,
) : IOException

class BoundedLineReader(
    reader: Reader,
    maxLineChars: Int,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
) {
    fun readLine(): String?
}

fun Reader.boundedLineReader(
    maxLineChars: Int,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): BoundedLineReader
```

### 5.1 입력 검증

- `maxLineChars`는 0 이상이어야 한다. 음수이면 underlying Reader에 접근하기 전에
  `IllegalArgumentException`을 발생시킨다.
- `bufferSize`는 양수여야 한다. 0 이하이면 underlying Reader에 접근하기 전에
  `IllegalArgumentException`을 발생시킨다.
- 검증에는 `requireZeroOrPositiveNumber("maxLineChars")`와
  `requirePositiveNumber("bufferSize")`를 사용하고, 검증된 값을 내부 field에
  저장한다.
- `maxLineChars == 0`은 빈 줄만 허용한다. 첫 code unit이 LF/CR이면 빈 문자열을
  반환하고, 일반 code unit이면 즉시 전용 예외를 발생시킨다.

### 5.2 줄 반환과 EOF

- `readLine()`은 입력을 다음 LF, CRLF, CR 또는 EOF까지 읽는다.
- 입력이 시작부터 EOF이면 `null`을 반환한다.
- terminator 직전 code unit이 없으면 빈 문자열을 반환한다.
- EOF 전에 한 개 이상의 code unit을 읽었으면 terminator가 없어도 문자열을 반환한다.
- 마지막 줄을 반환한 뒤 다음 호출은 `null`이다.
- wrapper의 `readLine()`은 원본 Reader를 닫지 않는다. caller는 다음처럼 원본을
  직접 닫는다.

```kotlin
reader.use {
    val lines = it.boundedLineReader(maxLineChars = 64 * 1024)
    while (true) {
        val line = lines.readLine() ?: break
        consume(line)
    }
}
```

### 5.3 UTF-16 상한

Kotlin/JVM `Char` 하나를 하나의 code unit으로 센다. 문자열의 surrogate pair를
분리해 code point로 합치거나 정규화하지 않는다.

- `"abcd"`는 4 code unit이다.
- `"😀"`는 2 code unit이다.
- `maxLineChars == 2`에서 `"😀"`은 성공한다.
- `maxLineChars == 1`에서 `"😀"`은 low surrogate를 읽는 순간 실패한다.

상한을 넘은 줄의 부분 `String`은 반환하지 않으며 예외 message나 property에 payload를
저장하지 않는다. `LineLimitExceededException.maxLineChars`만 분기 가능한 안정된
정보이며 message의 정확한 문구는 호환성 계약이 아니다.

### 5.4 read-ahead와 resource ownership

내부 buffer는 생성 시 `bufferSize` 길이로 한 번 할당한다. 현재 줄 길이를 `length`라
할 때 bulk read 요청 길이는 다음 값이다.

```kotlin
minOf(bufferSize.toLong(), maxLineChars.toLong() - length + 1L).toInt()
```

`+1`은 최대 허용 길이 다음 code unit을 읽어 overflow를 판정하기 위한 값이며, `Long`
산술로 `Int.MAX_VALUE` overflow를 피한다. newline 또는 EOF가 먼저 나오면 그 지점에서
반환한다. CR을 읽은 뒤 다음 code unit이 buffer에 없으면 underlying `Reader.read()`를
한 번만 호출해 LF 여부를 판정하고, LF가 아니면 다음 `readLine()`을 위해 pending
code unit으로 보관한다. 따라서 줄 초과 시 입력 소비는 `maxLineChars + 1` 이내이고,
terminator 판정 read-ahead를 포함해도 buffer 외부에서 무제한으로 읽지 않는다.

`BoundedLineReader`는 `Closeable`을 구현하지 않는다. wrapper가 원본 Reader를 소유하지
않는다는 사실을 API surface에서 드러내고, 성공·EOF·전용 예외·원본 IOException
경로에서 close를 시도하지 않는다.

## 6. 내부 동작

상태는 `buffer`, `bufferIndex`, `bufferLimit`, `pendingChar` 네 부분으로 제한한다.

1. `readLine()` 시작 시 `StringBuilder`를 `minOf(maxLineChars, bufferSize)` capacity로
   만든다. 상한이 `Int.MAX_VALUE`여도 상한 크기를 선할당하지 않는다.
2. pending code unit이 있으면 먼저 소비한다. 없으면 buffer에 남은 code unit을
   사용하고, 비어 있으면 현재 줄에 남은 허용량을 고려한 bulk read를 수행한다.
3. bulk read가 0을 반환하면 `Reader.read()`로 한 code unit을 요청한다. `Reader.read()`
   의 0은 NUL code unit이므로 유효한 입력으로 처리한다.
4. LF이면 줄을 반환한다. CR이면 pending/buffer/단일 read-ahead로 LF를 소비하고 줄을
   반환하거나 다음 code unit을 pending에 둔다.
5. terminator가 아닌 code unit을 읽기 전에 `lineLength == maxLineChars`이면
   `LineLimitExceededException`을 발생시킨다. 아니면 builder에 append한다.
6. EOF에서 builder가 비어 있으면 `null`, 그렇지 않으면 builder 문자열을 반환한다.

이 알고리즘은 입력에 비례해 내부 buffer와 현재 허용 상한까지만 사용한다. 단일 줄의
허용 결과 문자열 자체는 상한까지 커질 수 있으며, caller가 동시에 처리하는 줄 수의
메모리 예산은 caller가 책임진다.

## 7. 실패 모드와 책임 경계

| 상황 | 계약 | 검증 |
|---|---|---|
| `maxLineChars < 0` | 읽기 전에 `IllegalArgumentException` | tracking Reader 호출 수 0 |
| `bufferSize <= 0` | 읽기 전에 `IllegalArgumentException` | tracking Reader 호출 수 0 |
| 줄이 상한 초과 | 첫 초과 code unit에서 `LineLimitExceededException` | `maxLineChars`와 소비 code unit 수 검증 |
| CR 다음이 LF가 아님 | 다음 code unit을 잃지 않고 다음 호출에서 반환 | `"a\rb"`와 작은 buffer 테스트 |
| underlying read 실패 | 같은 IOException instance를 그대로 전달 | custom Reader identity 검증 |
| caller가 Reader를 닫아야 함 | wrapper는 close를 호출하지 않음 | close counter 검증 |
| supplementary character | surrogate pair를 두 code unit으로 계산 | 상한 1/2 경계 테스트 |
| 무한/generated Reader | 전체 입력을 읽지 않고 상한+1 이내에서 종료 | read-count bound 테스트 |

다음 항목은 이번 API가 보장하지 않는다.

- Unicode grapheme, code point 또는 normalized text 단위 제한
- JSON/NDJSON parsing, schema, 행 번호, codec 오류 변환
- Reader의 blocking timeout, coroutine cancellation, executor/dispatcher 선택
- underlying Reader의 thread safety 또는 여러 wrapper 간 동기화
- overflow 후 Reader를 EOF까지 drain하거나 부분 문자열을 복구하는 동작

## 8. 호환성과 downstream migration

- 기존 `Reader.readLine()`과 `InputStream` helper는 변경하지 않는다. 새 API는 opt-in
  top-level class/factory이므로 source와 binary compatibility를 유지한다.
- 새 예외는 `IOException`으로 분류한다. caller는 type과 `maxLineChars`만 사용하고
  message에 payload가 없다는 계약을 따른다.
- Graph #615는 Projects API가 `2.1.0-SNAPSHOT`으로 제공된 후 Jackson2·Jackson3
  parser에 동일한 `boundedLineReader(maxLineChars)`를 생성해 loop를 교체한다.
  parser의 JSON domain과 source close는 Graph가 계속 소유한다.
- 외부 API publish, Graph catalog 동기화, consumer migration, merge와 release는 이번
  PR에 포함하지 않는다.

## 9. 테스트와 문서 수용 기준

### 테스트

- LF, CRLF, CR을 각각 읽고 여러 줄 사이의 다음 code unit을 보존한다.
- empty input, empty line, final line without newline을 구분한다.
- `maxLineChars - 1`, `maxLineChars`, `maxLineChars + 1` 경계를 검증한다.
- `maxLineChars == 0`, 음수 상한, 잘못된 buffer size를 검증한다.
- supplementary character의 surrogate pair를 두 code unit으로 계산한다.
- generated infinite Reader에서 overflow가 `maxLineChars + 1` code unit 안에 발생하고
  read-count가 무제한으로 증가하지 않음을 검증한다.
- custom Reader의 bulk read 0 fallback, IOException identity, close 미호출을 검증한다.
- 작은 `bufferSize`에서 CRLF/CR lookahead와 read-ahead 경계를 검증한다.

### 문서

- 새 public class, exception, factory, `readLine()`에 한국어 KDoc과 ownership/단위/
  overflow 예제를 추가한다.
- `io/io/README.md`와 `io/io/README.ko.md`에 source-equivalent bounded line example,
  UTF-16 단위, close 책임, JSON domain 제외를 추가한다.
- `CHANGELOG.md`의 `Unreleased/추가`에 #1642 provider API를 기록한다. 후속 Graph
  migration이 완료되지 않았으므로 consumer 완료로 표현하지 않는다.

## 10. DoD와 검토 결과

- [x] `bluetape4k-workflow` 공통 gates와 Type A 단계가 PASS한다.
- [x] 공개 API/KDoc/README locale parity가 source와 일치한다.
- [x] targeted test, `:bluetape4k-io:build`, `:bluetape4k-io:detekt`,
  `git diff --check`가 fresh evidence로 통과한다.
- [ ] exact diff self-review에서 P0=0/P1=0이다.
- [ ] Korean Lore commit에 이 spec, plan, 구현, test, README, lesson만 포함된다.
- [x] Graph #615 consumer 변경과 push/PR/merge는 이 worktree 밖 범위다.

검토 시점의 spec writer gate: `SPW-01`~`SPW-05` PASS.
한국어 naturalness gate: `KO-01`~`KO-07` PASS.
