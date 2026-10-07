# JDK BinarySerializer 필터 변경 코드 리뷰

## 검토 범위와 기준

- 대상: `fix/jdk-binary-serializer-awt-filter`의 `bluetape4k-io` 변경.
- 기준 commit: `21a8fc4a324e5a293c1c789caa05bf713258bc20` (`origin/develop`).
- 검토 파일: `BinarySerializers.kt`, `JdkBinarySerializer.kt`, `JdkBinarySerializerSecurityTest.kt`, 한국어·영어 README, `docs/security/serialization-trust-profiles.md`.
- 독립 검토: `code-reviewer` 보안 검토 및 `architect` 설계 검토. 두 검토 모두 코드를 수정하거나 테스트를 실행하지 않았다.

## 통합 결과

| 관점 | 결과 | 처리 |
|---|---|---|
| 보안 | P0–P2 없음. P3: `JdkUnfiltered` 이름으로 JVM 전역 필터 적용을 직접 검증하는 fork 테스트 권고. | 기존 별도 JVM fixture가 같은 `objectInputFilter = null` 생성자 경로의 ByteArray·ByteBuffer 전역 필터 동작을 검증한다. 현 변경의 병합을 막지 않는 후속 보강으로 기록한다. |
| 설계 | 최초 `WATCH`: 새 공개 이름이 기존 `Unsafe` 명명 규칙과 어긋남. | 요청된 이름을 유지하고 중앙 trust profile에서 deprecated `TrustedInternal` 예외로 분류했다. KDoc와 두 README에 테스트·신뢰 입력 전용 및 JVM 전역 필터 설명을 추가한 뒤 재검토를 요청했다. 최종 `CLEAR`. |
| 교훈 문서 | P2: 기본 필터 동작이 유지된다는 표현이 `java.awt.Color` 허용 목록 추가를 가릴 수 있음. | 기본 필터 적용 경로와 허용 목록 변경을 분리해 적고, `Font` 검증을 “필터 판정 거부”로 구체화했다. 최종 문서 검토 `APPROVE`. |

## 검증 근거

- 기본 필터는 `java.awt.Color`만 추가로 허용하며 마지막 `!*`를 유지한다. 테스트는 `Color` 왕복과 `Font` 필터 판정을 확인한다.
- 기본 allowlist 밖의 payload는 deprecated `BinarySerializers.JdkUnfiltered`로 왕복한다.
- `repo-test-summary -- ./gradlew :bluetape4k-io:test --tests io.bluetape4k.io.serializer.JdkBinarySerializerSecurityTest --rerun-tasks --no-parallel --max-workers=1 --console=plain`: 15개 통과.
- `repo-test-summary -- ./gradlew :bluetape4k-io:test --rerun-tasks --no-parallel --max-workers=1 --console=plain`: 81개 suite, 1,316개 테스트 중 1,314개 통과, 2개 건너뜀, 실패·오류 0개.
- `:bluetape4k-io:detekt`: 빌드 성공. 진단 4건은 수정 파일 밖의 기존 항목이다.
- 한국어 기술 표면 5개 파일의 contextual terminology audit: finding 0. `git diff --check`: 통과.

## 판정과 남은 검증

구현·API·문서 변경은 승인한다. 허용 목록은 좁게 유지되고, 필터 생략 API는 문서상 `TrustedInternal` 예외로 구분된다. PR 생성 전 최종 검토 및 CI는 아직 수행하지 않았으며, 병합 전에는 live PR의 정확한 head에서 CI와 리뷰를 다시 확인해야 한다. `JdkUnfiltered`의 JVM 전역 필터 직접 검증은 기존 동일 생성자 경로 테스트를 근거로 비차단 P3로 남긴다.
