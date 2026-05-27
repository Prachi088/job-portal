package com.jobportal.job_portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.job_portal.entity.User;
import com.jobportal.job_portal.repository.UserRepository;
import com.jobportal.job_portal.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=01234567890123456789012345678901",
        "jwt.expiration=86400000"
})
class ConnectionMessagingFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        alice = new User();
        alice.setName("Alice");
        alice.setEmail("alice@example.com");
        alice.setPassword("password");
        alice.setRole("USER");
        alice = userRepository.save(alice);

        bob = new User();
        bob.setName("Bob");
        bob.setEmail("bob@example.com");
        bob.setPassword("password");
        bob.setRole("USER");
        bob = userRepository.save(bob);

        aliceToken = "Bearer " + jwtUtil.generateToken(alice.getEmail(), alice.getRole(), alice.getId());
        bobToken = "Bearer " + jwtUtil.generateToken(bob.getEmail(), bob.getRole(), bob.getId());
    }

    @Test
    void connectionRequestAcceptanceAndMessagingFlow() throws Exception {
        var sendRequestBody = Map.of(
                "senderId", alice.getId(),
                "receiverId", bob.getId()
        );

        var sendResult = mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequestBody)))
                .andExpect(status().isOk())
                .andReturn();

        var sentRequest = objectMapper.readValue(sendResult.getResponse().getContentAsString(), Map.class);
        assertThat(sentRequest).containsEntry("senderId", alice.getId());
        assertThat(sentRequest).containsEntry("receiverId", bob.getId());
        assertThat(sentRequest).containsEntry("status", "PENDING");

        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequestBody)))
                .andExpect(status().isBadRequest());

        var incoming = mockMvc.perform(get("/api/connections/requests/" + bob.getId())
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andReturn();

        var incomingList = objectMapper.readValue(incoming.getResponse().getContentAsString(), java.util.List.class);
        assertThat(incomingList).hasSize(1);

        Number requestId = (Number) ((Map<?, ?>) incomingList.get(0)).get("id");
        assertThat(requestId).isNotNull();

        var acceptResult = mockMvc.perform(put("/api/connections/request/" + requestId.longValue())
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isOk())
                .andReturn();

        var acceptanceResponse = objectMapper.readValue(acceptResult.getResponse().getContentAsString(), Map.class);
        assertThat(acceptanceResponse).containsKey("request");
        assertThat(acceptanceResponse).containsKey("connection");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senderId", alice.getId(),
                                "receiverId", bob.getId(),
                                "content", "Hello Bob"
                        ))))
                .andExpect(status().isOk());

        var conversation = mockMvc.perform(get("/api/messages/conversation/" + alice.getId() + "/" + bob.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andReturn();

        var conversationList = objectMapper.readValue(conversation.getResponse().getContentAsString(), java.util.List.class);
        assertThat(conversationList).hasSize(1);
    }
}
