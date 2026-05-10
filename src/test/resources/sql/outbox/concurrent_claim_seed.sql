INSERT INTO notifications (
    id, receiver_id, type, channel, ref_type, ref_id, body, idempotency_key, created_at
) VALUES
(1, 1001, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1001, '본문 1', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', '2026-05-11 11:00:00'),
(2, 1002, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1002, '본문 2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', '2026-05-11 11:00:01'),
(3, 1003, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1003, '본문 3', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', '2026-05-11 11:00:02'),
(4, 1004, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1004, '본문 4', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', '2026-05-11 11:00:03'),
(5, 1005, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1005, '본문 5', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5', '2026-05-11 11:00:04'),
(6, 1006, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1006, '본문 6', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa6', '2026-05-11 11:00:05'),
(7, 1007, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1007, '본문 7', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7', '2026-05-11 11:00:06'),
(8, 1008, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1008, '본문 8', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa8', '2026-05-11 11:00:07'),
(9, 1009, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1009, '본문 9', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa9', '2026-05-11 11:00:08'),
(10, 1010, 'ENROLLMENT_COMPLETED', 'EMAIL', 'ENROLLMENT', 1010, '본문 10', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab1', '2026-05-11 11:00:09');

INSERT INTO notification_outboxes (
    id, notification_id, idempotency_key, status, processing_attempt, processing_lease_state,
    processing_started_at, next_attempt_at, receiver_id, channel, body, created_at, updated_at
) VALUES
(1, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'PENDING', 0, 'IDLE', NULL, NULL, 1001, 'EMAIL', '본문 1', '2026-05-11 11:00:00', '2026-05-11 11:00:00'),
(2, 2, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'PENDING', 0, 'IDLE', NULL, NULL, 1002, 'EMAIL', '본문 2', '2026-05-11 11:00:01', '2026-05-11 11:00:01'),
(3, 3, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 'PENDING', 0, 'IDLE', NULL, NULL, 1003, 'EMAIL', '본문 3', '2026-05-11 11:00:02', '2026-05-11 11:00:02'),
(4, 4, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 'PENDING', 0, 'IDLE', NULL, NULL, 1004, 'EMAIL', '본문 4', '2026-05-11 11:00:03', '2026-05-11 11:00:03'),
(5, 5, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5', 'PENDING', 0, 'IDLE', NULL, NULL, 1005, 'EMAIL', '본문 5', '2026-05-11 11:00:04', '2026-05-11 11:00:04'),
(6, 6, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa6', 'PENDING', 0, 'IDLE', NULL, NULL, 1006, 'EMAIL', '본문 6', '2026-05-11 11:00:05', '2026-05-11 11:00:05'),
(7, 7, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7', 'PENDING', 0, 'IDLE', NULL, NULL, 1007, 'EMAIL', '본문 7', '2026-05-11 11:00:06', '2026-05-11 11:00:06'),
(8, 8, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa8', 'PENDING', 0, 'IDLE', NULL, NULL, 1008, 'EMAIL', '본문 8', '2026-05-11 11:00:07', '2026-05-11 11:00:07'),
(9, 9, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa9', 'PENDING', 0, 'IDLE', NULL, NULL, 1009, 'EMAIL', '본문 9', '2026-05-11 11:00:08', '2026-05-11 11:00:08'),
(10, 10, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab1', 'PENDING', 0, 'IDLE', NULL, NULL, 1010, 'EMAIL', '본문 10', '2026-05-11 11:00:09', '2026-05-11 11:00:09');
