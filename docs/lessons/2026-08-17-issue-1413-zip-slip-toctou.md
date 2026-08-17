# ZIP 추출 sink의 SecureDirectoryStream 경계

## 배경

CodeQL `java/zipslip` alert 46은 `ZipFileSupport.unzip`의 정규화된 entry 경로
검사만으로는 실제 파일 sink의 TOCTOU 경계를 증명할 수 없다는 점을 드러냈다.
대상 디렉토리의 조상 또는 중간 디렉토리를 심볼릭 링크로 바꾸면
`Files.createDirectories`와 path 기반 `FileOutputStream`이 검사 뒤의 다른 위치를
따라갈 수 있었다.

## 결정

- archive entry는 `Path.normalize()`와 destination `Path.startsWith`로 먼저
  containment를 확인한다.
- 대상 디렉토리와 각 중간 디렉토리는 `LinkOption.NOFOLLOW_LINKS`로 검증하고,
  파일을 열기 직전에 file key를 다시 읽어 open 시점의 식별자와 비교한다.
- 실제 출력 sink는 `SecureDirectoryStream`의 디렉토리 상대 `newByteChannel`과
  `NOFOLLOW_LINKS`로 고정한다. 열린 디렉토리 handle은 경로가 교체되어도 원래
  디렉토리에 상대적으로 동작하므로 path-only pre-check에 의존하지 않는다.
- secure directory handle을 제공하지 않는 파일 시스템은 path 기반 fallback 없이
  `IOException`으로 fail-closed 한다.

## 검증

- RED: 대상 조상 symlink 회귀 테스트가 기존 `createDirectories` 경로를 통해
  외부 디렉토리에 도달하여 `IOException` 기대를 만족하지 못했다.
- GREEN: 조상 symlink, 기존 중간 디렉토리 symlink, 최종 파일 symlink, 중간
  디렉토리 교체 경합에서 외부 sentinel 파일이 바뀌지 않는 회귀 테스트를 고정했다.
- `ZipFileSupportTest` 14개 테스트가 구현 전후 계약을 검증했고, 변경 후 전체
  클래스 실행에서 모두 성공했다.
- CodeQL `java/zipslip` 재분석은 PR의 hosted workflow에서 현재 head를 대상으로
  확인한다. alert가 계속 열려 있으면 sink 경계와 workflow 결과를 함께 기록하고
  임의로 dismiss하지 않는다.

## 잔여 한계

`SecureDirectoryStream`은 운영체제와 파일 시스템 provider가 제공해야 한다. 이를
지원하지 않는 환경에서는 추출이 실패하며 안전하지 않은 path-only fallback을
사용하지 않는다. hard link, mount 변경, 파일 시스템 관리자 수준의 동시 조작은
이 API가 소유하지 않는 경계이므로 caller는 신뢰 가능한 전용 대상 디렉토리와
최소 권한을 사용해야 한다.

## 향후 가드

ZIP extraction 변경에는 `../` traversal, sibling-prefix, 조상·중간 디렉토리
symlink, 최종 파일 symlink, 디렉토리 교체 경합을 함께 유지한다. sink를 바꾸면
`SecureDirectoryStream` relative handle, `NOFOLLOW_LINKS`, directory identity
검증, CodeQL hosted rerun을 같은 PR에서 확인한다.

## 참고

- [CodeQL `java/zipslip` query help](https://codeql.github.com/codeql-query-help/java/java-zipslip/)
- [Java `SecureDirectoryStream` API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/SecureDirectoryStream.html)
- [ZipFileSupport.kt](../../io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZipFileSupport.kt)
