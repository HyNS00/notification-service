# 요구사항 해석

명세 추가 제출물 2 *"요구사항을 어떻게 해석했는지"* 의 답.

본 문서는 **개발 phase 순서대로** 정리한다. 각 phase 에서:
- *그 phase 가 답하려는 명세 요구* 를 어떻게 해석했고
- *명세에 없는 부분* 을 어떤 가정으로 메웠으며
- *그 phase 에서 내린 결정* 의 요지가 무엇인지

를 한꺼번에 본다. Phase 순서가 곧 결정의 의존 순서다 (P1 등록 → P1-1 멱등성 → P1.5 인프라 → P2 비동기 발송 → P4 조회).

마지막에 *phase 횡단* 으로 묶어야 할 항목 (명세 선택 구현 4 항목 미적용 사유 / 재해석 여지) 을 따로 모은다.

---

## P1 — 등록 API + outbox 동기 저장

명세 1번 *"알림 발송 요청 API"* 와 3번 *"동일 이벤트 중복 발송 방지"* 의 토대를 만드는 단계.

### 요구사항 해석

- **"동일 이벤트" 의 정의** — 명세 3번의 *"동일 이벤트"* 를 `(receiverId, type, refType, refId, channel)` **5요소** 의 SHA-256 hex 64자로 정의한다. 5요소가 같으면 같은 알림. 근거: 명세 1번의 알림 발송 요청 페이로드가 정확히 이 5요소. *이 5요소가 같다면 동일 이벤트로 본다* 가 자연스러운 해석이다.
- **"발송 채널(EMAIL / IN_APP)" 단수 해석** — *한 요청 = 한 채널*. 같은 알림을 EMAIL + IN_APP 둘 다 보내려면 두 번 POST. 명세 페이로드 예시도 `channel` 단수형. 멀티 채널이 필요하면 클라이언트가 두 번 호출하는 모델.
- **등록 API 책임 경계 — 발송 X, 큐 적재까지** — `POST /api/notifications` 의 책임은 *알림 등록* 까지. *발송 자체는 비동기 워커가 따로 한다.* 명세 1번 "발송 요청 (전송 X)" 표현과 2번 "비동기 처리" 요구의 자연스러운 분리.

### 자체 가정

- **`body` 길이 VARCHAR(500)** — 본문 길이는 명세에 없음. 운영 환경에서 한국어 알림 (수강 신청 / 결제 확정 등) 의 자연스러운 한계로 500자 가정. 추후 템플릿 시스템 도입 시 재검토.
- **`refId` 단일 BIGINT** — 명세 *"참조 데이터(이벤트 ID, 강의 ID 등)"* 의 *"등"* 은 metadata 가변성 신호로 해석 가능. 본 과제 범위에서는 단일 ID 면 충분하다고 가정.
- **`notifications` 에 별도 `status` 컬럼 없음** — 큐/재시도 상태가 사용자 가시 결과와 섞이지 않도록 분리. status 는 응답 DTO 가 `sent_at` / `failed_at` 으로 derive.

### 결정 (요지)

- **두 테이블 분리** — `notifications` (영구 / 사용자 가시) + `notification_outboxes` (휘발 / 워커 큐). 책임도 보존도 가시성도 모두 다르다.
- **outbox 가 dispatch 스냅샷 자체 보유** — `receiver_id`, `channel`, `body` 를 outbox 컬럼으로 가짐. 워커 claim 쿼리가 join 없이 단일 테이블 scan.
- **양쪽 테이블에 `idempotency_key` UNIQUE** — 사용자 시점 + 워커 시점 가드 둘 다.

---

## P1-1 — 멱등 등록 흐름 (try saveNew + catch)

명세 3번 *"동시에 같은 요청이 여러 번 들어오는 경우도 고려"* 의 race-safe 보장을 자바 / DB 레벨에서 어떻게 다룰지 결정하는 단계.

