package com.hyso.notifier.presentation.notification;

import com.hyso.notifier.application.notification.NotificationService;
import com.hyso.notifier.application.notification.RegisterNotificationResult;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.infrastructure.notification.exception.NotificationNotFoundException;
import com.hyso.notifier.presentation.notification.dto.response.NotificationListResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationStatus;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void 새_알림이_등록되면_202와_id를_반환한다() throws Exception {
        given(notificationService.register(any())).willReturn(RegisterNotificationResult.created(42L));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void 중복_요청이면_200과_기존_id를_반환한다() throws Exception {
        given(notificationService.register(any())).willReturn(RegisterNotificationResult.existing(42L));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void receiverId가_없으면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ENROLLMENT_COMPLETED",
                                  "channel": "EMAIL",
                                  "refType": "ENROLLMENT",
                                  "refId": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("수신자 ID 는 비어 있을 수 없습니다."));
    }

    @Test
    void refType이_빈_문자열이면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": 1,
                                  "type": "ENROLLMENT_COMPLETED",
                                  "channel": "EMAIL",
                                  "refType": "",
                                  "refId": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("참조 타입은 비어 있을 수 없습니다."));
    }

    @Test
    void 알_수_없는_type_값이면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": 1,
                                  "type": "UNKNOWN_TYPE",
                                  "channel": "EMAIL",
                                  "refType": "ENROLLMENT",
                                  "refId": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 본문을 해석할 수 없습니다."));
    }

    @Test
    void 알_수_없는_channel_값이면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": 1,
                                  "type": "ENROLLMENT_COMPLETED",
                                  "channel": "FAX",
                                  "refType": "ENROLLMENT",
                                  "refId": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 본문을 해석할 수 없습니다."));
    }

    @Test
    void JSON이_깨져있으면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 본문을 해석할 수 없습니다."));
    }

    @Test
    void 본인_소유_알림을_조회하면_200과_응답_DTO를_반환한다() throws Exception {
        given(notificationService.findOne(eq(1L), eq(42L))).willReturn(sentResponse());

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.type").value("ENROLLMENT_COMPLETED"))
                .andExpect(jsonPath("$.channel").value("IN_APP"))
                .andExpect(jsonPath("$.refType").value("ENROLLMENT"))
                .andExpect(jsonPath("$.refId").value(100))
                .andExpect(jsonPath("$.body").value("수강 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void sentAt만_있으면_status가_SENT로_내려온다() throws Exception {
        given(notificationService.findOne(any(), any())).willReturn(sentResponse());

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void failedAt만_있으면_status가_FAILED로_내려온다() throws Exception {
        given(notificationService.findOne(any(), any())).willReturn(failedResponse());

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("smtp connection refused"));
    }

    @Test
    void sentAt도_failedAt도_없으면_status가_PENDING으로_내려온다() throws Exception {
        given(notificationService.findOne(any(), any())).willReturn(pendingResponse());

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 존재하지_않거나_타인_소유면_404와_NOTIFICATION_NOT_FOUND를_반환한다() throws Exception {
        given(notificationService.findOne(any(), any()))
                .willThrow(new NotificationNotFoundException(42L));

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
    }

    @Test
    void X_User_Id_헤더가_없으면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications/42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("사용자 식별 헤더가 비어 있을 수 없습니다."));
    }

    @Test
    void X_User_Id가_숫자가_아니면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
    }

    @Test
    void 응답에_receiverId는_노출되지_않는다() throws Exception {
        given(notificationService.findOne(any(), any())).willReturn(sentResponse());

        mockMvc.perform(get("/api/notifications/42").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverId").doesNotExist());
    }

    private String validRequestJson() {
        return """
                {
                  "receiverId": 1,
                  "type": "ENROLLMENT_COMPLETED",
                  "channel": "EMAIL",
                  "refType": "ENROLLMENT",
                  "refId": 100
                }
                """;
    }

    @Test
    void 목록을_조회하면_200과_items를_반환한다() throws Exception {
        given(notificationService.findList(any(), any(), anyInt()))
                .willReturn(new NotificationListResponse(List.of(sentResponse())));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].status").value("SENT"));
    }

    @Test
    void 목록_조회시_read_파라미터가_없으면_null로_서비스를_호출한다() throws Exception {
        given(notificationService.findList(any(), any(), anyInt()))
                .willReturn(new NotificationListResponse(List.of()));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(notificationService).findList(1L, null, 20);
    }

    @Test
    void 목록_조회시_read_true면_true로_서비스를_호출한다() throws Exception {
        given(notificationService.findList(any(), any(), anyInt()))
                .willReturn(new NotificationListResponse(List.of()));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1").param("read", "true"))
                .andExpect(status().isOk());

        verify(notificationService).findList(1L, true, 20);
    }

    @Test
    void 목록_조회시_read_false면_false로_서비스를_호출한다() throws Exception {
        given(notificationService.findList(any(), any(), anyInt()))
                .willReturn(new NotificationListResponse(List.of()));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1").param("read", "false"))
                .andExpect(status().isOk());

        verify(notificationService).findList(1L, false, 20);
    }

    @Test
    void 목록_조회시_limit이_없으면_기본값_20으로_서비스를_호출한다() throws Exception {
        given(notificationService.findList(any(), any(), anyInt()))
                .willReturn(new NotificationListResponse(List.of()));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(notificationService).findList(1L, null, 20);
    }

    @Test
    void 목록_조회시_limit이_0이면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("limit 은 1 이상이어야 합니다."));
    }

    @Test
    void 목록_조회시_limit이_101이면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("limit 은 100 을 넘을 수 없습니다."));
    }

    @Test
    void 목록_조회시_limit이_숫자가_아니면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications").header("X-User-Id", "1").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
    }

    @Test
    void 목록_조회시_X_User_Id가_없으면_400과_INVALID_INPUT을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("사용자 식별 헤더가 비어 있을 수 없습니다."));
    }

    private NotificationResponse sentResponse() {
        return new NotificationResponse(
                42L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                NotificationStatus.SENT,
                LocalDateTime.of(2026, 5, 10, 12, 0),
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 9, 10, 0)
        );
    }

    private NotificationResponse failedResponse() {
        return new NotificationResponse(
                42L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                NotificationStatus.FAILED,
                null,
                LocalDateTime.of(2026, 5, 10, 12, 0),
                "smtp connection refused",
                null,
                LocalDateTime.of(2026, 5, 9, 10, 0)
        );
    }

    private NotificationResponse pendingResponse() {
        return new NotificationResponse(
                42L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                NotificationStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 9, 10, 0)
        );
    }
}
