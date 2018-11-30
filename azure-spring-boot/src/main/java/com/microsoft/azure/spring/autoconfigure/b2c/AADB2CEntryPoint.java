/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.b2c;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

@Slf4j
public class AADB2CEntryPoint implements AuthenticationEntryPoint {

    private final AADB2CProperties b2cProperties;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public AADB2CEntryPoint(@NonNull AADB2CProperties properties) {
        this.b2cProperties = properties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        String requestURL = request.getRequestURL().toString();
        final URL url = new URL(requestURL);

        // By default, request URL will be '/error' when exception handling, here handle this endpoint to
        // policy redirect URL instead of redirect to '/error' directly.
        if (url.getPath().equalsIgnoreCase("/error")) {
            requestURL = this.b2cProperties.getPolicies().getSignUpOrSignIn().getReplyURL();
        }

        final String redirectURL = AADB2CURL.getOpenIdSignUpOrSignInURL(b2cProperties, requestURL, request);

        log.debug("Authentication is required to access URL {}. Redirecting to {}.", requestURL, redirectURL);

        redirectStrategy.sendRedirect(request, response, redirectURL);
    }
}
