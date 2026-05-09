package com.hyso.notifier.application.notification;

import com.hyso.notifier.infrastructure.notification.NotificationChannel;
import com.hyso.notifier.infrastructure.notification.NotificationType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class IdempotencyKeyGenerator {

	public String generate(
			Long receiverId,
			NotificationType type,
			String refType,
			Long refId,
			NotificationChannel channel
	) {
		Objects.requireNonNull(receiverId, "receiverId");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(refType, "refType");
		Objects.requireNonNull(refId, "refId");
		Objects.requireNonNull(channel, "channel");

		String raw = "%d|%s|%s|%d|%s".formatted(
				receiverId,
				type.name(),
				refType,
				refId,
				channel.name()
		);
		return HexFormat.of().formatHex(sha256(raw));
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
