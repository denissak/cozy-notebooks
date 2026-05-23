package com.cozy.notebooks.api;

import com.cozy.notebooks.domain.AssetEntity;
import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.domain.PageEntity;
import com.cozy.notebooks.domain.UserEntity;
import com.cozy.notebooks.domain.UserPlan;
import com.cozy.notebooks.repository.AssetRepository;
import com.cozy.notebooks.repository.NotebookRepository;
import com.cozy.notebooks.repository.PageRepository;
import com.cozy.notebooks.repository.UserRepository;
import com.cozy.notebooks.service.PageContentHashService;
import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPlanQuotaIT {

    @Nested
    class RealAuth extends AbstractRealAuthIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @Autowired
        UserRepository userRepository;

        @Autowired
        NotebookRepository notebookRepository;

        @Autowired
        PageRepository pageRepository;

        @Autowired
        AssetRepository assetRepository;

        @Autowired
        PageContentHashService pageContentHashService;

        @Test
        void register_emailUser_hasPlanCodeFree() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UserEntity user = userRepository.findByIdAndDeletedAtIsNull(tokens.userId()).orElseThrow();
            assertThat(user.getPlanCode()).isEqualTo(UserPlan.FREE.code());
        }

        @Test
        void limits_freeUser_returnsFreeLimits() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");

            mockMvc.perform(get("/api/v1/account/limits")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plan").value("free"))
                    .andExpect(jsonPath("$.limits.maxStorageMb").value(25))
                    .andExpect(jsonPath("$.limits.maxStorageBytes").value(26_214_400))
                    .andExpect(jsonPath("$.limits.maxNotebooks").value(3))
                    .andExpect(jsonPath("$.limits.maxPagesPerNotebook").value(25))
                    .andExpect(jsonPath("$.limits.maxPagesTotal").value(75))
                    .andExpect(jsonPath("$.limits.syncEnabled").value(true));
        }

        @Test
        void limits_proUser_returnsProLimits() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            setPlan(tokens.userId(), UserPlan.PRO.code());

            mockMvc.perform(get("/api/v1/account/limits")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plan").value("pro"))
                    .andExpect(jsonPath("$.limits.maxStorageMb").value(300))
                    .andExpect(jsonPath("$.limits.maxStorageBytes").value(314_572_800))
                    .andExpect(jsonPath("$.limits.maxNotebooks").value(20))
                    .andExpect(jsonPath("$.limits.maxPagesPerNotebook").value(100))
                    .andExpect(jsonPath("$.limits.maxPagesTotal").value(2000))
                    .andExpect(jsonPath("$.limits.syncEnabled").value(true));
        }

        @Test
        void limits_withoutJwt_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/limits"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void limits_withValidJwt_returns200() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            mockMvc.perform(get("/api/v1/account/limits")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plan").value("free"));
        }

        @Test
        void freeUser_canCreateUpToMaxNotebooks() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
                createNotebook(tokens, "NB " + i);
            }
        }

        @Test
        void freeUser_fourthNotebook_returns403QuotaExceeded() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
                createNotebook(tokens, "NB " + i);
            }

            mockMvc.perform(post("/api/v1/notebooks")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "Over limit"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("quota_exceeded"))
                    .andExpect(jsonPath("$.message").value(
                            "Notebook limit reached for plan free (max 3)"));
        }

        @Test
        void proUser_canCreateMoreThanThreeNotebooks() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            setPlan(tokens.userId(), UserPlan.PRO.code());
            for (int i = 0; i < 4; i++) {
                createNotebook(tokens, "Pro NB " + i);
            }
        }

        @Test
        void proUser_exceedingMaxNotebooks_returns403QuotaExceeded() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            setPlan(tokens.userId(), UserPlan.PRO.code());
            for (int i = 0; i < UserPlan.PRO.maxNotebooks(); i++) {
                createNotebook(tokens, "Pro NB " + i);
            }

            mockMvc.perform(post("/api/v1/notebooks")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "Pro over"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("quota_exceeded"));
        }

        @Test
        void freeUser_canCreateUpToMaxPagesPerNotebook() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID notebookId = createNotebook(tokens, "Pages NB");
            for (int i = 0; i < UserPlan.FREE.maxPagesPerNotebook(); i++) {
                createPage(tokens, notebookId, "P" + i);
            }
        }

        @Test
        void freeUser_26thPageInNotebook_returns403QuotaExceeded() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID notebookId = createNotebook(tokens, "Pages NB");
            for (int i = 0; i < UserPlan.FREE.maxPagesPerNotebook(); i++) {
                createPage(tokens, notebookId, "P" + i);
            }

            mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "Over",
                                    "content", Map.of("blocks", java.util.List.of())))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("quota_exceeded"))
                    .andExpect(jsonPath("$.message").value(
                            "Page limit per notebook reached for plan free (max 25)"));
        }

        @Test
        void freeUser_cannotExceedMaxPagesTotal() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID notebookId = fillFreePlanPageQuotaAcrossNotebooks(tokens);

            mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "Over total",
                                    "content", Map.of("blocks", java.util.List.of())))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("quota_exceeded"))
                    .andExpect(jsonPath("$.message").value(
                            "Total page limit reached for plan free (max 75)"));
        }

        @Test
        void proUser_hasHigherPageLimits() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            setPlan(tokens.userId(), UserPlan.PRO.code());
            UUID notebookId = createNotebook(tokens, "Pro pages");
            for (int i = 0; i < 30; i++) {
                createPage(tokens, notebookId, "P" + i);
            }
        }

        @Test
        void softDeletedNotebook_doesNotCountTowardQuota() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID toDelete = null;
            for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
                UUID id = createNotebook(tokens, "NB " + i);
                if (i == 0) {
                    toDelete = id;
                }
            }

            mockMvc.perform(delete("/api/v1/notebooks/{id}", toDelete)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isNoContent());

            createNotebook(tokens, "After delete");
        }

        @Test
        void softDeletedPage_doesNotCountTowardQuota() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID notebookId = createNotebook(tokens, "Soft page");
            UUID pageId = null;
            for (int i = 0; i < UserPlan.FREE.maxPagesPerNotebook(); i++) {
                UUID id = createPage(tokens, notebookId, "P" + i);
                if (i == 0) {
                    pageId = id;
                }
            }

            mockMvc.perform(delete("/api/v1/pages/{id}", pageId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isNoContent());

            createPage(tokens, notebookId, "Replacement");
        }

        @Test
        void softDeletedPage_doesNotCountTowardTotalPageQuota() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UUID notebookId = fillFreePlanPageQuotaAcrossNotebooks(tokens);
            UUID pageToDelete = firstPageIdInNotebook(tokens, notebookId);

            mockMvc.perform(delete("/api/v1/pages/{id}", pageToDelete)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isNoContent());

            createPage(tokens, notebookId, "Replacement total");
        }

        private UUID firstPageIdInNotebook(Tokens tokens, UUID notebookId) throws Exception {
            MvcResult list = mockMvc.perform(get("/api/v1/notebooks/{nid}/pages", notebookId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode pages = objectMapper.readTree(list.getResponse().getContentAsString());
            assertThat(pages.isArray()).isTrue();
            assertThat(pages.size()).isGreaterThan(0);
            return UUID.fromString(pages.get(0).get("id").asText());
        }

        /**
         * Fills free plan page quota (3 notebooks × 25 pages). Returns the last notebook id
         * (already at maxPagesPerNotebook) for overflow assertions.
         */
        private UUID fillFreePlanPageQuotaAcrossNotebooks(Tokens tokens) throws Exception {
            UUID lastNotebookId = null;
            for (int nb = 0; nb < UserPlan.FREE.maxNotebooks(); nb++) {
                lastNotebookId = createNotebook(tokens, "Fill " + nb);
                for (int p = 0; p < UserPlan.FREE.maxPagesPerNotebook(); p++) {
                    createPage(tokens, lastNotebookId, "P" + nb + "-" + p);
                }
            }
            return lastNotebookId;
        }

        @Test
        void softDeletedAsset_doesNotCountTowardStorageUsage() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            long activeBytes = 1_048_576L;
            long deletedBytes = 5_242_880L;

            assetRepository.save(activeAsset(tokens.userId(), activeBytes, null));
            assetRepository.save(activeAsset(tokens.userId(), deletedBytes, OffsetDateTime.now()));

            mockMvc.perform(get("/api/v1/account/limits")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usage.storageBytesUsed").value(activeBytes))
                    .andExpect(jsonPath("$.usage.storageMbUsed").value(1));
        }

        @Test
        void otherUsersNotebooks_doNotCountTowardNotebookQuota() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UserEntity other = persistOtherUser();
            for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
                notebookRepository.save(foreignNotebook(other.getId(), "Foreign " + i));
            }

            for (int i = 0; i < UserPlan.FREE.maxNotebooks(); i++) {
                createNotebook(tokens, "Mine " + i);
            }
        }

        @Test
        void otherUsersPages_doNotCountTowardPageQuota() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UserEntity other = persistOtherUser();
            NotebookEntity foreignNb = notebookRepository.save(foreignNotebook(other.getId(), "Foreign nb"));
            String hash = pageContentHashService.hash(
                    objectMapper.valueToTree(Map.of("blocks", java.util.List.of())));
            for (int i = 0; i < UserPlan.FREE.maxPagesTotal(); i++) {
                pageRepository.save(PageEntity.builder()
                        .id(UUID.randomUUID())
                        .userId(other.getId())
                        .notebookId(foreignNb.getId())
                        .title("Foreign page " + i)
                        .contentJson(objectMapper.valueToTree(Map.of("blocks", java.util.List.of())))
                        .contentHash(hash)
                        .version(1L)
                        .build());
            }

            UUID notebookId = createNotebook(tokens, "Mine");
            createPage(tokens, notebookId, "My page");
        }

        @Test
        void otherUsersAssets_doNotCountTowardStorageUsage() throws Exception {
            Tokens tokens = register(randomEmail(), "Password123!");
            UserEntity other = persistOtherUser();
            assetRepository.save(activeAsset(other.getId(), 50_000_000L, null));

            mockMvc.perform(get("/api/v1/account/limits")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usage.storageBytesUsed").value(0))
                    .andExpect(jsonPath("$.usage.storageMbUsed").value(0));
        }

        private UserEntity persistOtherUser() {
            return userRepository.save(UserEntity.builder()
                    .id(UUID.randomUUID())
                    .email("other-quota-" + UUID.randomUUID() + "@example.com")
                    .planCode(UserPlan.FREE.code())
                    .build());
        }

        private static NotebookEntity foreignNotebook(UUID userId, String title) {
            return NotebookEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .hrefCode("q" + UUID.randomUUID().toString().replace("-", "").substring(0, 17))
                    .title(title)
                    .position(0)
                    .build();
        }

        private static AssetEntity activeAsset(UUID userId, long byteSize, OffsetDateTime deletedAt) {
            AssetEntity asset = AssetEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .fileName("test.bin")
                    .mimeType("application/octet-stream")
                    .byteSize(byteSize)
                    .storageUrl("https://example.test/objects/" + UUID.randomUUID())
                    .build();
            asset.setDeletedAt(deletedAt);
            return asset;
        }

        private void setPlan(UUID userId, String planCode) {
            UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow();
            user.setPlanCode(planCode);
            userRepository.saveAndFlush(user);
        }

        private UUID createNotebook(Tokens tokens, String title) throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/notebooks")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", title))))
                    .andExpect(status().isCreated())
                    .andReturn();
            return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asText());
        }

        private UUID createPage(Tokens tokens, UUID notebookId, String title) throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", title,
                                    "content", Map.of("blocks", java.util.List.of())))))
                    .andExpect(status().isCreated())
                    .andReturn();
            return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asText());
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
                    UUID.fromString(json.get("user").get("id").asText())
            );
        }

        private static String randomEmail() {
            return "quota-" + UUID.randomUUID() + "@example.com";
        }

        private record Tokens(String accessToken, UUID userId) {
        }
    }

    @Nested
    class MockMode extends AbstractIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Test
        void limits_withoutJwt_worksWithMockUser() throws Exception {
            mockMvc.perform(get("/api/v1/account/limits"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plan").value("free"))
                    .andExpect(jsonPath("$.limits.maxNotebooks").value(3));
        }
    }

    @Nested
    class MockModeDisabled extends AbstractRealAuthIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Test
        void limits_withoutJwt_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/account/limits"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
