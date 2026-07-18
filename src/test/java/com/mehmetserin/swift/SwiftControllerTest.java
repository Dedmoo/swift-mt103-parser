package com.mehmetserin.swift;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SwiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void parse_returnsStructuredFields() throws Exception {
        String message = """
                :20:CTRLREF01
                :23B:CRED
                :32A:260720TRY5000,00
                :59:BENEFICIARY LTD
                :70:SALARY
                :71A:OUR
                """;
        String body = objectMapper.writeValueAsString(Map.of("message", message));

        mockMvc.perform(post("/api/swift/mt103/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderReference").value("CTRLREF01"))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    void parse_invalidMessage_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", ":20:ONLYREF"));

        mockMvc.perform(post("/api/swift/mt103/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
