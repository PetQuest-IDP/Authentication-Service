package org.acs.idp.authenticationservice.service;

import org.acs.idp.authenticationservice.model.dto.RefreshTokenDto;
import org.acs.idp.authenticationservice.model.dto.UserDto;
import org.acs.idp.authenticationservice.model.exception.TokenNotFoundException;
import org.acs.idp.authenticationservice.model.request.SaveRefreshTokenRequest;
import org.acs.idp.authenticationservice.model.request.SaveUserRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DbClientService {

    private final RestClient restClient;

    public DbClientService(@Value("${db-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    //  User calls
    public UserDto findUserByEmail(String email) {
        try {
            return restClient.get()
                    .uri("/users?email={email}", email)
                    .retrieve()
                    .body(UserDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveUser(SaveUserRequest request) {
        restClient.post()
                .uri("/users")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    //  Refresh token calls
    public void saveRefreshToken(String email, String token, long expiresAt) {
        restClient.post()
                .uri("/refresh-tokens")
                .body(new SaveRefreshTokenRequest(email, token, expiresAt))
                .retrieve()
                .toBodilessEntity();
    }

    public RefreshTokenDto findRefreshToken(String token) {
        try {
            return restClient.get()
                    .uri("/refresh-tokens?token={token}", token)
                    .retrieve()
                    .body(RefreshTokenDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteRefreshToken(String token) {
        restClient.delete()
                .uri("/refresh-tokens?token={token}", token)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    //  DB says 404
                    throw new TokenNotFoundException();
                })
                .toBodilessEntity();
    }

    public void deleteAllRefreshTokensByEmail(String email) {
        restClient.delete()
                .uri("/refresh-tokens/all?email={email}", email)
                .retrieve()
                .toBodilessEntity();
    }
}