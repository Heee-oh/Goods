package com.goods.market.common.auth.presentation;

import com.goods.market.common.auth.application.AuthService;
import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void signupReturnsCreatedToken() throws Exception {
        when(authService.signup(any(AuthSignupCommand.class)))
                .thenReturn(new AuthTokenDto(1L, "token-1", 3600L));

                mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "01012345678",
                                  "nickname": "alice",
                                  "regionId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.accessToken").value("token-1"));

        ArgumentCaptor<AuthSignupCommand> captor = ArgumentCaptor.forClass(AuthSignupCommand.class);
        verify(authService).signup(captor.capture());
        assertThat(captor.getValue().phoneNumber()).isEqualTo("01012345678");
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.login(any(AuthLoginCommand.class)))
                .thenReturn(new AuthTokenDto(1L, "token-1", 3600L));

                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "01012345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token-1"));
    }
}
