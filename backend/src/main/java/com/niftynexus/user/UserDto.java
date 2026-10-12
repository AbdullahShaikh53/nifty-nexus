package com.niftynexus.user;

public record UserDto(Long id, String name, String email, String role, boolean blocked, boolean fnoUnlocked, int riskScore, String createdAt) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isBlocked(), u.isFnoUnlocked(), u.getRiskScore(), u.getCreatedAt().toString());
    }
}
