package com.jobcraft.service;

import com.jobcraft.dto.AuthResponse;
import com.jobcraft.dto.ChangePasswordRequest;
import com.jobcraft.dto.LoginRequest;
import com.jobcraft.dto.RegisterRequest;
import com.jobcraft.dto.UserDto;
import com.jobcraft.exception.ConflictException;
import com.jobcraft.exception.InvalidCredentialsException;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.Profile;
import com.jobcraft.model.User;
import com.jobcraft.repository.ProfileRepository;
import com.jobcraft.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registrierung, Anmeldung und Passwortänderung.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-Mail oder Passwort ist falsch.";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Legt ein Konto samt leerem Briefkopf (Name, E-Mail) an und meldet den Benutzer direkt an.
     *
     * @param request Registrierungsdaten
     * @return Token und Benutzerdaten
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Für diese E-Mail-Adresse existiert bereits ein Konto.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        Profile profile = new Profile();
        profile.setUserId(user.getId());
        profile.setFullName(request.name().trim());
        profile.setEmail(email);
        profileRepository.save(profile);

        return new AuthResponse(jwtService.createToken(user), toDto(user, profile.getFullName()));
    }

    /**
     * Prüft die Anmeldedaten.
     *
     * @param request Anmeldedaten
     * @return Token und Benutzerdaten
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        return new AuthResponse(jwtService.createToken(user), toDto(user, displayName(user)));
    }

    /**
     * @param userId angemeldeter Benutzer
     * @return Benutzerdaten
     */
    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        User user = findUser(userId);
        return toDto(user, displayName(user));
    }

    /**
     * Ändert das Passwort nach Prüfung des aktuellen Passworts.
     *
     * @param userId  angemeldeter Benutzer
     * @param request altes und neues Passwort
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Das aktuelle Passwort ist falsch.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden."));
    }

    private String displayName(User user) {
        return profileRepository.findByUserId(user.getId()).map(Profile::getFullName).orElse(user.getEmail());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserDto toDto(User user, String name) {
        return new UserDto(user.getId(), user.getEmail(), name);
    }
}