### 요구사항 해석

- **"동시에 같은 요청" — DB UNIQUE 가 진짜 race 결정자** — 자바 락 (synchronized / `ReentrantLock`) 이나 분산 락 (Redis / Zookeeper) 없이 **DB UNIQUE 제약** 만으로 race 결정. *INSERT 시도 → 성공 또는 UNIQUE 위반 → 분기*. 자바 단의 try/catch 는 *dedup 분기일 뿐 race 의 결정자가 아니다*.
- **"운영 환경으로 전환 가능" — DB 추상화** — `DuplicateKeyDetector` 가 DB 별 시그니처 (MySQL: SQLState=23000 / ErrorCode=1062, H2: SQLState=23505, PostgreSQL: SQLState=23505) 를 한 클래스에서 분기. 클래스명에 DB 를 포함시키지 않음 → PostgreSQL 추가 시 한 줄 확장.

### 자체 가정

- **신규 / 중복 응답 동일 (silent dedup)** — 클라이언트가 *신규* 인지 *중복* 인지 구분할 필요가 없다고 가정. 응답 body 는 둘 다 `{"id": N}`, HTTP status 만 `202` (신규) vs `200` (중복) 으로 미세하게 구분. REST 의미와 자연스럽게 맞음.
- **race 의 본질적 한계** — 같은 5요소가 *완전히 동시에* 들어오면 DB UNIQUE 가 둘 중 *어느 쪽* 을 성공시킬지는 commit 타이밍에 달림. 어느 쪽이 *최초* 인지는 클라에게 의미 없음 — 결과만 같으면 됨.

### 결정 (요지)

- **`@Transactional(REQUIRES_NEW)` + `entityManager.flush()` 의 *왜***:
  - `REQUIRES_NEW` — 자식 트랜잭션 분리. UNIQUE 위반으로 자식이 rollback-only 마킹되어도 부모 catch 가능.
  - `flush()` — JPA 기본 flush 시점은 커밋 시점. 명시적 flush 로 INSERT 를 메서드 *안에서* 발행 → 같은 메서드 안에서 예외 발생 → 호출자 catch 가능. SEQUENCE id 전략으로 바꿔도 catch 가 깨지지 않게.
- **`DuplicateKeyDetector` 명명** — DB 를 클래스명에 포함시키지 않음. *어떤 DB 가 들어와도 시그니처 분기 만 추가하면 됨*.
- **silent dedup → 200 OK + 기존 id** — `409 Conflict` 가 아니라 *같은 모양* 으로 응답. 클라가 retry 안전.

상세 분석 + 대안 비교 (find-first / INSERT…ON DUPLICATE / 409 Conflict) → [`reason2.md`](reason2.md).

---

## P1.5 — MySQL local profile + Docker

명세 제출물 *"실행 방법 (Docker 또는 로컬)"* 의 *"또는"* 을 어떻게 해석하고 인프라를 어떻게 구성할지 결정하는 단계.

### 요구사항 해석

- **"Docker 또는 로컬" 의 "또는" 을 *양립 가능* 으로 해석** — 채점자가 *Docker 로 한 줄 실행* 도, *IDE 직접 실행* 도 모두 가능해야 함. compose 가 `mysql + app` 둘 다 띄우고, IDE 실행 시엔 `docker compose up -d mysql` 만 해도 됨.
- **"H2 / MySQL / PostgreSQL 중 선택" — MySQL 단독** — 운영 시뮬레이션이 가장 자연스러운 선택. `FOR UPDATE SKIP LOCKED` 안정 지원도 기준.
- **테스트는 H2 유지** — Testcontainers 도입 비용 대비 가치 부족. `MODE=MySQL` 호환으로 P2 동시성 테스트도 커버.

### 자체 가정

