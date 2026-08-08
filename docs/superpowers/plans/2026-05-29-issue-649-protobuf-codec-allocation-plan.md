# Issue 649 Protobuf 코덱 할당 계획

## 단계

1. `benchmark/protobuf-codec-benchmark`에 protobuf 코덱 전용 벤치마크를 추가한다.
2. Redisson protobuf 인코딩에서 `ByteArray`로 감싸는 방식을 `ByteBuf` 직접 쓰기로
   교체한다.
3. 왕복 변환과 직접 버퍼 크기를 검증하는 회귀 테스트를 추가한다.
4. 대상 테스트, 벤치마크 컴파일, 짧은 벤치마크 샘플, diff 위생 검사를 실행한다.
5. 이슈를 닫기 전에 #649에 벤치마크 증거를 게시한다.
