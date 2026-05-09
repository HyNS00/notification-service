INSERT INTO notifications (
    id,
    receiver_id,
    type,
    channel,
    ref_type,
    ref_id,
    body,
    idempotency_key,
    created_at
) VALUES (
    1,
    1,
    'ENROLLMENT_COMPLETED',
    'EMAIL',
    'ENROLLMENT',
    100,
    '수강 신청이 완료되었습니다.',
    'c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d',
    '2026-05-09 12:00:00'
);
