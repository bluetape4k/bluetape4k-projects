# Hibernate encryption keyset 교훈 (2026-06-26)

관련 이슈: #816
영향 module: `:bluetape4k-hibernate`

## L1: persistent converter는 process-local key를 만들면 안 된다

### 문제

`AESStringConverter`와 `DeterministicAESStringConverter`는 persistent entity-field
converter로 문서화됐지만, 기본 encryptor는 process 안에서 생성한 Tink keyset을 사용했다.
같은 process 안의 test는 통과해도, restart 후나 다른 application instance에서는 저장된
ciphertext를 읽지 못할 수 있었다.

### 교훈

persistent encryption converter에는 외부에 저장된 명시적 key material이 필요하다. test는
같은 persisted keyset으로 restart-safe positive case를 검증하고, 다른 keyset으로
restart-unsafe negative case도 함께 검증해야 한다.

### 향후 가드

persistent encryption API를 추가할 때 same-instance round trip만 검증하지 않는다.
cross-instance 또는 cross-keyset regression test를 추가하고, key material이 어디에
저장되어야 하는지 문서화한다.
