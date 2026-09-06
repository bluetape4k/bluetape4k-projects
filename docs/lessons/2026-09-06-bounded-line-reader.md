# 길이 제한 Reader는 문자 단위와 소유권을 함께 고정해야 한다

## 맥락

`Reader.readLine()`은 line 전체를 먼저 문자열로 만들기 때문에, JSON/NDJSON parsing
전에 신뢰할 수 없는 긴 line이 들어오면 입력 크기만큼 메모리를 사용할 수 있다. 이번
변경은 `bluetape4k-io`에 domain parser와 분리된 provider API를 추가해 읽는 중에 line
길이를 판정하도록 했다. Graph의 Jackson2·Jackson3 consumer 전환은 provider artifact가
준비된 뒤의 별도 작업으로 남겼다.

## 결정과 발견

- `BoundedLineReader` 상태 보유 wrapper와 `Reader.boundedLineReader(...)` factory를
  제공한다. 반복 `readLine()` 호출 사이의 CR lookahead와 buffer 잔여 상태를 한 객체가
  보존하므로 호출마다 새 extension을 만드는 방식보다 계약이 분명하다.
- `maxLineChars`는 Kotlin/JVM `Char`와 같은 UTF-16 code unit 단위다. supplementary
  character 하나는 두 unit으로 세며, LF·CRLF·CR terminator는 상한에 포함하지 않는다.
- 상한까지는 반환하고 다음 일반 code unit을 읽기 전에
  `LineLimitExceededException`을 던진다. 현재 줄에 남은 허용량에 `+1`을 더한
  bounded read만 수행해 overflow를 판정하며, 예외에는 입력 payload를 저장하지 않는다.
- 내부 buffer는 고정 크기이고, CR 뒤의 LF 확인이 필요할 때만 code unit 하나를
  read-ahead한다. bulk `Reader.read`가 0을 반환하는 구현은 단일 `read()`로 진행한다.
- wrapper는 `Closeable`을 구현하지 않고 원본 `Reader`를 닫지 않는다. timeout, blocking,
  dispatcher, close 및 JSON/NDJSON parsing은 caller가 계속 소유한다.

## 결과

`bluetape4k-io`에 빈 입력·빈 line·개행 없는 마지막 line, LF/CRLF/CR, 정확한 상한과
초과, surrogate counting, generated Reader read-count, IOException identity와 close
ownership를 고정한 테스트 10개를 추가했다. English/Korean README와 `CHANGELOG.md`에는
같은 factory 사용 예제와 책임 경계를 기록했다.

초기 RED에서는 production symbol이 없어 `compileTestKotlin`이 실패했고, API 구현 후
동일 targeted class가 10개 모두 통과했다. 첫 detekt report에서 새 production/test
파일의 `ReturnCount`를 확인한 뒤 상태 머신의 결과 변수와 테스트 reader의 `when`으로
정리했으며, 재실행 시 변경 파일 finding은 0건이었다. 모듈 전체 테스트는 1,308개가
통과했다.

## 재사용 체크리스트

1. 상한의 단위를 byte, code point, UTF-16 code unit 중 하나로 명시하고 supplementary
   character 경계를 테스트한다.
2. terminator를 상한에 포함할지, CR 뒤 lookahead를 어디까지 허용할지 고정한다.
3. `max + 1` 계산은 `Int` overflow 없이 수행하고, generated Reader로 실제 소비량을
   검증한다.
4. wrapper와 underlying Reader의 close ownership 및 원본 IOException identity를
   테스트한다.
5. provider helper에는 JSON/domain parsing을 넣지 않고 consumer migration을 별도 PR로
   유지한다.

## 잔여 범위

이번 작업은 provider API와 로컬 검증까지만 포함한다. Graph #615 consumer 적용, 공개
artifact resolution, PR CI, release 및 동시 사용/thread-safety 계약은 후속 검증 대상이다.

## 검증

- `:bluetape4k-io:test --tests 'io.bluetape4k.io.BoundedLineReaderSupportTest'`: 10 passing
- `:bluetape4k-io:cleanTest :bluetape4k-io:test`: 1,308 passing
- `:bluetape4k-io:build`: `BUILD SUCCESSFUL`
- `:bluetape4k-io:detekt`: exit 0, 변경 파일 finding 0건; 기존 파일 finding은 baseline으로 남음
- Korean terminology audit와 `git diff --check`: 통과
