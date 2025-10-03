package com.example.bankcards.controller;

import com.example.bankcards.dto.UserAuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;

import com.example.bankcards.service.JwtService;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.util.UserMapper;
import jakarta.annotation.Resource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Комплексные тесты для AuthController")
class AuthControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private RefreshTokenService refreshTokenService;
    @MockBean
    private RefreshTokenRepository refreshTokenRepository;


    @Nested
    @DisplayName("Тесты регистрации")
    class RegistrationTests {

        @Test
        void register_Valid_ShouldReturn201() throws Exception {
            String json = """
            {"username":"testusername","password":"testpassword"}
            """;

            User user = new User();
            user.setUsername("testusername");
            user.setRole(Role.USER);

            when(userRepository.findByUsername("testusername")).thenReturn(Optional.empty());
            when(userMapper.map(any(UserAuthDTO.class))).thenReturn(user);
            when(passwordEncoder.encode("testpassword")).thenReturn("encodedPass");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.map(any(User.class))).thenReturn(new UserDTO("testusername",Role.USER));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.username")
                                    .value(user.getUsername()),
                            jsonPath("$.role")
                                    .value("USER")
                    )
                    .andDo(print())
                    .andReturn();
        }

        @Test
        void register_DuplicateUsername_ShouldReturn400() throws Exception {
            String json = """
            {"username":"existingUser","password":"testpassword"}
            """;

            when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.error")
                                    .value("Этот юзернейм уже занят")
                    )
                    .andDo(print())
                    .andReturn();
        }

        static Stream<Arguments> invalidRegister() {
            return Stream.of(
                    Arguments.of("", "testpassword", "Логин не может быть пустым"),
                    Arguments.of("testusername", "", "Пароль не может быть пустым")
            );
        }

        @ParameterizedTest(name = "[{index}] {2}")
        @MethodSource("invalidRegister")
        void register_InvalidData_ShouldReturn400(String username, String password, String error) throws Exception {
            String json = String.format("""
                    {"username":"%s","password":"%s"}
                    """, username, password);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.errors", Matchers.contains(error))
                    )
                    .andDo(print())
                    .andReturn();
        }
    }


    @Nested
    @DisplayName("Тесты логина")
    class LoginTests {

        @Test
        void login_Valid_ShouldReturn200WithTokens() throws Exception {
            String json = """
            {"username":"testusername","password":"testpassword"}
            """;

            User user = new User();
            user.setUsername("testusername");

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userRepository.findByUsername("testusername")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("access123");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.jwtAccess")
                                    .value("access123"),
                            jsonPath("$.jwtRefresh")
                                    .value("refresh123")
                    )
                    .andDo(print())
                    .andReturn();
        }


        @Test
        void login_Invalid_ShouldReturn401() throws Exception {
            String json = """
            {"username":"wrongusername","password":"wrongpassword"}
            """;

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException(""));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.error")
                                    .value("Неверный юзернейм или пароль")
                    )
                    .andDo(print())
                    .andReturn();
        }
    }


    @Nested
    @DisplayName("Тесты jwtAccessToken & jwtRefreshToken")
    class JWTTests {

        @Test
        void refresh_Valid_ShouldReturn200WithAccessToken() throws Exception {
            String json = """
            {"refreshToken":"goodToken"}
            """;

            User user = new User();
            user.setUsername("testusername");
            user.setRole(Role.USER);

            when(refreshTokenService.isValid("goodToken")).thenReturn(true);
            when(refreshTokenService.getUsernameByToken("goodToken")).thenReturn("testusername");
            when(userRepository.findByUsername("testusername")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("newAccess123");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.jwtAccess")
                                    .value("newAccess123")
                    )
                    .andDo(print())
                    .andReturn();
        }

        @Test
        void refresh_Invalid_ShouldReturn404() throws Exception {
            String json = """
            {"refreshToken":"badToken"}
            """;

            when(refreshTokenService.isValid("badToken")).thenReturn(false);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.error")
                                    .value("Невалидный refreshToken")
                    )
                    .andDo(print())
                    .andReturn();
        }

        @Test
        void logout_Valid_ShouldReturn200() throws Exception {
            String json = """
            {"refreshToken":"refresh123"}
            """;

            when(refreshTokenRepository.findByToken("refresh123"))
                    .thenReturn(Optional.of(new RefreshToken()));

            mockMvc.perform(delete("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.message")
                                    .value("Logged out")
                    )
                    .andDo(print())
                    .andReturn();
        }

        @Test
        void logout_Invalid_ShouldReturn400() throws Exception {
            String json = """
            {"refreshToken":"badToken"}
            """;

            when(refreshTokenRepository.findByToken("badToken"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(delete("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.error")
                                    .value("Refresh токен не валидный")
                    )
                    .andDo(print())
                    .andReturn();
        }
    }
}

