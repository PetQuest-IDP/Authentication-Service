package org.acs.idp.authenticationservice.model.dto;

public record RefreshTokenDto(String token,
                              String email,
                              long expiresAt) {}

