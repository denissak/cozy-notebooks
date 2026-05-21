package com.cozy.notebooks.security;

import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.domain.UserEntity;
import com.cozy.notebooks.repository.NotebookRepository;
import com.cozy.notebooks.repository.UserRepository;
import com.cozy.notebooks.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the current user (provided by {@link CurrentUserProvider})
 * cannot see other users' notebooks. The dev mock-user is fixed by
 * application-test.yml; we create another user in the DB and confirm
 * its notebook is invisible to the API.
 */
class TenantIsolationIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotebookRepository notebookRepository;

    @Test
    void otherUsersNotebookIsInvisible() throws Exception {
        UserEntity other = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("other-%s@cozy.local".formatted(UUID.randomUUID()))
                .displayName("Other")
                .build();
        userRepository.save(other);

        NotebookEntity foreign = NotebookEntity.builder()
                .id(UUID.randomUUID())
                .userId(other.getId())
                .hrefCode("zzzzzzzzzzzzzzzz")
                .title("Top secret")
                .position(0)
                .build();
        notebookRepository.save(foreign);

        mockMvc.perform(get("/api/v1/notebooks/{id}", foreign.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(get("/api/v1/notebooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + foreign.getId() + "')]").isEmpty());
    }
}
