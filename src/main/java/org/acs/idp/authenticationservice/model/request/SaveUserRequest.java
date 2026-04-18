package org.acs.idp.authenticationservice.model.request;

public record SaveUserRequest(String email,
                              String passwordHash) {}

