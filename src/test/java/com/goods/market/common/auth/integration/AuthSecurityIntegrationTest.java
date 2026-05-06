package com.goods.market.common.auth.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("토큰 없이 보호 API 호출 시 401을 반환한다")
    void protectedApiRequiresToken() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원가입 후 발급된 JWT로 보호 API에 접근할 수 있다")
    void signupAndAccessWithToken() throws Exception {
        String phoneNumber = randomPhoneNumber();

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {
                                  "phone_number": "%s",
                                  "nickname": "auth_user"
                                }
                                """.formatted(phoneNumber)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = JsonPath.read(signupResult.getResponse().getContentAsString(), "$.data.access_token");
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전화번호 로그인으로 JWT를 다시 발급받을 수 있다")
    void loginReturnsToken() throws Exception {
        String phoneNumber = randomPhoneNumber();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {
                                  "phone_number": "%s",
                                  "nickname": "auth_user"
                                }
                                """.formatted(phoneNumber)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "phone_number": "%s"
                                }
                                """.formatted(phoneNumber)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.access_token");
        assertThat(accessToken).isNotBlank();
    }

    private String randomPhoneNumber() {
        int suffix = ThreadLocalRandom.current().nextInt(10_0000_0000);
        return "010" + String.format("%08d", suffix % 100_000_000);
    }
}

