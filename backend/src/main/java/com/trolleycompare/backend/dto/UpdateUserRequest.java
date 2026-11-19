package com.trolleycompare.backend.dto;

public record UpdateUserRequest(String name, String email, String password, Boolean enabled, String role) {}
