package com.library.member;

import jakarta.validation.constraints.NotNull;

public record ChangeTypeRequest(@NotNull(message = "Choose a member type") Long memberTypeId) {
}
