package org.acs.idp.authenticationservice.model.request;

public record SaveRefreshTokenRequest(String email,
                                      String token,
                                      long expiresAt) {}
