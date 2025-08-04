package fr.grozeille.dataprep.api.service;

import fr.grozeille.dataprep.api.dto.LoginRequest;
import fr.grozeille.dataprep.api.model.User;
import fr.grozeille.dataprep.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.passay.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public boolean isInitialized() {
        return userRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    @Transactional
    public void createInitialAdmin(LoginRequest loginRequest) {
        if (isInitialized()) {
            throw new IllegalStateException("Initialization already done.");
        }
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!isPasswordStrong(loginRequest.getPassword())) {
            throw new IllegalArgumentException("Password is not strong enough.");
        }

        User user = new User();
        user.setEmail(loginRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(loginRequest.getPassword()));
        user.setSuperAdmin(true);
        userRepository.save(user);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null) return false;
        PasswordValidator validator = new PasswordValidator(
            new LengthRule(8, 64),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(
                new CharacterData() {
                    public String getErrorCode() { return "INSUFFICIENT_DIGIT_OR_SPECIAL"; }
                    public String getCharacters() {
                        return EnglishCharacterData.Digit.getCharacters() + EnglishCharacterData.Special.getCharacters();
                    }
                }, 1
            )
        );
        RuleResult result = validator.validate(new PasswordData(password));
        return result.isValid();
    }
}
