# 이슈 833 - Spring WebClient README example

## 배경

`bluetape4k-spring-boot-core`의 양쪽 README는 `WebClient.httpGet`과
`WebClient.httpPost` helper를 문서화했다. 이 helper들은 내부에서 이미 `retrieve()`를
호출하고 `WebClient.ResponseSpec`을 반환하지만, README snippet은 다시 `retrieve()`를
chain했다. extension function 자체는 동작했지만 문서의 example은 compile되지 않았다.

## 결정

public helper contract는 변경하지 않고, 반환된 `ResponseSpec`을 직접 소비하도록 example을
수정한다.

- `httpGet("/users").bodyToFlux(User::class.java).asFlow()`
- `httpPost("/users", newUser).bodyToMono(User::class.java).awaitSingle()`

또한 README drift가 test에서 잡히도록 `WebClientReadmeExamplesTest`를 추가한다.
README file은 `httpGet` 또는 `httpPost` 뒤에 `.retrieve()`를 chain하면 안 되고, 문서화된
`bodyToFlux`와 `bodyToMono` chain은 실제 extension function에 대해 compile되어야 한다.

## 후속 가드

WebClient helper return type이 바뀌면 `README.md`, `README.ko.md`,
`WebClientReadmeExamplesTest`를 함께 업데이트한다. public snippet은 반환 type에서 실제로
가능한 바로 다음 호출을 보여줘야 한다.
