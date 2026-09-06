# Issue #1645 원자적 파일 쓰기 API 설계

## 1. 문제

`bluetape4k-image`는 파일 하나를 안전하게 교체하는 lifecycle을 세 곳에서
반복한다. 각 구현은 대상 파일과 같은 디렉터리에 임시 파일을 만들고, 내용을
쓴 뒤 검증하고, `ATOMIC_MOVE`와 `REPLACE_EXISTING`을 전달해 기존 파일을 교체한다.
실패하면 임시 파일을 정리하고 기존 대상은 유지한다.

- `images/.../batch/AtomicImageOutput.kt`는 `OutputStream` 종료, commit 직전
  coroutine 취소 검사, 파일 크기 반환, cleanup 실패의 suppressed 보존을 구현한다.
- `images-spring-boot/.../s3/S3ImageStorage.kt`는 S3 응답 stream 종료, 다운로드
  상한과 S3 metadata 기준 크기 검증, 취소 및 `ImageStorageException` 변환을
  구현한다.
- `benchmark/images-benchmark/.../CodecMatrixJson.kt`는 strict JSON과 최대 크기를
  검증하고, 파일 교체 뒤 입력 bytes의 SHA-256을 반환한다.

파일 lifecycle은 같지만 각 소비자의 검증·오류 변환 정책은 다르다. 공통 부분을
`bluetape4k-io` 공개 API로 승격하고 소비자 정책은 각 모듈에 남긴다.

## 2. 목표와 성공 조건

`Path`에 내용을 쓰는 callback 하나를 받아 다음 순서를 실행하는 동기 공개 API를
제공한다.

1. 원래 receiver에 유효한 파일명이 있는지 검사한 뒤 최종 대상 경로를 absolute
   normalized path로 결정한다.
2. parent directory를 생성한다.
3. 같은 parent에 고유한 sibling 임시 파일을 생성한다.
4. provider가 연 `OutputStream`으로 callback을 실행한다.
5. callback 정상 반환 뒤 stream을 닫는다.
6. provider-owned counting stream이 기록한 byte 수를 확정한다.
7. `ATOMIC_MOVE`와 `REPLACE_EXISTING`을 함께 전달해 target을 교체한다. JDK 계약상
   `ATOMIC_MOVE`가 적용되면 다른 option은 무시되므로 기존 target 교체 여부는
   filesystem provider가 결정한다.
8. 성공 시 기록한 크기를 반환한다. commit 전 실패 경로에서만 남은 임시 파일을
   정리하며, commit 뒤에는 이미 이동된 staged path를 다시 조회하지 않는다.

성공 조건은 지원 provider에서의 기존 target 보존과 성공 교체,
callback·close·commit·temp 생성·cleanup 실패의 예외 계약, fail-closed atomic move,
실제 이동 옵션, public API/ABI, README locale parity를 테스트로 증명하는 것이다.

## 3. 현재 근거

### 3.1 제공자

