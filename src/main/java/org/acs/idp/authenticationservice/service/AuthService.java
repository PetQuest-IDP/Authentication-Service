package org.acs.idp.authenticationservice.service;

import org.acs.idp.authenticationservice.model.dto.RefreshTokenDto;
import org.acs.idp.authenticationservice.model.dto.UserDto;
import org.acs.idp.authenticationservice.model.request.AuthRequest;
import org.acs.idp.authenticationservice.model.request.SaveUserRequest;
import org.acs.idp.authenticationservice.model.response.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final DbClientService dbClient;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    public AuthService(DbClientService dbClient, JwtService jwtService) {
        this.dbClient = dbClient;
        this.jwtService = jwtService;
    }

    //  Register
    public void register(AuthRequest request) {
        //  Check if email already exists
        UserDto existing = dbClient.findUserByEmail(request.email());
        if (existing != null) {
            throw new RuntimeException("Email already in use");
        }

        //  Hash the password and save the user
        String hashed = BCrypt.withDefaults()
                .hashToString(12, request.password().toCharArray());
        dbClient.saveUser(new SaveUserRequest(request.email(), hashed));
    }

    //  Login
    public AuthResponse login(AuthRequest request) {
        //  Fetch user
        UserDto user = dbClient.findUserByEmail(request.email());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        //  Check the password against the stored hash
        boolean matches = BCrypt.verifyer().verify(
                request.password().toCharArray(),
                user.passwordHash()).verified;

        if (!matches) {
            throw new RuntimeException("Wrong password");
        }

        //  Delete any other refresh token associated with the current account
        dbClient.deleteAllRefreshTokensByEmail(user.email());

        return issueTokens(user.email());
    }

    //  Refresh
    public AuthResponse refresh(String refreshToken) {
        //  Look up the refresh token in DB
        RefreshTokenDto stored = dbClient.findRefreshToken(refreshToken);

        //  Check it hasn't expired
        if (System.currentTimeMillis() > stored.expiresAt()) {
            dbClient.deleteRefreshToken(refreshToken); // clean up expired token
            throw new RuntimeException("Refresh token expired, please log in again");
        }

        //  Delete the old refresh token and issue a new pair
        dbClient.deleteRefreshToken(refreshToken);
        return issueTokens(stored.email());
    }

    //  Logout
    public void logout(String refreshToken) {
        //  Delete the refresh token
        //  The access token will expire on its own
        dbClient.deleteRefreshToken(refreshToken);
    }

    //  Helpers

    private AuthResponse issueTokens(String email) {
        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken();
        long expiresAt = System.currentTimeMillis() + refreshTokenExpiry;

        dbClient.saveRefreshToken(email, refreshToken, expiresAt);

        return new AuthResponse(accessToken, refreshToken);
    }
}
