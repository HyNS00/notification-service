-- 시연용 더미 데이터 — `demo` profile 활성 시 자동 적재
-- INSERT IGNORE 로 idempotency_key UNIQUE 충돌 silently 무시 → 재부팅 시 안전
-- id 는 AUTO_INCREMENT 에 위임 (POST API 의 신규 알림 id 와 충돌 회피)

INSERT IGNORE INTO notifications
  (receiver_id, type, channel, ref_type, ref_id, body, idempotency_key,
   sent_at, failed_at, failure_reason, read_at, created_at)
VALUES
-- ① SENT + 읽음 (IN_APP) — 사용자 1
(1, 'ENROLLMENT_COMPLETED', 'IN_APP', 'ENROLLMENT', 1001,
 '수강 신청이 완료되었습니다.',
 'a3f7b8c2d5e9014687f3a2b1c4d5e6f7891a2b3c4d5e6f7081a2b3c4d5e6f708',
 '2026-05-10 11:00:00', NULL, NULL, '2026-05-10 12:00:00', '2026-05-10 10:00:00'),

-- ② SENT + 안 읽음 (IN_APP) — 사용자 1
(1, 'PAYMENT_CONFIRMED', 'IN_APP', 'PAYMENT', 2001,
 '결제가 확정되었습니다.',
 'b4e0c5d6e7f8901a2b3c4d5e6f70819273645566778899aabbccddeeff001122',
 '2026-05-10 14:00:00', NULL, NULL, NULL, '2026-05-10 13:00:00'),

-- ③ FAILED + 실패 사유 (EMAIL) — 사용자 1
(1, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1002,
 '수강 신청이 완료되었습니다.',
 'c5f1d6e7f8901a2b3c4d5e6f708192736455667788990011223344556677889a',
 NULL, '2026-05-10 15:30:00',
 'max attempts exceeded: IOException: smtp connection refused',
 NULL, '2026-05-10 15:00:00'),

-- ④ PENDING (sent_at / failed_at 모두 NULL) — 사용자 1
(1, 'COURSE_START_D1', 'IN_APP', 'COURSE', 3001,
 '강의 시작이 하루 남았습니다.',
 'd6027e8f9a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f607182930a1b',
 NULL, NULL, NULL, NULL, '2026-05-11 09:00:00'),

-- ⑤ SENT + 안 읽음 (격리 검증용) — 사용자 2
(2, 'ENROLLMENT_COMPLETED', 'IN_APP', 'ENROLLMENT', 4001,
 '수강 신청이 완료되었습니다.',
 'e71308f9a0b1c2d3e4f50617283940a1b2c3d4e5f60718293a4b5c6d7e8f90a1',
 '2026-05-11 08:30:00', NULL, NULL, NULL, '2026-05-11 08:00:00');
