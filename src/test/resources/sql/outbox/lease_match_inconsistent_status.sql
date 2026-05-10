INSERT INTO notification_outboxes (
    id,
    notification_id,
    idempotency_key,
    status,
    processing_attempt,
    processing_lease_state,
    processing_started_at,
    next_attempt_at,
    receiver_id,
    channel,
    body,
    created_at,
    updated_at
) VALUES (
    1, 1, 'c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d',
    'RETRY_PENDING', 1, 'IDLE', '2026-05-10 12:00:00', '2026-05-10 12:00:30',
    101, 'EMAIL', '본문',
    '2026-05-10 11:00:00', '2026-05-10 12:00:00'
);
