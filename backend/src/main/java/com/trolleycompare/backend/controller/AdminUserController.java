package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.CreateUserRequest;
import com.trolleycompare.backend.dto.UpdateUserRequest;
import com.trolleycompare.backend.dto.UserResponse;
import com.trolleycompare.backend.security.UserPrincipal;
import com.trolleycompare.backend.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.register(request));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return UserResponse.from(userService.update(id, requester.id(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
