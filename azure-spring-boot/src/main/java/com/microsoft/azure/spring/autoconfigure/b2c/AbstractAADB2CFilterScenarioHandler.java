/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.b2c;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractAADB2CFilterScenarioHandler {

    protected static final String PARAMETER_CODE = "code";

    protected static final String PARAMETER_ID_TOKEN = "id_token";

    protected static final String PARAMETER_STATE = "state";

    protected static final String PARAMETER_ERROR = "error";

    protected static final String PARAMETER_ERROR_DESCRIPTION = "error_description";

    protected void validatePolicyReply(HttpServletRequest request) throws AADB2CAuthenticationException {
        if (StringUtils.hasText(request.getParameter(PARAMETER_ERROR))) {
            final String code = request.getParameter(PARAMETER_ERROR);
            final String description = request.getParameter(PARAMETER_ERROR_DESCRIPTION);
            final String message = String.format("%s:%s.", code, description);

            throw new AADB2CAuthenticationException("Authentication failure: " + message);
        }
    }

    protected boolean isAuthenticated(Authentication auth) {
        if (auth == null) {
            return false;
        } else if (auth instanceof PreAuthenticatedAuthenticationToken) {
            final UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            return !principal.isUserExpired() && principal.isUserValid();
        } else {
            return auth.isAuthenticated();
        }
    }

    protected void updateAuthentication() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            auth.setAuthenticated(isAuthenticated(auth));
        }
    }
}
