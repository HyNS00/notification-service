# 일단 삽입하고 중복을 잡는 패턴, 그리고 별도 트랜잭션을 쓴 이유

## 상황

과제 C 는 "동일 이벤트에 대해 알림이 중복 발송되면 안 된다", "동시에 같은 요청이 여러 번 들어오는 경우도 고려하라"는 두 요구사항을 가진다.

같은 알림 요청에 대해 단 한 행만 등록되어야 하고, 같은 키로 동시에 여러 요청이 들어와도 결과가 동일해야 한다는 의미다.

문제는 이 멱등성을 자바 단에서 풀지, DB 단에서 풀지였다. 그리고 어느 쪽을 택하든 트랜잭션 경계를 어떻게 그어야 패턴이 안전하게 동작할지도 함께 결정해야 했다.

## 해석

멱등성을 풀 수 있는 방식은 크게 네 가지였다.

첫째, 등록 전에 같은 키가 있는지 먼저 조회하고, 없을 때만 INSERT 한다. find-first 패턴이다.

둘째, 일단 INSERT 를 시도하고 UNIQUE 제약 위반이 나면 충돌 응답으로 끊는다. save-then-409 패턴이다.

셋째, MySQL 의 `INSERT ... ON DUPLICATE KEY UPDATE` 같은 native 구문으로 한 번에 처리한다.

넷째, 일단 INSERT 를 시도하고 UNIQUE 제약 위반이 나면 catch 해서 기존 행을 조회해 같은 응답을 돌려준다. try saveNew + catch 패턴이다.

이 중 race-safe 한 패턴은 둘째, 셋째, 넷째다.

첫째 패턴은 race window 가 남는다. 두 스레드가 거의 동시에 같은 키로 사전 조회를 호출하면, 양쪽 다 빈 결과를 받고 둘 다 INSERT 로 진입한다. 결국 같은 키 두 행이 들어가거나, 한쪽만 UNIQUE 제약에 막혀 예외로 끝난다. 자바 단의 사전 조회는 race 의 게이트키퍼가 될 수 없다.

race 의 진짜 결정자는 DB 의 UNIQUE 제약이다. 동시 진입 두 스레드 중 한쪽만 INSERT 에 성공하고, 나머지는 반드시 UNIQUE 위반 예외를 받는다. 이 사실을 받아들이면 자바 단에서 할 일은 그 예외를 어떻게 처리할지 정하는 것뿐이다.

## 외부 응답 의미

남은 셋째, 넷째 중 어느 쪽을 택할지는 클라이언트에 어떤 응답을 돌려줄지에 달려 있었다.

같은 키 재요청이 *에러* 라면 409 Conflict 가 자연스럽다. 클라이언트는 이미 등록되었음을 알고 별도 분기를 태운다.

같은 키 재요청이 *정상 흐름* 이라면 200 OK 가 자연스럽다. 클라이언트는 신규 등록과 중복 재요청을 구분할 필요 없이 같은 자원의 id 를 받는다.

본 과제의 시나리오는 후자에 가깝다고 해석했다. 이벤트 발생 → 알림 등록 흐름에서 클라이언트의 네트워크 재시도, 워커 재기동, 중복 디스패치 같은 이유로 같은 키 요청이 다시 들어오는 것은 정상이다. 클라이언트가 이를 에러로 받아 분기 코드를 박는 것은 부담이고, REST 의 멱등 의미와도 어긋난다.

따라서 같은 키 재요청은 silent dedup 으로 처리하기로 했다. 신규 INSERT 든 중복이든 응답 모양은 `{ "id": 42 }` 로 같다.

이 결정을 따라가면 셋째와 넷째 중에는 넷째가 남는다. 셋째 (`INSERT ... ON DUPLICATE KEY UPDATE`) 는 MySQL 전용 native 구문이라 H2 와 다른 DB 로의 포팅이 어려워진다. JPA 영속성 컨텍스트와도 충돌 가능성이 있다.

넷째는 JPA 의 표준 흐름 안에서 동작하고, DB 별 차이는 UNIQUE 위반 예외의 SQLState/ErrorCode 시그니처만 따로 분류하면 된다.

## 별도 트랜잭션이 필요한 이유

넷째 패턴을 그대로 단일 트랜잭션 안에 둘 수는 없었다. 트랜잭션 경계를 잘못 잡으면 catch 분기 자체가 동작하지 않는다.

이유는 두 가지다.

### 부모 트랜잭션이 rollback-only 로 마킹된다

같은 트랜잭션 안에서 INSERT 가 UNIQUE 위반을 일으키면, 그 트랜잭션은 commit 불가능 상태가 된다. catch 로 예외를 잡아도 이미 트랜잭션은 죽어 있어, fallback 으로 `findByIdempotencyKey` 를 호출해도 그 SELECT 가 commit 될 수 없다.

자식 트랜잭션을 분리하면 INSERT 가 자식 안에서 롤백되고 부모 트랜잭션은 영향을 받지 않는다. 부모는 catch 이후 같은 트랜잭션 안에서 fallback 조회를 마저 진행하고 정상 commit 된다.

