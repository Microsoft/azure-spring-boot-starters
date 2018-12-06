/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.b2c;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This handler will take care of the url with error and error description query. It is the workaround
 * for AAD B2C sign in page cannot redirect users to password reset policy or default password reset page.
 * This class will be removed after AAD B2C fix that issue.
 */
@Slf4j
public class AADB2CFilterForgotPasswordHandler extends AbstractAADB2CFilterScenarioHandler implements
        AADB2CFilterScenarioHandler {

    private final AADB2CProperties b2cProperties;

    private static final String FORGOT_PASSWORD_CODE = "AADB2C90118";

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public AADB2CFilterForgotPasswordHandler(@NonNull AADB2CProperties b2cProperties) {
        this.b2cProperties = b2cProperties;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            auth.setAuthenticated(false);
        }

        log.debug("The user has forgotten their password, will redirect to password reset.");

        final String requestURL = request.getRequestURL().toString();
        final String url = AADB2CURL.getOpenIdPasswordResetURL(b2cProperties, requestURL, request);

        redirectStrategy.sendRedirect(request, response, url);
    }

    @Override
    public Boolean matches(HttpServletRequest request) {
        final String errorMessage = request.getParameter(PARAMETER_ERROR_DESCRIPTION);

        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.contains(FORGOT_PASSWORD_CODE);
        } else {
            return false;
        }
    }
}
