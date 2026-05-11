# 테스트 전략 메모

## Repository 테스트를 매번 작성해야 하는가

모든 repository 메서드를 기계적으로 테스트하지 않는다.

Spring Data JPA가 메서드 이름만으로 제공하는 단순 CRUD는 프레임워크 기능에 가깝다. 이런 메서드를 매번 테스트하면 테스트 수는 늘지만, 실제 설계 위험을 줄이는 효과는 작다.

대신 repository 테스트는 다음처럼 **프로젝트가 직접 책임지는 저장소 동작**을 검증할 때 작성한다.

- 직접 정의한 조회 메서드가 요구하는 조건으로 데이터를 찾는지
- unique 제약, not null 제약, FK 제약처럼 과제 요구사항과 연결된 DB 제약이 실제로 동작하는지
- native query, locking query, `FOR UPDATE SKIP LOCKED`처럼 DB 동작에 의존하는 쿼리가 의도대로 동작하는지
- adapter가 domain repository 포트를 올바르게 구현하는지
- outbox claim, lease 비교, timeout recovery처럼 트랜잭션/동시성 의미가 있는 저장소 동작인지

반대로 다음은 별도 repository 테스트를 생략할 수 있다.

- `save`, `findById`, `delete`처럼 Spring Data가 그대로 제공하는 단순 CRUD만 감싼 경우
- application/service 통합 테스트에서 이미 같은 저장 흐름을 충분히 검증하는 경우
- 비즈니스 의미가 없는 단순 enum, DTO, getter/setter 성격의 코드

## 이 프로젝트의 기준

notifier는 outbox 기반 알림 발송 시스템이다. 뒤 단계에서 worker, 재시도, timeout recovery, 다중 인스턴스 중복 방지를 검증해야 하므로 테스트는 실제 DB 상태를 기준으로 하는 편이 적합하다.

따라서 repository/integration 테스트는 `@DataJpaTest`의 트랜잭션 롤백에 기대기보다 다음 패턴을 기본으로 둔다.

- 테스트 스키마는 `src/test/resources/sql/schema.sql`로 명시한다.
- 각 통합 테스트 시작 전 `src/test/resources/sql/cleanup.sql`로 DB를 비운다.
- 필요한 fixture 데이터는 테스트별 `@Sql`로 삽입한다.
- 테스트 대상 코드가 저장하는 행은 실제 commit된 DB 상태로 검증한다.

이 방식은 `slack-bot-server`, `statistics-server`의 테스트 패턴을 차용한 것이다.

## 왜 트랜잭션 롤백만으로 충분하지 않은가

단순 repository 테스트에서는 rollback 기반 테스트도 편하다. 그러나 이 과제의 핵심 요구사항은 비동기 처리와 운영 시나리오다.

특히 다음 요구사항은 테스트 트랜잭션 내부의 uncommitted 데이터보다 실제 DB에 commit된 상태를 기준으로 검증하는 편이 명확하다.

- API 요청 트랜잭션과 발송 worker 트랜잭션 분리
- 서버 재시작 후 미처리 outbox 재처리
- unique 제약을 이용한 동일 이벤트 중복 저장 방지
- 여러 worker 또는 여러 인스턴스의 outbox claim 경쟁
- `processing_started_at` 기반 lease 유실 감지

테스트 트랜잭션이 열려 있으면 worker나 별도 트랜잭션이 테스트가 만든 데이터를 보지 못하는 문제가 생길 수 있다. 그래서 통합 테스트는 실제 DB에 반영된 상태를 기준으로 작성한다.

## 현재 단계의 적용

`NotificationRepositoryAdapterTest`는 다음 정도만 검증한다.

- `NotificationRepositoryAdapter`가 `NotificationRepository` 포트의 저장 동작을 수행한다.
- `idempotencyKey`로 기존 알림을 조회할 수 있다.
- 존재하지 않는 `idempotencyKey`는 `Optional.empty()`를 반환한다.

이 테스트는 adapter 구조와 멱등성 조회의 출발점을 고정하기 위한 최소 테스트다.

다만 앞으로 repository 테스트를 늘릴 때는 다음 기준을 따른다.

- 단순 CRUD wrapper는 테스트하지 않는다.
- 직접 작성한 쿼리와 DB 제약은 테스트한다.
- 동시성, lock, lease, 재시도 상태 전이는 repository 또는 integration 테스트로 검증한다.
- service 테스트에서 충분히 덮인 단순 저장 흐름은 repository 테스트를 중복 작성하지 않는다.

## 향후 P1~P3에서 추가할 가능성이 높은 repository 테스트

- `notifications.idempotency_key` unique 제약 테스트
- `notification_outbox.idempotency_key` unique 제약 테스트
- `notification_outbox.notification_id` unique 제약 테스트
- claim 대상 outbox를 `PENDING`, `RETRY_PENDING` 중 오래된 순서로 가져오는 테스트
- 동시에 여러 worker가 claim할 때 같은 outbox를 중복 claim하지 않는 테스트
- lease timestamp가 일치할 때만 처리 결과 저장이 성공하는 테스트
- timeout 된 `PROCESSING` outbox가 `RETRY_PENDING` 또는 `FAILED`로 복구되는 테스트
