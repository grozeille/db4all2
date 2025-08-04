package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.dto.CreateUserRequest;
import fr.grozeille.db4all.api.dto.LoginRequest;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.passay.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    private void validateUserCredentials(String login, String password) {
        // Rule: The login must be a valid email address.
        final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        if (login == null || !login.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format for login. Please use a valid email address.");
        }

        // Rule: Password must be strong.
        if (!isPasswordStrong(password)) {
            throw new IllegalArgumentException("Password is not strong enough.");
        }
    }

    @Transactional
    public void createInitialAdmin(LoginRequest loginRequest) {
        if (isInitialized()) {
            throw new IllegalStateException("Initialization already done.");
        }
        validateUserCredentials(loginRequest.getUsername(), loginRequest.getPassword());

        User user = new User();
        user.setEmail(loginRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(loginRequest.getPassword()));
        user.setSuperAdmin(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        validateUserCredentials(request.getLogin(), request.getPassword());

        if (userRepository.existsById(request.getLogin())) {
            throw new IllegalArgumentException("User with this login already exists.");
        }

        User user = new User();
        user.setEmail(request.getLogin());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setSuperAdmin(request.isSuperAdmin());
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String login) {
        if (!userRepository.existsById(login)) {
            throw new UsernameNotFoundException("User not found with login: " + login);
        }
        userRepository.deleteById(login);
    }

    @Transactional
    public void changeCurrentUserPassword(String oldPassword, String newPassword, Authentication authentication) {
        String login = authentication.getName();
        User user = userRepository.findById(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with login: " + login));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password is not strong enough.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateUserPasswordByAdmin(String login, String newPassword) {
        User user = userRepository.findById(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with login: " + login));

        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password is not strong enough.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateSuperAdminStatus(String login, boolean superAdmin, Authentication authentication) {
        // Rule: A user cannot change their own superAdmin status.
        if (authentication.getName().equals(login)) {
            throw new IllegalArgumentException("A user cannot change their own superAdmin status to prevent accidental lock-out.");
        }

        User user = userRepository.findById(login)
                .orElseThrow(() -> new RuntimeException("User not found: " + login));

        user.setSuperAdmin(superAdmin);
        return userRepository.save(user);
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
