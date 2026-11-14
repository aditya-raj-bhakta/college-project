package com.library.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendRequest(@NotBlank(message = "Give a reason") @Size(max = 255) String reason) {
}
