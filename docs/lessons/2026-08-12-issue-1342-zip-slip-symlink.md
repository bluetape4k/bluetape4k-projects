# ZIP 추출 경로와 심볼릭 링크 경계

## 배경

CodeQL `java/zipslip` alert 46이 `ZipFileSupport.unzip`의 archive entry 경로에서
계속 열려 있었다. 기존 canonical path 검사는 `../` traversal과 sibling-prefix는
차단했지만, 출력 디렉토리 또는 파일을 심볼릭 링크로 바꾼 뒤의 쓰기 경계와
TOCTOU 위험을 회귀 테스트로 고정하지 않았다.

## 결정

- archive entry를 `Path.normalize()`로 정규화하고 destination 경로의 완전한
  path segment로 시작하는지 확인한다.
- 출력 디렉토리를 생성·확인할 때 `LinkOption.NOFOLLOW_LINKS`를 사용해 각
  경로 구성요소가 심볼릭 링크가 아닌지 재검증한다.
- 파일 출력은 `Files.newByteChannel`과 `NOFOLLOW_LINKS`로 열어 기존 파일
  심볼릭 링크를 따라가지 않게 한다.
- destination 경로와 기존 출력 파일·디렉토리는 쓰기 직전에 다시 검증한다.
  지원하지 않는 archive entry는 외부 경로 쓰기 전에 실패한다.

## 결과

기존 `../` 및 sibling-prefix 방어를 유지하면서 출력 디렉토리 심볼릭 링크와
출력 파일 심볼릭 링크를 통한 외부 파일 쓰기를 차단한다. ZIP 추출 동작과
보안 경계를 영문·한국어 README에 함께 기록했다.

## 검증

- RED: 새 symlink 회귀 2건이 기존 구현에서 `IllegalArgumentException` 경계로
  실패했다.
- GREEN: `./gradlew :bluetape4k-io:test --tests io.bluetape4k.io.compressor.ZipFileSupportTest --no-daemon --no-build-cache --console=plain`
  — 12개 테스트 성공.
- `./gradlew :bluetape4k-io:test :bluetape4k-io:detekt --no-daemon --no-build-cache --console=plain`
  — `BUILD SUCCESSFUL`.
- `git diff --check` — 성공.

## 놓친 점

canonical path 검사는 archive entry traversal만 증명하며, 기존 출력 경로의
심볼릭 링크와 파일 sink의 no-follow 계약을 별도로 증명하지 않는다. CodeQL의
권고도 정규화된 전체 경로 검증을 요구하므로, 경로 검사와 실제 sink 옵션을
분리하지 않는 것이 중요하다.

## 향후 가드

archive extraction 변경에는 최소한 `../`, sibling-prefix, 기존 출력 디렉토리
symlink, 기존 출력 파일 symlink의 네 가지 회귀 시나리오를 유지한다. 파일
출력 sink를 바꾸면 `NOFOLLOW_LINKS`와 쓰기 직전 경로 재검증을 함께 확인하고,
CodeQL 재분석 결과가 현재 HEAD에 적용됐는지 별도로 확인한다.

## 참고

- [CodeQL `java/zipslip` query help](https://codeql.github.com/codeql-query-help/java/java-zipslip/)
- [ZipFileSupport.kt](../../io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZipFileSupport.kt)
