package com.library.auth;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.common.exception.BadRequestException;
import com.library.common.security.JwtService;
import com.library.member.CardNumbers;
import com.library.member.Role;
import com.library.member.User;
import com.library.member.UserRepository;
import com.library.member.UserResponse;
import com.library.settings.SettingsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       SettingsService settingsService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.settingsService = settingsService;
        this.auditService = auditService;
    }

    /** Self-registration always creates a member of the default type, with a new library card. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.MEMBER);
        user.setMemberType(settingsService.current().getDefaultMemberType());
        userRepository.save(user);
        user.setCardNumber(CardNumbers.forMember(user.getId()));
        auditService.record(user, AuditAction.MEMBER_REGISTERED, "User", user.getId(),
                "registered with card " + user.getCardNumber());
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /** Checks the email and password (throws BadCredentialsException if wrong) and returns a token. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        User user = (User) auth.getPrincipal();
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