- **`prod` profile 미작성** — 과제는 prod 시나리오 미포함. 향후 필요 시 yaml 한 개 + 환경변수 매핑으로 확장 가능.
- **`MYSQL_HOST_PORT=3307`** 같은 환경변수의 *기본값* 으로 충분. 채점자가 기본값 (3306, 8080) 으로 실행할 거라 가정.

### 결정 (요지)

- **Spring Boot Actuator 미도입** — `/actuator/health` 기반 app healthcheck 는 mysql healthcheck + `depends_on: service_healthy` 만으로 race 차단 가능.
- **Docker app 포트 range 매핑** (`8080-8089:8080`) — `docker compose --scale app=N` 시 호스트 포트가 자동 배분. 다중 인스턴스 시연 핵심.

---

## P2 — outbox 비동기 발송 코어 + Cleanup

명세 필수 5종 (비동기 / 재시도 / 멱등성 / 복구 / 다중 인스턴스) 의 *직접 답* 을 만드는 핵심 단계.

### 요구사항 해석 (명세 필수 5종 직접 매핑)

- **"비즈니스 트랜잭션과 분리된 비동기 발송"** — 등록 트랜잭션은 *INSERT 2건* (notifications + outbox `PENDING`) commit 만 한다. dispatch 는 워커가 *별도 스레드 + 별도 트랜잭션 + DB lock 미보유 IO* 단계에서 수행. 등록 응답은 외부 IO 에 묶이지 않음.
- **"일시적 장애 시 재시도"** — `RetryExceptionClassifier` 가 retryable / non-retryable 분류. retryable 은 지수백오프 + ±20% jitter + 최대 5회. *운영 환경 전환 시 즉시 동작*.
- **"동일 이벤트 중복 발송 방지" — 워커 시점 가드** — P1-1 에서 사용자 시점 멱등성을 처리했고, P2 에서는 *워커 시점* 가드. `FOR UPDATE SKIP LOCKED` (claim) + lease timestamp exact-match (결과 저장) 두 단계.
- **"처리 중 상태 복구 + 서버 재시작 시 유실 X"** — outbox 가 DB 본체라 *서버 죽어도 PENDING / RETRY_PENDING 행은 그대로*. 다음 부팅 시 워커가 자연 재처리. PROCESSING 고착은 `OutboxTimeoutRecoveryWorker` (60s timeout) 가 복구.
- **"다중 인스턴스 환경 중복 처리 X"** — `FOR UPDATE SKIP LOCKED` 가 단일 진입 보장. 두 워커가 같은 batch 못 잡음. lease exact-match 가 *늦게 깨어난 워커의 결과 저장* 도 차단.

### 자체 가정 — 모든 수치

| 항목 | 값 | 근거 |
| --- | --- | --- |
| Polling min delay | 100 ms | 작업 폭주 시 거의 즉시 다음 batch claim |
| Polling max delay | 5 s | 작업 없을 때 휴식 한도. wake-up 실패 시에도 5초 안에 자연 회복 |
| Polling jitter | ±20 % | 다중 인스턴스 동시 깨어남 완화 |
| Claim batch size | 10 | 한 트랜잭션이 잡는 행 수 |
| Lease timeout | 60 s | mock dispatcher 라 dispatch 자체는 ms 단위. GC pause / 장애 마진 |
| Recovery 주기 | 30 s | lease timeout 의 절반 |
| Recovery batch | 100 | 한 사이클 회복 행 수 |
| Retry max attempts | 5 | 명세에 없음. 시연 + 운영 일반 기준 |
| Retry base delay | 5 s | 첫 재시도 |
| Retry multiplier | 2 | 지수 백오프 |
| Retry max delay | 5 min (300s) | 5번째 시도가 5분 안에 떨어짐 |
| Retry jitter | ±20 % | 다중 인스턴스 동시 재시도 완화 |
| DISPATCHED retention | 7 일 | 시연 + 단기 디버깅 충분 |
| FAILED retention | 30 일 | 운영 사후 분석 / 채점자 검증 여유 |
| Cleanup 주기 | 1 h | 본 과제 규모에 충분 |
| Cleanup batch | 500 / cycle | 한 batch 가 짧게 락 잡고 풀도록 |

