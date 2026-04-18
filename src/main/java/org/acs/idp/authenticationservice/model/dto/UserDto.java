package org.acs.idp.authenticationservice.model.dto;

public record UserDto(String id,
                      String email,
                      String passwordHash) {}
