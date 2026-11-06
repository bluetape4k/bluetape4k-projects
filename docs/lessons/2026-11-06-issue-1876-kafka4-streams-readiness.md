# Kafka4 KRaft Streams 테스트는 전송 전에 `RUNNING`을 기다린다

## 맥락

`infra/kafka4/src/test/kotlin/org/springframework/kafka/streams/KafkaStreamsTests.kt`는 `StreamsBuilderFactoryBean.stop()` 후 즉시 `start()`하고 두 레코드를 전송했다. KRaft 리더 선출과 changelog 복구가 끝나기 전에는 `NotLeaderOrFollowerException` 또는 메타데이터 복구 파일의 `NoSuchFileException`이 발생할 수 있으며, 기존 CI에서는 bounded retry가 이 lifecycle race를 가렸다. 테스트 종료 시 listener container·Streams·producer가 살아 있으면 broker destroy 단계에서도 같은 KRaft 저장소 경고가 발생했다.

## 결정

Streams state listener에서 `KafkaStreams.State.RUNNING`일 때만 `runningLatch`를 해제하고, 전송 전에 `runningLatch.await(60.seconds).shouldBeTrue()`로 bounded readiness를 확인한다. 기존 `stateLatch`는 상태 전이 관찰 계약으로 유지하고, readiness와 관찰을 같은 latch로 합치지 않는다.

## 결과

Kafka4 Streams 테스트가 broker와 pipeline 준비 완료 이후에만 producer를 호출하므로 stop/restart 직후의 전송 순서가 명시적인 계약이 되었다. 테스트 코드는 Kafka3 대응 구현과 같은 state-listener 패턴을 사용하며 새 dependency나 공개 API를 추가하지 않는다.

## 검증

- RED: readiness 계약 테스트가 기존 코드에서 전송 전 `RUNNING` await 부재로 실패했다.
- GREEN: 동일 계약 테스트 통과.
- `./gradlew :bluetape4k-kafka4:test --tests 'org.springframework.kafka.streams.KafkaStreamsTests' --no-daemon --no-configuration-cache --console=plain` 통과.
- 동일 targeted 테스트 3회 반복 모두 `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-kafka4:test --no-daemon --no-configuration-cache --console=plain`에서 342 tests 통과.
- 결과 대기 timeout을 600초에서 60초로 줄여 readiness 이후 downstream 정지 시에도 bounded failure를 남긴다.
- Kafka Infra CI에서 `nick-fields/retry`를 제거하고 readiness contract test를 명시적으로 실행해 최종 green으로 실패를 숨기지 않는다.
- `@AfterEach`에서 listener registry, Streams, `KafkaTemplate`, `DefaultKafkaProducerFactory`를 이 순서로 종료하고 `@DirtiesContext(AFTER_CLASS)`로 broker context 재사용을 막아 `KafkaStreamsTests`의 KRaft shutdown warning을 제거한다.

## 놓친 점과 다음 방지책

`stateLatch`가 한 번의 상태 전이만 관찰하고 있다는 사실을 Streams가 실제로 `RUNNING`인지 확인하는 readiness 증거로 잘못 사용했다. 이후 `stop()`/`start()`를 포함하는 Kafka Streams 테스트는 producer/consumer 작업 전에 명시적인 terminal readiness state와 bounded await를 함께 고정한다. CI는 재시도로 lifecycle 실패를 감추지 않고 contract test와 단일 Gradle 실행의 결과를 보존한다. 테스트 종료 순서를 바꾸는 경우에는 broker destroy 전에 listener, Streams, producer가 모두 종료됐는지 uncached 실행 로그에서 Kafka 저장소 예외가 없는지 확인한다.
