package com.hyso.notifier.application.notification;

import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class IdempotencyKeyGenerator {

	public String generate(
			Long receiverId,
			NotificationType type,
			String refType,
			Long refId,
			NotificationChannel channel
	) {
		validate(receiverId, type, refType, refId, channel);

		String raw = "%d|%s|%s|%d|%s".formatted(
				receiverId,
				type.name(),
				refType,
				refId,
				channel.name()
		);
		return HexFormat.of().formatHex(sha256(raw));
	}

	private void validate(
			Long receiverId,
			NotificationType type,
			String refType,
			Long refId,
			NotificationChannel channel
	) {
		validateReceiverId(receiverId);
		validateType(type);
		validateRefType(refType);
		validateRefId(refId);
		validateChannel(channel);
	}

	private void validateReceiverId(Long receiverId) {
		if (receiverId == null) {
			throw new IllegalArgumentException("수신자 ID 는 비어 있을 수 없습니다.");
		}
	}

	private void validateType(NotificationType type) {
		if (type == null) {
			throw new IllegalArgumentException("알림 타입은 비어 있을 수 없습니다.");
		}
	}

	private void validateRefType(String refType) {
		if (refType == null || refType.isBlank()) {
			throw new IllegalArgumentException("참조 타입은 비어 있을 수 없습니다.");
		}
	}

	private void validateRefId(Long refId) {
		if (refId == null) {
			throw new IllegalArgumentException("참조 ID 는 비어 있을 수 없습니다.");
		}
	}

	private void validateChannel(NotificationChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException("발송 채널은 비어 있을 수 없습니다.");
		}
	}

	private static byte[] sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(input.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
