package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.exceptions.PasswordTooWeakException;
import fr.grozeille.db4all.api.exceptions.UserAlreadyExistsException;
import fr.grozeille.db4all.api.exceptions.UserNotFoundException;
import fr.grozeille.db4all.api.exceptions.SelfStatusChangeForbiddenException;
import fr.grozeille.db4all.api.exceptions.WrongPasswordException;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.util.Strings;
import org.passay.*;
import org.passay.CharacterData;
import org.springframework.security.core.Authentication;
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
    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(username);
        }
        User user = userOpt.get();
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new WrongPasswordException();
        }
        return user;
    }

    private void validateEmail(String email) {
        // Rule: The email must be a valid email address.
        final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9\\.-]+\\.[a-zA-Z]{2,6}$";
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format. Please use a valid email address.");
        }
    }

    private void validatePassword(String password) {
        // Rule: Password must be strong.
        if (password == null) throw new PasswordTooWeakException("Password cannot be null.");
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
        
        if(!result.isValid()) {
            throw new PasswordTooWeakException("The password is not strong enough: \n" +
                Strings.join(validator.getMessages(result), '\n'));
        }
    }

    @Transactional
    public void createInitialAdmin(String email, String password) {
        if (isInitialized()) {
            throw new IllegalStateException("Initialization already done.");
        }
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsById(email)) {
            throw new UserAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setSuperAdmin(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createUser(String email, String password, boolean isSuperAdmin) {
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsById(email)) {
            throw new UserAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setSuperAdmin(isSuperAdmin);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String email) {
        if (!userRepository.existsById(email)) {
            throw new UserNotFoundException(email);
        }
        userRepository.deleteById(email);
    }

    @Transactional
    public void changeCurrentUserPassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new WrongPasswordException();
        }

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateUserPasswordByAdmin(String email, String newPassword) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateSuperAdminStatus(String email, boolean superAdmin, Authentication authentication) {
        // Rule: A user cannot change their own superAdmin status.
        if (authentication.getName().equals(email)) {
            throw new SelfStatusChangeForbiddenException("A user cannot change their own superAdmin status to prevent accidental lock-out.");
        }

        User user = userRepository.findById(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        user.setSuperAdmin(superAdmin);
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return this.userRepository.findByEmail(email).orElse(null);
    }
}
