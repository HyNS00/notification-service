INSERT INTO notifications (id, receiver_id, type, channel, ref_type, ref_id, body, idempotency_key, read_at, created_at) VALUES
(1, 1, 'ENROLLMENT_COMPLETED', 'IN_APP', 'ENROLLMENT', 100, '수강 신청이 완료되었습니다.', 'a3f7b8c2d5e9014687f3a2b1c4d5e6f7891a2b3c4d5e6f7081a2b3c4d5e6f708', NULL,                  '2026-05-09 10:00:00'),
(2, 1, 'PAYMENT_CONFIRMED',    'IN_APP', 'PAYMENT',    101, '결제가 확정되었습니다.',     'b4e0c5d6e7f8901a2b3c4d5e6f70819273645566778899aabbccddeeff001122', '2026-05-10 09:00:00', '2026-05-09 11:00:00'),
(3, 1, 'ENROLLMENT_COMPLETED', 'EMAIL',  'ENROLLMENT', 102, '수강 신청이 완료되었습니다.', 'c5f1d6e7f8901a2b3c4d5e6f708192736455667788990011223344556677889a', NULL,                  '2026-05-09 12:00:00'),
(4, 1, 'COURSE_START_D1',      'IN_APP', 'COURSE',     103, '강의 시작이 하루 남았습니다.', 'd6027e8f9a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f607182930a1b', NULL,                  '2026-05-09 13:00:00'),
(5, 1, 'ENROLLMENT_CANCELLED', 'IN_APP', 'ENROLLMENT', 104, '수강 신청이 취소되었습니다.', 'e71308f9a0b1c2d3e4f50617283940a1b2c3d4e5f60718293a4b5c6d7e8f90a1', NULL,                  '2026-05-09 13:00:00'),
(6, 2, 'ENROLLMENT_COMPLETED', 'IN_APP', 'ENROLLMENT', 200, '수강 신청이 완료되었습니다.', 'f824019a0b1c2d3e4f50617283940a1b2c3d4e5f60718293a4b5c6d7e8f90a1b', NULL,                  '2026-05-09 14:00:00'),
(7, 2, 'PAYMENT_CONFIRMED',    'EMAIL',  'PAYMENT',    201, '결제가 확정되었습니다.',     '0935a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e', NULL,                  '2026-05-09 15:00:00');
