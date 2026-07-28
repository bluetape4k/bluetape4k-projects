# CodeQL Java/Kotlin timeout

## 배경

Workflow-local Kotlin `2.3.21` pin으로 CodeQL `java-kotlin` matrix를 다시 활성화한
뒤, scheduled CodeQL run이 약 6시간 뒤 취소되었다.

## 결정

Repository catalog는 Kotlin `2.4.0`으로 유지하고 CodeQL 전용 `2.3.21` rewrite도
유지하되, manual Java/Kotlin build를 full `assemble`에서 scoped generated library
compiler task로 좁힌다.

## 결과

Workflow는 이제 example, demo, benchmark, archive task, distribution task,
resource processing, aggregate assembly 대신 별도의 Java/Kotlin scope에서 library
production source compilation만 CodeQL이 trace하도록 한다. Testing helper는
`testing-core` scope에 유지한다. `bluetape4k-testcontainers` module은 CodeQL tracing
아래에서 단일 `compileKotlin` task가 약 31분 뒤에도 `Build with Gradle`에 머물러
일시적으로 제외했고, issue #999가 해당 coverage 복원을 추적한다. Gradle build step에는
명시적인 120분 timeout도 두어 향후 regression이 기본 360분 GitHub Actions timeout을
소모하지 않게 했다.

## 향후 지침

CodeQL Kotlin analysis에서는 일반 Gradle build가 성공했다고 같은 command가 CodeQL
tracing 아래에서도 적합하다고 가정하지 않는다. Extraction에 필요한 source set만
build하고, workflow-local Kotlin pin은 격리하며, live CodeQL workflow dispatch로
검증한다.
