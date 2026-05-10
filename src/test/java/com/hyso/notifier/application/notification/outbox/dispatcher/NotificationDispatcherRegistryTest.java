package com.hyso.notifier.application.notification.outbox.dispatcher;

import com.hyso.notifier.application.notification.outbox.exception.UnsupportedDispatchChannelException;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationDispatcherRegistryTest {

    @Test
    void 등록된_채널로_dispatch_하면_해당_채널의_dispatcher_만_호출된다() {
        RecordingDispatcher emailDispatcher = new RecordingDispatcher(NotificationChannel.EMAIL);
        RecordingDispatcher inAppDispatcher = new RecordingDispatcher(NotificationChannel.IN_APP);
        NotificationDispatcherRegistry registry = new NotificationDispatcherRegistry(
                List.of(emailDispatcher, inAppDispatcher)
        );
        NotificationOutbox outbox = sampleOutbox(NotificationChannel.EMAIL);

        registry.dispatch(NotificationChannel.EMAIL, outbox);

        assertThat(emailDispatcher.callCount).isEqualTo(1);
        assertThat(emailDispatcher.lastDispatched).isSameAs(outbox);
        assertThat(inAppDispatcher.callCount).isZero();
    }

    @Test
    void 등록되지_않은_채널로_dispatch_하면_UnsupportedDispatchChannelException_을_던진다() {
        RecordingDispatcher emailDispatcher = new RecordingDispatcher(NotificationChannel.EMAIL);
        NotificationDispatcherRegistry registry = new NotificationDispatcherRegistry(
                List.of(emailDispatcher)
        );
        NotificationOutbox outbox = sampleOutbox(NotificationChannel.IN_APP);

        assertThatThrownBy(() -> registry.dispatch(NotificationChannel.IN_APP, outbox))
                .isInstanceOf(UnsupportedDispatchChannelException.class)
                .hasMessage("등록되지 않은 발송 채널입니다: IN_APP");
    }

    @Test
    void 같은_채널의_dispatcher_가_둘_이상_주입되면_Registry_생성에_실패한다() {
        RecordingDispatcher first = new RecordingDispatcher(NotificationChannel.EMAIL);
        RecordingDispatcher second = new RecordingDispatcher(NotificationChannel.EMAIL);

        assertThatThrownBy(() -> new NotificationDispatcherRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("같은 채널에 둘 이상의 dispatcher 가 등록되었습니다: EMAIL");
    }

    @Test
    void dispatch_에_null_채널을_넘기면_IllegalArgumentException_을_던진다() {
        NotificationDispatcherRegistry registry = new NotificationDispatcherRegistry(
                List.of(new RecordingDispatcher(NotificationChannel.EMAIL))
        );
        NotificationOutbox outbox = sampleOutbox(NotificationChannel.EMAIL);

        assertThatThrownBy(() -> registry.dispatch(null, outbox))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("발송 채널은 비어 있을 수 없습니다.");
    }

    private NotificationOutbox sampleOutbox(NotificationChannel channel) {
        return NotificationOutbox.create(
                1L,
                "idem-key-test",
                42L,
                channel,
                "테스트 본문",
                LocalDateTime.now()
        );
    }

    private static class RecordingDispatcher implements NotificationDispatcher {
        private final NotificationChannel channel;
        private NotificationOutbox lastDispatched;
        private int callCount;

        RecordingDispatcher(NotificationChannel channel) {
            this.channel = channel;
        }

        @Override
        public void dispatch(NotificationOutbox outbox) {
            this.lastDispatched = outbox;
            this.callCount++;
        }

        @Override
        public NotificationChannel getChannel() {
            return channel;
        }
    }
}
