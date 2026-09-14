package com.github.nurbolotalt.bank_app.auth;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.github.nurbolotalt.bank_app.User.User;
import com.github.nurbolotalt.bank_app.User.UserRepository;
import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Account.AccountRepository;

@Service
public class AuthService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AccountRepository accountRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
    }
    
    public void register(RegisterRequest request) {
        String email = request.email();
        String password = request.password();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = new User(email, hashedPassword);
        newUser = userRepository.save(newUser);

        String accountNumber = "ACC-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Account newAccount = new Account(newUser, accountNumber, "USD");
        accountRepository.save(newAccount);
    }

    public String login(LoginRequest request) {
        String username = request.email();
        String password = request.password();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return jwtService.generateToken(user.getEmail());
    }
}
