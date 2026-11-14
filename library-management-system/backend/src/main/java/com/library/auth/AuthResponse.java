package com.library.auth;

import com.library.member.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
