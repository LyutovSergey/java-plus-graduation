package ru.practicum.common.dto;

public record UserDto(
		Long id,
		String name,
		String email
) {
}