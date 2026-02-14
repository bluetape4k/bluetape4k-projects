# Module bluetape4k-junit5

JUnit 5 테스트 작성 시 반복 코드를 줄여주는 확장 라이브러리입니다.

## 주요 기능

- **Stopwatch Extension**: 테스트 실행 시간 측정
- **TempFolder Extension**: 테스트용 임시 디렉토리/파일 제공
- **Output Capture**: System.out/err 및 로그 출력 캡처
- **Random/Faker 확장**: 랜덤/가짜 데이터 주입
- **System Property 확장**: 테스트 중 시스템 속성 설정/복원
- **Awaitility + Coroutines**: suspend 조건 대기 유틸
- **Stress Tester**: JUnit5 테스트에서 멀티스레드/가상스레드/코루틴 기반 스트레스 테스트 수행
- **Parameter Source 확장**: FieldSource 기반 인자 제공
- **Mermaid 리포트**: 테스트 실행 결과를 Mermaid Gantt 타임라인으로 출력

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-junit5:${version}")
}
```

## 주요 기능 상세

### 1. 테스트 실행 보조

- `stopwatch/StopwatchExtension.kt`
- `tempfolder/TempFolderExtension.kt`
- `output/OutputCaptureExtension.kt`

### 2. 데이터 주입/랜덤화

- `faker/FakeValueExtension.kt`
- `random/RandomExtension.kt`
- `params/provider/FieldArgumentsProvider.kt`

### 3. 환경/설정 보조

- `system/SystemPropertyExtension.kt`
- `awaitility/AwaitilityCoroutines.kt`
- `awaitility/AwaitilityConfigurationExtension.kt`

### 4. 동시성/코루틴 테스트 보조

- `concurrency/MultithreadingTester.kt`
- `concurrency/StructuredTaskScopeTester.kt`
- `coroutines/SuspendedJobTester.kt`

### 5. 테스트 실행 리포트

- `report/MermaidTestExecutionListener.kt`

## JUnit5 Stress Test 강조

`bluetape4k-junit5`는 확장(Extension)만 제공하는 모듈이 아니라, JUnit5 테스트 본문에서 바로 사용할 수 있는 Stress Tester 유틸도 제공합니다.

- `MultithreadingTester`: 플랫폼 스레드 기반
- `StructuredTaskScopeTester`: Java 21 Virtual Thread 기반
- `SuspendedJobTester`: 코루틴 Job 기반

```kotlin
@Test
fun `멀티스레드 스트레스 테스트`() {
    MultithreadingTester()
        .workers(Runtime.getRuntime().availableProcessors())
        .rounds(100)
        .add {
            // 동시성 검증 코드
        }
        .run()
}
```

## `gantt.mermaid` 생성/활용

`MermaidTestExecutionListener`가 JUnit Platform `TestExecutionListener`로 자동 등록되어, 테스트 종료 시 Mermaid Gantt 문자열을 출력합니다.

- 출력 포맷: `gantt` 문법
- 섹션 구분: 테스트 클래스 단위
- 상태 표기: 성공(`active`), 실패(`crit`), 중단(`done`)

생성된 문자열을 `gantt.mermaid` 파일로 저장하면 Mermaid 뷰어/문서에서 바로 시각화할 수 있습니다.

```bash
./gradlew :testing:junit5:test | awk 'f||/^gantt$/{f=1; print}' > testing/junit5/gantt.mermaid
```

## 참고

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Awaitility](https://github.com/awaitility/awaitility)