모두 `@ConfigurationProperties` 로 외부화 ([`global/config/Outbox*Properties.java`](../src/main/java/com/hyso/notifier/global/config/)). 코드 변경 없이 application properties 만으로 조정 가능.

### 결정 (요지)

- **두 테이블 결과 필드 dual-write** — `notifications.sent_at / failed_at / failure_reason` (영구) + `notification_outboxes.dispatched_at / failed_at / failure_reason` (휘발). `OutboxResultPersister.persist` 안에서 한 트랜잭션으로 둘 다 갱신.
- **`RETRY_PENDING` 시 `notifications` 미반영** — `notifications` 는 *최종 결과* 만 (DISPATCHED / FAILED 종결 시점만). RETRY_PENDING 시도별 사유는 outbox 휘발로 보유. 사용자에게 "처리 중" 상태가 일관되게 보임.
- **사용자 가시 status 컬럼 없음 — timestamp derive 일관성** — P1 의 결정을 P2 에서 유지. invariant 위반 차단.
- **재시도 모델: DB-level only** — Spring `RetryTemplate` 미사용. outbox 의 `RETRY_PENDING` + `processing_attempt++` 로만 처리. *in-process retry 도 의도적 미도입* — claim 한 워커가 in-process 로 N번 시도하면 *그 워커 죽으면 N 번 모두 잃음*. DB-level retry 가 다중 인스턴스 / 재시작 안전.
- **`next_attempt_at` 컬럼** — backoff 시간 비교를 claim 쿼리 안에서 함. DB 시간 함수에 의존 안 함 (Spring 의 `Clock` 빈 → application 시계 일관성).
- **lease timestamp exact-match** — 다른 워커가 가로챈 경우 timestamp 가 다르므로 결과 저장 시 0건 갱신 → 자연 차단. 추가 분기 / lock 불필요.
- **adaptive polling** — 작업 없을 때 max delay 까지 점진 증가, 작업 있을 때 min delay 로 reset. 다중 인스턴스에서 jitter 로 동시 깨어남 완화.
- **wake-up 이벤트** — Spring `ApplicationEventPublisher` + `@TransactionalEventListener(phase=AFTER_COMMIT)`. 등록 → 첫 dispatch 까지 *평균 100ms 이내*. in-process 한정 (다른 인스턴스는 polling).

---

## P4 — 조회 / 읽음 필터 API

명세 1번의 *"본인의 알림 목록 (읽음/안읽음 필터 포함)"* 과 *"현재 상태 조회"* 의 API 면을 구현하는 단계.

### 요구사항 해석

- **"본인의 알림" — 본인 자원 한정** — 비소유 / 비존재 모두 404 + `NOTIFICATION_NOT_FOUND`. *존재는 하지만 본인 게 아님* 을 403 으로 알려주면 *id 가 어떤 사용자 소유인지* 정보 누설.
- **"읽음/안읽음 필터" — 필터링 기능만** — 명세 1번의 *"읽음/안읽음 필터 포함"* 은 *조회 시 필터링* 만 요구. *상태 전이 액션* (PATCH `/{id}/read`) 은 선택 구현 3번 *"여러 기기에서 동시에 읽음 처리 요청이 오면..."* 안에서 *동시성 측면* 으로 등장 → 본 단계 미구현.
- **"사용자 알림 목록" — `?limit=N` 만** — 명세가 페이지네이션 미요구. *최신 N 개* 만 보여줌 (default 20, max 100).
- **"현재 상태 조회" — `status` 응답 필드** — `sent_at` / `failed_at` 으로 `PENDING / SENT / FAILED` derive. RETRY_PENDING 은 PENDING 과 묶임 (사용자 관점 "처리 중").

### 자체 가정

