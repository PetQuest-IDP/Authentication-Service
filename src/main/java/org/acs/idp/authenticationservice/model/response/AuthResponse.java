package org.acs.idp.authenticationservice.model.response;

public record AuthResponse(String accessToken,
                           String refreshToken) {}
