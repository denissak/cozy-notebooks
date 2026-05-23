package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.AccountDtos.AccountLimitsResponse;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.UserPlanLimitsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account")
public class AccountController {

    private final UserPlanLimitsService userPlanLimitsService;
    private final CurrentUserProvider currentUserProvider;

    public AccountController(UserPlanLimitsService userPlanLimitsService,
                             CurrentUserProvider currentUserProvider) {
        this.userPlanLimitsService = userPlanLimitsService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/limits")
    public AccountLimitsResponse limits() {
        return userPlanLimitsService.getAccountLimits(currentUserProvider.requireId());
    }
}