- **`read_at` 갱신은 외부 책임** — 본 시스템에 PATCH `/read` 가 없으므로 `read_at` 컬럼이 *어떻게* 채워지는지 본 시스템이 결정 안 함. 외부 시스템 (게이트웨이 / 외부 read tracker / 모바일 앱 + 별도 서비스) 또는 시연용 수동 SQL.
- **`users` 테이블 미존재 → X-User-Id 사용자 존재 검증 안 함** — 본 시스템에 `User` 엔티티가 없다. `X-User-Id: 999999` 같은 *존재하지 않는 사용자 id* 도 거부하지 않음. *해당 사용자 소유 알림이 없으므로 빈 결과만 응답*. 진짜 인증은 게이트웨이가 토큰 검증 후 헤더 주입하는 모델로 위임.
- **`receiverId` 미노출** — 응답 DTO 에 `receiverId` 없음. 사용자가 `X-User-Id` 로 자기 자신을 알고 있으므로.

### 결정 (요지)

- **`@RequestHeader("X-User-Id") Long userId` 직접 사용** — `HandlerMethodArgumentResolver` + `@CurrentUser` 추상화 미도입. 3 엔드포인트 규모에 추상화 비용 정당화 안 됨.
- **service 가 검증 책임** — repository 는 *저장소 접근만*. 입력 검증은 service 가 *비즈니스 메시지* 로. *어디서 검증을 하느냐* 에 따라 책임 분리 명확.
- **JPQL 단일 쿼리 + readFilter 분기** — `FOR UPDATE SKIP LOCKED` 불필요, native 미사용. `WHERE (:read IS NULL OR (:read = true AND read_at IS NOT NULL) OR (:read = false AND read_at IS NULL))` 식 NULL 가드.
- **인덱스 컬럼 ASC** — `(receiver_id, created_at, id)` 모두 ASC 선언. H2 호환성 + `ORDER BY DESC` 는 reverse scan 으로 처리.
- **`limit` 검증을 controller `@Validated` + `@Min` / `@Max`** — `ConstraintViolationException` 으로 떨어져 `GlobalExceptionHandler` 가 400 매핑.

---

## 명세 선택 구현 4 항목 — 미적용 사유

명세는 *"여유가 된다면 다음 항목도 고려해 주세요"* 로 4 항목을 제시한다. 각각 미적용 사유.

### 1. 발송 스케줄링 (특정 시각 발송 예약)

도메인 확장 부담:
- `scheduled_at` 컬럼 추가 + outbox claim 쿼리 변경 (`WHERE scheduled_at <= now()` 추가)
- 등록 → "예약" 분기 + 즉시 발송과 분리

본 과제 범위 외. 도입 시 outbox 의 `next_attempt_at` 인덱스를 재활용해 *예약 시각이 곧 next_attempt_at* 으로 모델링 가능 — 별도 컬럼 없이 통합 가능.

### 2. 알림 템플릿 관리

본 구현은 `NotificationMessageRenderer` 가 `type` 별 *고정 메시지* 반환 — *자리만* 마련. 운영 환경 도입 옵션:
- **DB 기반 템플릿** — `notification_templates` 테이블 + `type` / `language` 매핑
- **파일 기반 + 캐시** — `classpath:templates/*.hbs` 같은 Handlebars / Thymeleaf
- **외부 CMS 통합** — 마케팅팀이 직접 편집

본 과제 범위에서 *어떤 옵션이 적합한가* 까지 결정하기 어려움 → 자리만 마련.

### 3. 다중 디바이스 읽음 동시성 (PATCH `/{id}/read`)

명세 1번의 *읽음/안읽음 필터* 는 *필터링만* 요구. *상태 전이 액션* 은 선택 구현 3번 안에서 *다중 디바이스 동시성 측면* 으로 등장 → API 자체를 미구현.

