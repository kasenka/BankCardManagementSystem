package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Комплексные тесты для UserController")
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Nested
    @DisplayName("Тесты метода getMe")
    class GetMeTests {

        @Test
        @WithMockUser(username = "testUser", roles = "USER")
        void getMe_Valid_ShouldReturn200() throws Exception {
            UserDTO userDTO = new UserDTO("testUser", Role.USER);

            when(userService.getMe("testUser")).thenReturn(userDTO);

            mockMvc.perform(get("/api/users/me"))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.me.username").value("testUser"),
                            jsonPath("$.me.role").value("USER")
                    )
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "unknown", roles = "USER")
        void getMe_NotFound_ShouldReturn404() throws Exception {
            when(userService.getMe("unknown"))
                    .thenThrow(new NoSuchElementException("Пользователь не найден"));

            mockMvc.perform(get("/api/users/me"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.error").value("Пользователь не найден")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("Тесты метода getAllUsers")
    class GetAllUsersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllUsers_Admin_ShouldReturn200() throws Exception {
            Page<UserDTO> page = new PageImpl<>(
                    List.of(new UserDTO("user1", Role.USER), new UserDTO("user2", Role.USER))
            );

            when(userService.getAllUsers(any())).thenReturn(page);

            mockMvc.perform(get("/api/users"))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.users.content[0].username").value("user1"),
                            jsonPath("$.users.content[1].username").value("user2")
                    )
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "USER")
        void getAllUsers_User_ShouldReturn403() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("Тесты метода getUser")
    class GetUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUser_Valid_ShouldReturn200() throws Exception {
            UserDTO userDTO = new UserDTO("adminUser", Role.ADMIN);

            when(userService.getUser(1L)).thenReturn(userDTO);

            mockMvc.perform(get("/api/users/1"))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.username").value("adminUser"),
                            jsonPath("$.role").value("ADMIN")
                    )
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUser_NotFound_ShouldReturn404() throws Exception {
            when(userService.getUser(99L))
                    .thenThrow(new NoSuchElementException("Пользователь не найден"));

            mockMvc.perform(get("/api/users/99"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.error").value("Пользователь не найден")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteUser")
    class DeleteUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteUser_Valid_ShouldReturn204() throws Exception {
            when(userService.deleteUser(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/users/1/delete"))
                    .andExpectAll(
                            status().isNoContent(),
                            jsonPath("$.message").value("Этот пользователь удален")
                    )
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteUser_Conflict_ShouldReturn409() throws Exception {
            when(userService.deleteUser(1L)).thenReturn(false);

            mockMvc.perform(delete("/api/users/1/delete"))
                    .andExpectAll(
                            status().isConflict(),
                            jsonPath("$.error").value("Не удалось удалить пользователя")
                    )
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteUser_NotFound_ShouldReturn404() throws Exception {
            when(userService.deleteUser(42L))
                    .thenThrow(new NoSuchElementException("Пользователь с id 42 не найден"));

            mockMvc.perform(delete("/api/users/42/delete"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.error").value("Пользователь с id 42 не найден")
                    )
                    .andDo(print());
        }
    }
}

