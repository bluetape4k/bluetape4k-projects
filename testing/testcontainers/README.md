# module kommons-testcontainers

## 개요

[TestContainers](https://www.testcontainers.org) 라이브러리를 이용하여 다양한 서버를 로컬에서 실행할 수 있게 합니다.
이름에서도 알 수 있듯이, 개발 시에 대상 서버를 설치할 필요없이 테스트 시에만 Docker를 이용하여 로드하고, 테스트 완료 후에는 Docker process에서 제거되므로 테스트를 손쉽게 할 수 있다

## 제공되는 Server 들

### AWS

S3, DynamoDB, Dynalite (Local engine for DynamoDB) 를 지원합니다.

### JDBC Databases

Cockroach, MySQL5, MySQL8, MariaDB, PostgreSQL 을 지원합니다.

### Storage

Cassandra, ElasticSearch, Hazelcast, MongoDB, Redis, RedisCluster 등을 지원합니다.

### Message Queue

Kafka, Nats, Pulsar, RabbitMQ 를 지원합니다.

### Infrastructure

Consul, Jaeger, Prometheus, Vault, Zipkin를 지원합니다.

### Usage

테스트를 위해 일시적으로 원하는 서버를 Docker를 이용하여 실행할 수 있습니다.

#### 기본 사용법

```kotlin
// Launch MySQL Server
val mysqlServer = MySQL8Server.Launcher.mysql

// Get Datasource
val dataSource = mysqlServer.getDataSource()
```

```kotlin
val mysql = MySQLServer(useDefaultPort = true).apply {
    start()
    ShutdownQueue.register(this)            // JVM 종료 시 자동으로 close 되도록 합니다    
}
```

실행하면 로그에 다음과 같이 서버의 정보를 보여주면, System property로 설정됩니다.
Spring Framework 에서 사용할 때에는 property value에 `${testcontainers.mysql.host}`, `${testcontainers.mysql.port}` 로 설정하면 동적정보를
활용할 수 있습니다.

```log
Start S3mock Server:
   testcontainers.mysql.host = localhost
   testcontainers.mysql.port = 32977
   testcontainers.mysql.url = http://localhost:32977
```

#### 기본 Port 설정

`useDefaultPort=true` 를 수행하면, Docker에서 동적으로 할당하는 Port가 아닌 Server의 기본 Port를 사용할 수 있습니다.

```kotlin
// Launch server
val redisServer = RedisServer(useDefaultPort = true).apply { start() }

// Connect to server
val redisson = Redisson.create(redisServer.url)  // url is "redis://localhost:6379"
```

#### Spring Boot 환경에서 테스트 하기

Spring Boot 환경에서 testcontainers를 사용하기 위해서는 동적으로 할당되는 port 때문에 환경설정에 애를 먹을 수 있다.
이 것 때문에 위와 같이 container 실행 시에, testcontainers.mysql.port 값을 System property에 자동으로 설정하게 하고, 이를 Spring 환경설정 값으로 지정하면 된다.

```properties
spring.datasource.url=${testcontainers.mysql.url}
spring.redis.host=${testcontainers.redis.host}
spring.redis.port=${testcontainers.redis.port}
```

---

## Docker Desktop 대체제

Docker Desktop이 유료가 되어서 대체재로 lima 를 사용합니다.
모든 대체재가 Apple Silicon arm64 이미지가 아니면 실행하지 못합니다.
그마나 lima 가 가장 쉽고, 빠른 대체재입니다.

[Docker Desktop의 대체재를 찾아보자](https://byungwoo.oopy.io/0e5485ba-dc5e-4951-a611-81ce08291817)

를 참고하여 환경을 설정하면 됩니다.

## FAQ

### Q. Mac의 port 7000 이 사용 중이다.

Redis Cluster 는 기본 7000:7005 를 사용하는데, Mac 에서는 이미 사용 중이라고 합니다.
Mac AirPlay 를 중단하면 됩니다.

* [MacOS Montrey AirPlay Port 5000 and 7000](https://jaynamm.tistory.com/entry/%EB%A7%A5%EB%B6%81%EC%97%90%EC%84%9C-5000%EB%B2%88-%ED%8F%AC%ED%8A%B8%EA%B0%80-%EC%82%AC%EC%9A%A9%EC%A4%91%EC%9D%B4%EB%8B%A4-MacOS-Montrey)

### Q. Docker 재실행 시 address already in use 에러 해결

* [Docker address already in use](https://www.baeldung.com/linux/docker-address-already-in-use)
* [[Docker] bind:address already in use](https://steady-coding.tistory.com/488)
* [포트가 이미 할달되어 있어서 발생한 에러 해결](https://a-half-human-half-developer.tistory.com/18)

### Q. Mac 에서 Docker Desktop을 대체할 솔루션은?

Colima 를 사용하세요.

vm type 을 vz 로 설정하면, mount type을 9p로 설정할 수 없는 문제가 있습니다.
그래서 mount type=9p 를 우선 시 하고, vm type 은 vz 가 아닌 qemu 를 사용하도록 합니다.

```shell
$ brew install colima
$ colima start --mount-type=9p --cpu 4 --memory 4 --disk 64 --runtime docker
```

만약 기존 설정을 변경하고 싶다면, 다음과 같이 기존 VM 을 삭제하고 새롭게 시작해야 합니다.

```shell
$ colima stop
$ colima delete
```

* [Docker on Macs without Docker Desktop with Lima and Colima](https://patrickwthomas.net/docker-on-macs-without-docker-desktop/)

Colima 사용 시 환경설정에 `DOCKER_HOST` 를 추가해야 합니다.

```shell
# Colima 사용 
export DOCKER_HOST="unix://$HOME/.colima/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$HOME/.colima/docker.sock"
export TESTCONTAINERS_RYUK_DISABLED=true
```

### Q. Colima 사용 시

Colima 로 Docker 를 실행할 때 다음과 같은 예외가 발생할 수 있습니다. 이 때에는 mount type 을 `9p` 를 사용하면 된다

```shell
Error response from daemon: error while creating mount source path '<path>':
chown '<path>': operation not permitted
```

* [Colima and mounting volumes on MacOS](https://mpanin.me/posts/colima-and-mounting-volumes/)
