# createTempDirectory TOCTOU 경쟁 조건 수정

**날짜**: 2026-05-16
**이슈**: #479
**브랜치**: `fix/create-temp-directory`

## 근본 원인

`createTempDirectory`는 time-of-check/time-of-use(TOCTOU) 경쟁 조건이 있는 3단계 절차를 사용했다:

```kotlin
// BEFORE (racy)
val dir = File.createTempFile(prefix, suffix)  // 1. 임시 FILE 생성
dir.deleteRecursively()                         // 2. 삭제
dir.mkdirs()                                    // 3. directory로 재생성
```

2단계와 3단계 사이에 다른 process가 같은 path를 선점할 수 있다. 또한 `deleteRecursively()`와
`mkdirs()`의 반환값을 확인하지 않아 실패가 조용히 무시되었고, 함수는 유효한 directory가
생성된 것처럼 반환했다.

## 수정

경쟁 조건이 있는 절차를 JDK atomic `Files.createTempDirectory` API로 교체한다:

```kotlin
// AFTER (atomic)
val dir = Files.createTempDirectory(prefix).toFile()
```

`Files.createTempDirectory`는 단일 atomic OS syscall로 directory를 생성하므로 다른 process가
path를 선점할 window가 없다.

`suffix` parameter는 API 호환성을 위해 signature에 남기지만 이제 무시된다. 생성된 directory
이름은 더 이상 suffix 값으로 끝나지 않는다(예: `.dir` extension 없음).
`Files.createTempDirectory`가 자체 고유 numeric suffix를 생성한다.

## 테스트 범위

`FileSupportTest`에 새 테스트 3개를 추가했다:

1. **Atomic 생성** — 반환된 path가 존재하고 `isDirectory == true`임을 검증한다.
2. **고유성** — 순차 호출 2회가 서로 다른 canonical path를 생성함을 검증한다.
3. **동시 고유성** — 8개 thread의 동시 호출 50회가 모두 고유하고 존재하는 directory를
   생성함을 확인해 race-free 속성을 보여준다.

## 핵심 교훈

**임시 resource에는 JDK atomic filesystem API를 사용한다.**
`File.createTempFile` + delete + mkdir는 전형적인 TOCTOU 패턴이다.
`Files.createTempDirectory`와 `Files.createTempFile`은 resource를 atomic하게 생성하도록
설계되었으므로 항상 우선 사용한다.

**filesystem operation의 반환값을 확인한다.**
`deleteRecursively()`와 `mkdirs()`는 성공 여부를 나타내는 `Boolean`을 반환한다. 기존 코드는
둘 다 무시했기 때문에 silent failure가 가능했다. 새 코드는 이 operation들을 완전히 피하며,
shutdown hook은 primary result를 가리지 않도록 `runCatching`으로 실패를 logging한다.

## 검증

```
:bluetape4k-io:test (FileSupportTest)  16 passing (2.1s) — BUILD SUCCESSFUL
```