- Projects issue: [#1645](https://github.com/bluetape4k/bluetape4k-projects/issues/1645)
- 기준: `bluetape4k-projects@31ee966cf339f7b895284329c8fbd53638ab837c`
- `io/io/src/main/kotlin/io/bluetape4k/io/FileSupport.kt`에는 일반 파일 생성·읽기·쓰기
  API가 있지만 atomic replacement lifecycle을 묶는 공개 API는 없다.
- `BufferFailurePolicy`와 compressor 구현은 primary failure identity를 유지하고
  cleanup failure를 suppressed로 붙이는 repository 관례를 제공한다.
- `:bluetape4k-io:test` baseline은 1,265 tests, 0 failures로 통과했다.

### 3.2 소비자

- Image issue: [#631](https://github.com/bluetape4k/bluetape4k-image/issues/631)
- 기준: `bluetape4k-image@f4a656bea134f1dc890bf6306e000404754ba763`
- `bluetape4k-images`는 이미 central catalog의 `bluetape4k-io`를 API dependency로
  사용한다.
- 2026-09-06에 확인한 기존 `2.1.0-SNAPSHOT` artifact에는 신규 API가 없다.
  Projects PR merge와 새 `2.1.0-SNAPSHOT` 발행이 Image migration의 선행 조건이다.

### 3.3 JDK provider 계약

- [JDK 25 `Files.move`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...))는
  `ATOMIC_MOVE`에서 다른 option을 무시하고 기존 target 처리 여부를 provider에 맡긴다.
- [JDK 25 `Files.createTempFile`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#createTempFile(java.nio.file.Path,java.lang.String,java.lang.String,java.nio.file.attribute.FileAttribute...))은
  고정 permission이나 기존 target attribute 상속을 보장하지 않는다.
- [JDK 25 `Files.createDirectories`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#createDirectories(java.nio.file.Path,java.nio.file.attribute.FileAttribute...))는
  실패 전에 일부 parent directory가 만들어질 수 있음을 명시한다.

## 4. 선택한 공개 API

```kotlin
@file:JvmName("AtomicFileSupport")

package io.bluetape4k.io

import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path

@Throws(IOException::class)
fun Path.writeAtomically(writer: (OutputStream) -> Unit): Long
```

API는 `suspend`가 아닌 동기 함수다. 파일 쓰기와 provider 호출은 blocking 작업이며,
dispatcher 선택과 cancellation 검사는 호출자가 소유한다. Image adapter는 기존처럼
`withContext(ioDispatcher)`에서 호출하고, 미리 캡처한 `CoroutineContext`로 callback이
반환되기 전에 `ensureActive()`를 실행한다.

반환값은 provider-owned counting stream이 기록하고 stream close 성공 뒤 확정한 byte
수다. `Files.size` metadata 조회를 추가하지 않으며, commit 뒤 target을 다시 조회하지
않으므로 경쟁 writer가 곧바로 target을 교체하더라도 이번 호출이 쓴 크기를 반환한다.

### 4.1 사용 예

```kotlin
val callerContext = currentCoroutineContext()
val written = withContext(ioDispatcher) {
    destination.writeAtomically { output ->
        source.use { input -> input.copyTo(output) }
        callerContext.ensureActive()
    }
}
```

callback은 provider가 연 `OutputStream`을 실행 중에만 사용한다. stream을 보관하거나
직접 닫지 않으며, 별도 buffering wrapper를 사용하면 callback이 반환되기 전에
flush한다. 이 소유권 계약을 어긴 동작은 보장하지 않는다. callback이 반환되면
provider가 stream을 닫고, 닫기가 성공한 뒤에만 commit한다.

## 5. 선택하지 않은 대안

### 5.1 staged `Path` callback

`writeAtomically(target) { stagedPath -> ... }`는 임의의 file API를 사용할 수 있어
범용성이 높다. 그러나 provider가 callback 내부에서 연 stream과 writer가 모두
닫혔는지 확인할 수 없다. 이번 세 소비자는 모두 `OutputStream`으로 표현할 수 있고
stream 종료 뒤 commit 순서가 완료 조건이므로 채택하지 않는다.

### 5.2 transaction 객체

staged path, stream factory, validation hook, commit option을 가진 transaction 객체는
향후 다양한 정책을 수용할 수 있다. 현재 필요한 동작보다 공개 surface가 크고,
no-clobber·directory promotion·durability 같은 별도 계약이 섞일 가능성이 있어
채택하지 않는다.

## 6. 경로와 commit 계약

- `Path.of("")`와 filesystem root처럼 원래 receiver의 파일명이 없거나 비어 있으면
  absolute 변환 전에 `IllegalArgumentException`으로 거부한다.
- 유효한 receiver는 `toAbsolutePath().normalize()`로 변환한다. 이는 lexical
  normalization이며 traversal, symlink, hard link 또는 TOCTOU 방어가 아니다.
- parent가 없으면 normalized absolute path의 parent를 사용한다.
- `Files.createDirectories(parent)`로 parent를 준비한다.
- `Files.createTempFile(parent, ".$fileName.", ".tmp")`로 sibling을 만든다.
- commit은 `Files.move(staged, target, ATOMIC_MOVE, REPLACE_EXISTING)` 한 번으로만
  수행한다.
- [JDK 25 `Files.move`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...))
  계약상 `ATOMIC_MOVE`에서는 다른 option이 무시된다. 따라서 `REPLACE_EXISTING`을
  의도와 기존 consumer parity를 위해 전달하되, 기존 target을 교체할지
  `IOException`으로 거부할지는 provider별 동작이다.
- 지원 provider에서 move가 성공하면 target을 한 번에 교체한다.
  `AtomicMoveNotSupportedException`, `UnsupportedOperationException`, 기존 target
  거부를 포함한 `IOException`을 그대로 전파하며 일반 move, delete-then-move 또는
  target 직접 쓰기로 낮추지 않는다.
- 동시에 여러 writer가 같은 target을 갱신하면 성공한 atomic move 중 마지막 결과가
  남는다. 호출 간 직렬화나 compare-and-swap은 보장하지 않는다.
- staged file의 attributes가 새 target의 attributes가 된다. 기존 target의 권한,
  owner, ACL, xattr 보존을 보장하지 않으며 timestamp와 그 밖의 attribute 처리는
  provider 계약을 따른다.

## 7. 실패와 cleanup 계약

### 7.1 primary failure

parent 생성, temp 생성, stream open, callback, stream close, commit 중 발생한
`Throwable`을 identity 그대로 다시 던진다. `Error`와 `CancellationException`도
다른 예외로 변환하지 않는다.

callback과 stream close가 모두 실패하면 callback failure가 primary이고 close
failure가 첫 suppressed다. 여기에 cleanup도 실패하면 cleanup failure를 다음
suppressed로 붙인다. callback이 성공하고 close가 실패하면 close failure가 primary다.
suppressed를 추가할 때 같은 instance, 이미 등록된 instance, 양방향 cause/suppressed
reachability를 거부해 중복과 순환 Throwable graph를 만들지 않는다.

### 7.2 cleanup failure

staged path를 만든 뒤 commit 전 실패하면 `finally`에서
`Files.deleteIfExists(staged)`를 실행한다. move가 성공하면 staged entry는 target으로
이동됐으므로 cleanup lookup을 실행하지 않는다.

- primary failure와 cleanup failure가 함께 발생하면 primary를 던지고 cleanup을
  suppressed로 추가한다.
- 성공한 commit 뒤에는 cleanup 실패로 정상 결과가 예외로 바뀌는 경로가 없다.

### 7.3 기존 target 보존 경계

callback, close 또는 실제 move 실행 전 실패에서는 기존 target을 유지한다. 지원
provider의 atomic move가 성공하면 새 target이 완전히 반영된다. provider가 move
요청을 거부하면 그 예외와 provider 계약을 그대로 따른다. 일반 move fallback을
실행하지 않으므로 library가 불완전 target을 만드는 복구 경로는 없다.

parent 자동 생성은 rollback 대상이 아니다. `createDirectories`가 일부 parent를 만든
뒤 실패하거나 이후 단계가 실패해도 이미 생성된 directory는 남을 수 있다. commit
성공 뒤 결과를 반환하는 시점의 coroutine 취소가 교체를 되돌린다고 약속하지 않는다.

## 8. 내부 구현과 테스트 seam

공개 surface는 위 extension 하나만 추가한다. production overload는 JDK `Files`를
사용하는 internal operations 객체에 위임한다. 같은 모듈의 테스트는 internal seam으로
다음 실패를 결정적으로 주입한다.

- parent 생성 실패
- temp 생성 실패
- stream open 실패
- callback 성공/실패와 stream close 성공/실패
- move 호출 시 stream이 이미 닫혔는지 검사
- 전달된 `ATOMIC_MOVE`, `REPLACE_EXISTING` 옵션 검사
- `AtomicMoveNotSupportedException`, `UnsupportedOperationException`, 기존 target 거부와
  일반 commit 실패
- callback·close·cleanup 동시 실패의 primary/suppressed identity와 순서
- commit 전 cleanup 실패와 suppressed graph 순환 방지

internal seam은 public constructor, SPI, dependency injection point가 아니다. 새 외부
dependency도 추가하지 않는다.

## 9. 보장하지 않는 범위

- file content 또는 parent directory의 `fsync`와 process/power loss durability
- path sandbox, traversal 검증, symlink/hard-link/mount 교체 방어
- 기존 target의 권한, owner, ACL, xattr 보존 또는 staged file attribute 사용자 지정
- directory tree 교체
- no-clobber, compare-and-swap, content equality 검사
- retry, lock, multi-writer ordering
- payload byte/time/quota 제한, checksum, JSON schema, S3 metadata 검증
- coroutine dispatcher 선택과 commit 이후 cancellation rollback
- library logging, metrics, orphan cleanup daemon

이 API는 호출 전체 동안 신뢰된 주체가 독점 관리하며 공격자가 entry를 생성·이름
변경·교체할 수 없는 parent에서만 사용한다. 공유 writable directory나 공격자가
제어하는 경로에는 사용하지 않고 secure directory handle 기반 API를 선택한다. 호출자는
remote/untrusted 입력을 읽는 동안 callback 내부에서 streaming byte/time/quota 제한을
적용하고, domain validation을 callback이 반환되기 전에 완료한다.

process crash나 강제 종료에서는 `finally`가 실행되지 않아
`.$fileName.*.tmp` sibling이 남을 수 있다. 운영자는 활성 writer가 없음을 확인한 뒤
이 패턴의 오래된 orphan을 정리하고 directory 용량을 감시한다. library는 로그나
metric을 만들지 않으므로 caller가 target/provider, primary failure와 suppressed
cleanup failure를 민감정보 없이 기록한다.

## 10. 호환성과 문서

- 신규 top-level extension 추가이므로 기존 source/binary signature를 변경하지 않는다.
- `AtomicFileSupport.kt`에 `@file:JvmName("AtomicFileSupport")`를 선언하고 public
  function은 non-inline로 유지해 구현 세부가 consumer bytecode에 복사되지 않게 한다.
- JVM signature는 `AtomicFileSupport.writeAtomically(Path, Function1)`이며
  `@Throws(IOException::class)`를 선언한다.
- Kotlin lambda가 주 사용 surface다. Java caller는 `Function1<OutputStream, Unit>`에서
  `Unit.INSTANCE`를 반환해야 하며 callback 내부 checked exception을 직접 처리해야 한다.
  이번 범위에서 별도 Java SAM interface나 options type을 추가하지 않는다.
- Kotlin compile fixture, 실제 Java compile/call fixture와 `javap`로 facade, descriptor,
  `throws IOException`이 예상대로 존재하는지 확인한다.
- `io/io/README.ko.md`와 `io/io/README.md`에 같은 구조의 계약과 예제를 추가한다.
- `CHANGELOG.md`의 미출시 섹션에 공개 API 추가를 한국어로 기록한다.
- 새 모듈, dependency, catalog alias, workflow, Kover registration은 추가하지 않는다.

## 11. downstream migration 경계

Projects PR merge와 새 `2.1.0-SNAPSHOT` 발행 이후 Image #631에서 다음을 수행한다.

- `AtomicImageOutput`은 suspend block에서 `CoroutineContext`를 먼저 캡처하고 callback
  마지막에 `context.ensureActive()`를 실행해 commit 전 취소 검사를 유지한다. 공용
  extension에 위임하거나 wrapper 중복을 제거하고 기존 `Long` 반환을 유지한다.
- `S3ImageStorage.download`는 callback 안에서 S3 `InputStream`을 닫고, streaming 크기
  상한과 metadata 기준 크기 검증을 완료한 뒤 캡처한 context의 `ensureActive()`를
  실행하고 callback을 반환한다. success, copy failure, input close failure, 최종 read
  직후 cancellation에서 source close와 기존 target 보존을 검증한다.
- `S3ImageStorage.download`의 cancellation identity와 `ImageStorageException` 변환을
  유지한다.
- `CodecMatrixJson`은 strict JSON, 최대 bytes, SHA-256 반환을 유지한다.

`CodecMatrixBenchmarkParameters`, immutable fixture directory, Graph Okio FileSystem,
`SecureDirectoryStream` 기반 경로는 다른 overwrite·directory·보안 계약이므로 이번
migration에서 제외한다.

Image migration 시작 전 Projects exact commit SHA, GAV, `2.1.0-SNAPSHOT` artifact 발행
시각을 기록한다.
Image가 local repository override 없이 신규 JVM signature가 포함된 원격
`io.github.bluetape4k:bluetape4k-io:2.1.0-SNAPSHOT`을 해석한 사실을 compile
dependency evidence로 확인한다.

## 12. 테스트 전략

### 12.1 실제 filesystem 계약

- target이 없을 때 새 파일을 만들고 counting stream의 byte 수를 반환한다.
- default provider에서 기존 target을 callback 성공 뒤 교체한다. 다른 provider의
  기존 target 처리는 portable 성공 조건으로 사용하지 않는다.
- callback 실패와 close 실패에서 기존 target을 보존하고 staged file을 제거한다.
- 성공 뒤 cleanup lookup을 실행하지 않고, 실패 뒤 sibling temporary file이 남지 않는다.
- relative/normalized target과 parent 자동 생성을 검증한다. `Path.of("")`와 filesystem
  root를 거부하고, 이후 실패에서 이미 생성된 parent가 남는 계약도 확인한다.
- barrier/latch로 서로 다른 target의 callback이 동시에 진입해 provider-global
  serialization이 없음을 증명한다.
- 같은 target의 동시 write는 완전한 payload 하나만 남기고 sibling temp를 누출하지
  않음을 시간 임계값 없이 검증한다.

### 12.2 결정적 failure injection

- parent/temp 생성 실패에서 writer와 move가 호출되지 않는다.
- stream open 실패에서 writer와 move가 호출되지 않고 staged file을 정리한다.
- move 전에 stream close가 완료된다.
- move에 두 필수 option만 전달된다.
- atomic move 미지원, unsupported option, 기존 target 거부와 일반 commit 실패가
  non-atomic fallback 없이 전파된다.
- callback·close·cleanup 동시 실패에서 callback identity, close 첫 suppressed,
  cleanup 다음 suppressed 순서를 검증한다. `CancellationException`과 `Error` 조합도
  포함한다.
- cleanup failure는 primary를 덮지 않고, 같은 instance·중복·순환 Throwable graph를
  추가하지 않는다.
- counting stream을 사용하며 `Files.size` operation을 호출하지 않음을 검증한다.

### 12.3 공개 API와 회귀

- Kotlin caller, 실제 Java caller와 `javap`로 `AtomicFileSupport` facade, public JVM
  signature와 `throws IOException`을 확인한다.
- KDoc는 blocking 성격, byte count, callback stream 유효 기간·소유권,
  `IllegalArgumentException`, provider I/O와 atomic-move 예외, callback exception
  identity, suppressed cleanup, 신뢰된 parent, metadata와 durability 제외, caller가
  맡는 실패 관측 책임을 설명한다.
- `:bluetape4k-io:test`, compile, detekt, `git diff --check`를 실행한다.
- Image migration은 새 `2.1.0-SNAPSHOT` 발행 뒤 세 consumer의 기존 regression suite를 별도
  repository에서 실행한다.

## 13. 수용 기준

- [ ] `Path.writeAtomically`가 parent와 sibling temp를 관리한다.
- [ ] callback과 stream close 성공 뒤에만 provider의 atomic move를 시도한다.
- [ ] 성공 시 provider-owned counting stream의 byte 수를 반환하고 `Files.size`를
  추가로 호출하지 않는다.
- [ ] callback·close·commit 전 실패에서 기존 target을 보존한다.
- [ ] unsupported atomic move와 provider의 기존 target 거부에서 fallback 없이 실패한다.
- [ ] cleanup failure가 primary `Throwable`과 cancellation identity, suppressed 순서와
  비순환 graph를 보존한다.
- [ ] 실제 move option과 stream-close-before-move 순서를 테스트한다.
- [ ] empty/root path, parent 잔존, metadata 비보존, trusted-parent, orphan-temp 운영과
  caller-owned 실패 관측 경계가 KDoc/README에 명시된다.
- [ ] 결정적 동시성 테스트가 global serialization 부재, same-target 완전 payload,
  temp 미누출을 증명한다.
- [ ] public KDoc, README locale pair, CHANGELOG, API/ABI proof가 source와 일치한다.
- [ ] 기존 module/dependency/catalog/workflow 등록은 변경하지 않는다.
- [ ] provider PR과 새 `2.1.0-SNAPSHOT` 발행, exact SHA/GAV/시각 및 remote resolution
  증거가 끝나기 전 Image migration을 시작하지 않는다.

## 14. DoD

- 승인된 API와 경계를 spec·plan·source·KDoc·README에 같은 의미로 기록한다.
- TDD의 RED/GREEN evidence와 targeted/broader validation을 남긴다.
- six-perspective spec/plan/code review와 main integration에서 P0=0, P1=0을 달성한다.
- 재사용 가능한 교훈을 `docs/lessons/`에 커밋한다.
- exact local head를 push하고 live Projects PR의 metadata, CI, review를 확인한다.
- merge와 `2.1.0-SNAPSHOT` 발행은 별도 fresh approval 전까지 실행하지 않는다.