이 분리를 위해 INSERT 묶음을 `NotificationCreator.saveNew(...)` 라는 별도 메서드로 빼고 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 를 붙였다.

### INSERT 가 트랜잭션 *바깥* 에서 발행되면 catch 가 못 잡는다

JPA 의 기본 flush 시점은 트랜잭션 commit 시점이다. SEQUENCE 같은 deferred id 전략에서는 INSERT 가 영속성 컨텍스트에 쌓여 있다가 commit 시점에 한꺼번에 발행된다.

이 경우 UNIQUE 위반은 자식 트랜잭션이 commit 되는 *바깥* 에서 발생해, 호출자의 catch 절이 그 예외를 잡을 수 없다.

이를 막기 위해 자식 메서드 마지막에 `entityManager.flush()` 를 명시적으로 호출했다. INSERT 를 자식 메서드 *안에서* 발행시켜, UNIQUE 위반이 같은 메서드 안에서 throw 되고 호출자 catch 가 잡을 수 있게 한다.

현재 두 엔티티 모두 IDENTITY id 전략이라 `save()` 시점에 INSERT 가 즉시 발행되어 `flush()` 는 사실상 no-op 이다. 그러나 id 전략이 추후 SEQUENCE 등으로 바뀌어도 catch 흐름이 깨지지 않도록 방어적으로 두었다. 호출자가 *"이 메서드 안에서 UNIQUE 가 터지는 것을 기대한다"* 는 의도를 코드로 드러내는 효과도 있다.

## 두 행을 한 자식 트랜잭션에 묶는 일관성

본 프로젝트의 `NotificationCreator.saveNew(...)` 는 `notification` 한 행과 `notification_outbox` 한 행을 같은 멱등성 키로 같이 INSERT 한다. 두 행 다 같은 키에 UNIQUE 제약을 가진다.

두 INSERT 가 같은 자식 트랜잭션 안에 있으므로 어느 쪽 UNIQUE 가 먼저 깨지든 양쪽 다 롤백된다. 호출자는 단일 catch 분기로 흐르고, notification 만 있고 outbox 가 없는 좀비 상태가 만들어지지 않는다.

만약 두 INSERT 를 별도 자식 트랜잭션으로 쪼갰다면 첫 INSERT 가 commit 된 뒤 두 번째에서 UNIQUE 가 깨지는 시나리오에서 일관성이 깨질 수 있다. 호출자가 하나뿐인 현 시점에서는 두 행 INSERT 를 한 묶음으로 두는 편이 더 안전하고 명확하다.

## UNIQUE 위반을 가려내는 분류기

`DataIntegrityViolationException` 은 UNIQUE 위반만 담는 게 아니다. NOT NULL 위반, FK 위반 등도 같은 클래스로 올라온다. 따라서 catch 절이 받은 예외가 정말 UNIQUE 위반인지 가려내야 dedup 분기로 안전하게 흐를 수 있다.

`DuplicateKeyDetector` 가 그 역할을 한다. cause chain 을 타고 내려가 SQLException 의 SQLState 와 ErrorCode 시그니처를 본다. MySQL 은 `(23000, 1062)`, H2 는 `23505` 다.

UNIQUE 위반이면 fallback 조회로 흐르고, 그 외 제약 위반이면 그대로 propagate 해 핸들러에서 500 으로 처리한다.

클래스명을 DB 중립적인 `DuplicateKeyDetector` 로 두어 추후 PostgreSQL 등이 추가될 때 같은 클래스에 시그니처 한 줄만 더하면 되도록 했다.

## 결과

알림 등록은 일단 두 행을 INSERT 하고, UNIQUE 위반이 나면 같은 멱등성 키로 기존 알림을 조회해 그 id 로 응답한다.

INSERT 묶음은 `REQUIRES_NEW` 로 부모 트랜잭션과 분리하고, 메서드 마지막의 `flush()` 로 INSERT 시점을 메서드 안으로 끌어와 호출자 catch 가 예외를 잡을 수 있게 했다.

신규 등록과 중복 재요청 모두 200 OK + `{ "id": 42 }` 형태의 같은 응답을 돌려준다. 클라이언트는 신규/중복을 구분할 필요가 없다.

race 가 발생해도 DB 의 UNIQUE 제약이 단일 진입을 보장한다. 진 쪽은 첫 INSERT 라인에서 예외가 터지고, 같은 자식 트랜잭션 안의 두 번째 INSERT 까지 도달하지 못해 중간 상태가 남지 않는다.

다음 조건이 추가되면 패턴을 재검토한다.

- 클라이언트가 신규 등록과 중복 재요청을 구분해야 하는 시나리오가 등장하는 경우 (응답에 `created` 플래그를 추가하거나 409 옵션화)
- 두 번째 호출자 (P3 의 수동 재발행, 배치 등록) 가 등장해 `NotificationCreator` 의 두 행 INSERT 묶음을 `OutboxCreator` 와 분리할 가치가 생기는 경우
- DB 버전 변경으로 UNIQUE 위반 시그니처(SQLState/ErrorCode) 가 미묘하게 바뀌어 Detector 가 false negative 를 내는 경우
