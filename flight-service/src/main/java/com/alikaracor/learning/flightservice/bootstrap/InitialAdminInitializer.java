package com.alikaracor.learning.flightservice.bootstrap;


import com.alikaracor.learning.flightservice.dto.RegisterRequest;
import com.alikaracor.learning.flightservice.model.RoleName;
import com.alikaracor.learning.flightservice.repository.UserRepository;
import com.alikaracor.learning.flightservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public InitialAdminInitializer(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Value("${app.initial-admin.username}")
    private String adminUserName;

    @Value("${app.initial-admin.email}")
    private String adminEmail;

    @Value("${app.initial-admin.password}")
    private String adminPassword;


    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.existsByUserRoles_RoleName(RoleName.ADMIN)) {
            return;

        }

        if (adminUserName.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {

            throw new IllegalStateException(
                    "İlk admin bilgileri environment variables içinde bulunamadı");

        }

        RegisterRequest firstAdminRequest = new RegisterRequest();

        firstAdminRequest.setUserEmail(adminEmail);
        firstAdminRequest.setUserPassword(adminPassword);
        firstAdminRequest.setUserName(adminUserName);

        firstAdminRequest.setUserRoleNames(Set.of(RoleName.ADMIN));

        userService.registerUser(firstAdminRequest,null,null);


    }
}
