# #1651: HTTP Future 취소는 실행 순서별로 검증한다

## 잘못된 가정과 관찰

`ClientWithRequestFuture`는 GET 제출 직후 `cancel(true)`를 호출하고 10ms 뒤
`get()`이 직접 `CancellationException`을 던진다고 가정했다.
[CI 33932767933](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33932767933)의
첫 시도에서는 `ExecutionException(CancellationException)`으로 실패했고 재시도는 통과했다.
로컬의 기존 예제도 1회 통과했으므로 단일 성공은 경합 부재의 증거가 아니다.

실제 소비 버전인 HC5 `5.6.4`의 JAR 바이트코드를 확인했다.
`HttpRequestFutureTask.cancel()`은 callable 취소 표시, 요청 취소,
`FutureTask.cancel()` 순서로 실행한다. callable이 취소 표시를 읽고 먼저 예외로
완료되면 마지막 `cancel()`은 `false`를 반환하고, `get()`은 취소 원인을
`ExecutionException`으로 감싼다. 빠른 GET이 먼저 정상 완료하는 경우도 있으므로
`cancel()` 호출 자체를 취소 성공으로 해석해서는 안 된다.

## 결정과 검증 구조

- 정상 GET·제한 시간·콜백 예제는 기존 컨테이너 기반 테스트에 유지한다.
- 취소 예제는 `ClientWithRequestFutureCancellationTest`로 분리한다.
  네트워크 응답 속도나 고정 sleep 대신 제출된 Runnable의 실행 순서를 제어한다.
- 실제 `futureRequestExecutionServiceOf`와 HC5 Future/callable을 사용한다.
  MockK는 executor의 제출과 HTTP I/O 경계만 대체한다. Future 결과나 취소 상태는
  모킹하지 않는다. 전용 동시성 테스터는 스트레스 검증에 적합하지만, 여기서는
  라이브러리 내부 두 단계 사이의 정확한 실행 순서를 지정해야 한다.
- 취소 콜백에서 제출된 Runnable을 실행해 callable 선행 완료를 강제한다.
  실행 전 취소, 감싸진 취소, 정상 완료, 일반 I/O 실패를 별도로 검증한다.
- 일반 예외를 모두 취소로 간주하는 catch나 공용 예외 정규화 계층은 추가하지 않는다.
  공개 API, 의존성 버전, CI 재시도 정책은 변경하지 않는다.

## 재현과 검증

- RED: 기존 직접 취소 예외 가정을 적용한 결정적 테스트가
  `Expected CancellationException but got ExecutionException`으로 실패했다.
  전체 4개 중 3개 통과, 1개 실패, 오류·제외 0개였다.
- GREEN: 경합 경로에서 `ExecutionException`의 직접 원인이
  `CancellationException`인지 검증한다. 기존 네트워크 예제를 포함해 5개 통과했다.
- `cleanTest --no-build-cache`를 포함한 대상 검증을 별도 JVM에서 5회 실행해
  매회 5개가 통과했다. HTTP 전체는 460개 중 457개 통과, 기존 비활성 3개,
  실패·오류 0개였다. Detekt 기존 지적은 변경 파일 밖에 남아 있다.
- 처음 반복 명령은 `cleanTest`만 사용해 테스트 결과가 `FROM-CACHE`로 복원됐다.
  이를 반복 실행 증거에서 제외하고 `--no-build-cache`로 다시 검증했다.
  앞으로 실행 횟수를 보고할 때 테스트 태스크의 실제 실행 여부를 먼저 확인한다.
- 첫 회귀 테스트 작성 때 assertion 이름을 추정해 컴파일 오류가 발생했다.
  실제 `shouldBeSameInstanceAs` 선언을 확인해 수정했다. 컴파일 실패를 RED로
  계산하지 않았으며, 다음 변경부터 assertion 선언을 먼저 조회한다.

## 재발 방지

1. 취소 요청, Future의 취소 상태, 요청 실행 실패를 구분한다.
2. 경합 테스트는 먼저 필요한 사건 순서를 고정하고, 고정 sleep으로 순서를 추정하지 않는다.
3. 포장된 예외는 정확한 원인 타입을 확인하고 정상 완료·다른 실패 대조군을 둔다.
4. 재시도 성공을 초기 실패의 해결 근거로 삼지 않는다. 실제 JUnit과 첫 시도 로그를 대조한다.
5. 예제의 실행 제어를 바꾸더라도 실제 라이브러리 상태 전이는 검증 대상에 남긴다.

관련 기록: [#1651](https://github.com/bluetape4k/bluetape4k-projects/issues/1651),
[#1646](https://github.com/bluetape4k/bluetape4k-projects/pull/1646),
[기존 동시성 예제 분류](2026-05-18-infra-io-concurrency-migration.md).
