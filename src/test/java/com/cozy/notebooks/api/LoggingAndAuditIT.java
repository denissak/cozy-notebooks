package com.cozy.notebooks.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cozy.notebooks.domain.UserActivityLogEntity;
import com.cozy.notebooks.domain.UserPlan;
import com.cozy.notebooks.logging.RequestIdFilter;
import com.cozy.notebooks.repository.UserActivityLogRepository;
import com.cozy.notebooks.service.UserActivityActions;
import com.cozy.notebooks.service.UserActivityLogService;
import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoggingAndAuditIT {

  @org.junit.jupiter.api.Nested
  class RequestId extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void everyResponseIncludesRequestIdHeader() throws Exception {
      mockMvc.perform(get("/api/v1/health"))
          .andExpect(status().isOk())
          .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
          .andExpect(header().string(RequestIdFilter.HEADER_NAME,
              org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())));
    }

    @Test
    void echoesIncomingRequestId() throws Exception {
      String requestId = "test-req-" + UUID.randomUUID();
      mockMvc.perform(get("/api/v1/health").header(RequestIdFilter.HEADER_NAME, requestId))
          .andExpect(status().isOk())
          .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId));
    }
  }

  @org.junit.jupiter.api.Nested
  class ActivityAudit extends AbstractRealAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserActivityLogRepository activityLogRepository;

    @Test
    void register_createsAuthRegisterSuccessActivity() throws Exception {
      String email = randomEmail();
      String requestId = "reg-" + UUID.randomUUID();
      MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of(
                  "email", email,
                  "password", "Password123!"
              ))))
          .andExpect(status().isOk())
          .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
          .andReturn();

      UUID userId = UUID.fromString(
          objectMapper.readTree(result.getResponse().getContentAsString())
              .path("user").path("id").asText());

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionAndUserIdOrderByCreatedAtDesc(
          UserActivityActions.AUTH_REGISTER, userId);
      assertThat(rows).isNotEmpty();
      UserActivityLogEntity row = rows.getFirst();
      assertThat(row.getStatus()).isEqualTo(UserActivityLogService.STATUS_SUCCESS);
      assertThat(row.getRequestId()).isEqualTo(requestId);
    }

    @Test
    void loginSuccess_createsAuthLoginSuccessActivity() throws Exception {
      String email = randomEmail();
      String password = "Password123!";
      register(email, password);

      String requestId = "login-ok-" + UUID.randomUUID();
      mockMvc.perform(post("/api/v1/auth/login")
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of(
                  "email", email,
                  "password", password
              ))))
          .andExpect(status().isOk())
          .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId));

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionOrderByCreatedAtDesc(
          UserActivityActions.AUTH_LOGIN);
      assertThat(rows.stream().filter(r -> UserActivityLogService.STATUS_SUCCESS.equals(r.getStatus())))
          .anyMatch(r -> requestId.equals(r.getRequestId()));
    }

    @Test
    void loginFailure_createsAuthLoginFailureWithoutLoggingPassword() throws Exception {
      String email = randomEmail();
      String password = "Password123!";
      register(email, password);
      String wrongPassword = "WrongSecret999!";

      Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      appender.start();
      root.addAppender(appender);
      try {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", wrongPassword
                ))))
            .andExpect(status().isUnauthorized());

        List<UserActivityLogEntity> failures = activityLogRepository.findByActionOrderByCreatedAtDesc(
            UserActivityActions.AUTH_LOGIN);
        assertThat(failures.stream()
            .filter(r -> UserActivityLogService.STATUS_FAILURE.equals(r.getStatus())))
            .isNotEmpty();

        for (ILoggingEvent event : appender.list) {
          assertThat(event.getFormattedMessage()).doesNotContain(wrongPassword);
          assertThat(event.getFormattedMessage()).doesNotContain(password);
        }
      } finally {
        root.detachAppender(appender);
      }
    }

    @Test
    void notebookCreate_createsNotebookCreateSuccessActivity() throws Exception {
      Tokens tokens = register(randomEmail(), "Password123!");
      String requestId = "nb-" + UUID.randomUUID();

      MvcResult nb = mockMvc.perform(post("/api/v1/notebooks")
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of("title", "Audit NB"))))
          .andExpect(status().isCreated())
          .andReturn();

      UUID notebookId = UUID.fromString(
          objectMapper.readTree(nb.getResponse().getContentAsString()).get("id").asText());

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionAndUserIdOrderByCreatedAtDesc(
          UserActivityActions.NOTEBOOK_CREATE, tokens.userId());
      assertThat(rows).anyMatch(r ->
          UserActivityLogService.STATUS_SUCCESS.equals(r.getStatus())
              && notebookId.equals(r.getEntityId())
              && requestId.equals(r.getRequestId()));
    }

    @Test
    void pageCreate_createsPageCreateSuccessActivity() throws Exception {
      Tokens tokens = register(randomEmail(), "Password123!");
      UUID notebookId = createNotebook(tokens, "Pages");
      String requestId = "page-" + UUID.randomUUID();

      MvcResult page = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of(
                  "title", "Audit page",
                  "content", Map.of("blocks", List.of())
              ))))
          .andExpect(status().isCreated())
          .andReturn();

      UUID pageId = UUID.fromString(
          objectMapper.readTree(page.getResponse().getContentAsString()).get("id").asText());

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionAndUserIdOrderByCreatedAtDesc(
          UserActivityActions.PAGE_CREATE, tokens.userId());
      assertThat(rows).anyMatch(r ->
          pageId.equals(r.getEntityId()) && requestId.equals(r.getRequestId()));
    }

    @Test
    void feedbackCreate_createsFeedbackCreateSuccessActivity() throws Exception {
      Tokens tokens = register(randomEmail(), "Password123!");
      String requestId = "fb-" + UUID.randomUUID();

      mockMvc.perform(post("/api/v1/feedback")
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of(
                  "type", "idea",
                  "message", "More themes"
              ))))
          .andExpect(status().isCreated());

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionAndUserIdOrderByCreatedAtDesc(
          UserActivityActions.FEEDBACK_CREATE, tokens.userId());
      assertThat(rows).anyMatch(r -> requestId.equals(r.getRequestId()));
    }

    @Test
    void quotaExceeded_returns403WithRequestIdAndLogsFailure() throws Exception {
      Tokens tokens = register(randomEmail(), "Password123!");
      for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
        createNotebook(tokens, "NB " + i);
      }
      String requestId = "quota-" + UUID.randomUUID();

      mockMvc.perform(post("/api/v1/notebooks")
              .header(RequestIdFilter.HEADER_NAME, requestId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of("title", "Over"))))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("quota_exceeded"))
          .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId));

      List<UserActivityLogEntity> rows = activityLogRepository.findByActionAndUserIdOrderByCreatedAtDesc(
          UserActivityActions.QUOTA_EXCEEDED, tokens.userId());
      assertThat(rows).anyMatch(r ->
          UserActivityLogService.STATUS_FAILURE.equals(r.getStatus())
              && requestId.equals(r.getRequestId()));
    }

    private Tokens register(String email, String password) throws Exception {
      MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of(
                  "email", email,
                  "password", password
              ))))
          .andExpect(status().isOk())
          .andReturn();
      JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
      return new Tokens(
          json.get("accessToken").asText(),
          json.get("refreshToken").asText(),
          UUID.fromString(json.get("user").get("id").asText()),
          json.get("user").get("email").asText()
      );
    }

    private UUID createNotebook(Tokens tokens, String title) throws Exception {
      MvcResult result = mockMvc.perform(post("/api/v1/notebooks")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(Map.of("title", title))))
          .andExpect(status().isCreated())
          .andReturn();
      return UUID.fromString(
          objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private static String randomEmail() {
      return "audit-" + UUID.randomUUID() + "@example.com";
    }

    private record Tokens(String accessToken, String refreshToken, UUID userId, String userEmail) {
    }
  }
}