도입 시 자연스러운 구현:
```sql
UPDATE notifications
SET read_at = :now
WHERE id = :id AND receiver_id = :userId AND read_at IS NULL
```

- 첫 PATCH 만 `read_at` 채움 (1 row updated)
- 이후 PATCH 는 0 row updated → idempotent
- *first-write-wins* 자연스럽게 동작 (DB UPDATE 의 row-level lock 만으로)
- 분산 락 / Redis 등 불필요

`read_at` 자체 갱신 경로는 *시스템 외부 책임* 으로 분리 (게이트웨이 / 외부 read tracker / 시연용 수동 SQL).

### 4. 최종 실패 알림 수동 재시도

`OutboxTimeoutRecoveryWorker` 가 stuck 상태를 자동 복구하므로 *운영 관리자용 수동 트리거 API* 미도입.

도입 시 자연스러운 구현:
- `POST /api/admin/notifications/{id}/retry` — admin 전용
- `WHERE status = 'FAILED'` 인 outbox 를 `RETRY_PENDING` + `processing_attempt = 0` 으로 reset
- 보안 / 권한 분리 필요 (관리자 인증)

본 과제는 admin endpoint 분리가 *과한 확장* 이라 판단.

---

## 재해석 여지가 있는 부분

채점자가 다르게 봤을 수 있는 지점을 솔직히 명시한다. *의도와 다른 해석을 빠르게 식별* 할 수 있게 한다.

- **`refId` 단일 BIGINT vs `ref_metadata JSON`** — 명세 *"참조 데이터(이벤트 ID, 강의 ID 등)"* 의 *"등"* 을 *단일 ID 면 충분* 으로 봤지만, *가변 metadata 필요* 로도 해석 가능. JSON 필드면 더 풍부.
- **silent dedup (200 OK) vs `409 Conflict`** — 멱등 재요청 시 *같은 모양 200* 으로 응답하는 게 클라 retry 안전 + REST 의미와 자연스러움. *명시적 409 Conflict + Location 헤더* 가 더 명시적이라고 볼 수도 있음.
- **PATCH `/{id}/read` 미구현** — 명세 1번의 *읽음/안읽음 필터* 가 *필터링만* 인지 *상태 전이 액션 포함* 인지 해석 갈림. 본 구현은 선택 구현 3번으로 분류해 미구현. *필터링과 액션은 같은 1번 안* 이라고 보면 본 단계 구현이 자연스러울 수도 있음.
- **404 vs 403 (비소유 자원)** — *존재하지만 본인 것 아님* 을 정보 누설 방지로 404 응답. *권한 부재 명시성* 측면에서 403 + `errorCode=FORBIDDEN` 도 정합.
- **사용자 가시 status 컬럼 없음 (timestamp derive)** — invariant 위반 차단을 위해 status 컬럼 미보유. *명시성* 측면에서 `notifications.status` enum 컬럼이 더 readable 하다고 볼 수도 있음.
- **단일 채널 (1요청=1채널) vs 멀티 채널** — `channel` 단수형 해석. `channels: [...]` 배열 입력도 자연스러운 확장.
- **inbox 패턴 미적용** — 본 시스템 API 가 *이미 정리된* 발송 요청을 받기 때문이라고 해석. *모든 비동기 시스템은 inbox 도 도입해야 한다* 는 입장에서는 누락으로 볼 수 있음 — 상세 [`reason.md`](reason.md).

---

## 참고 문서

- [`../README.md`](../README.md) — 프로젝트 개요
- [`architecture.md`](architecture.md) — 추가 제출물 1: 비동기 처리 구조 + 재시도 정책
- [`reason.md`](reason.md) — inbox 미적용 deep dive
- [`reason2.md`](reason2.md) — 멱등성 패턴 deep dive
- [`testing-strategy.md`](testing-strategy.md) — 테스트 전략
- [`과제C_알림발송시스템.md`](과제C_알림발송시스템.md) — 명세 원문
