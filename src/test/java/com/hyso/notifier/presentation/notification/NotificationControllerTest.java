package com.hyso.notifier.presentation.notification;

import com.hyso.notifier.application.notification.NotificationService;
import com.hyso.notifier.application.notification.RegisterNotificationResult;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
}
