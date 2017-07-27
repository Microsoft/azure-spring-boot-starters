/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.msgraph.api.impl;

import com.microsoft.azure.msgraph.api.Microsoft;
import com.microsoft.azure.msgraph.api.UserOperations;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.support.ClientHttpRequestFactorySelector;
import org.springframework.social.support.URIBuilder;

import java.net.URI;

public class MicrosoftTemplate extends AbstractOAuth2ApiBinding implements Microsoft {
    private static final String MS_GRAPH_BASE_API = "https://graph.microsoft.com/v1.0/";
    private UserOperations userOperations;

    public MicrosoftTemplate() {
        initialize();
    }

    public MicrosoftTemplate(String accessToken) {
        super(accessToken);
        initialize();
    }

    public String getGraphAPI(String path) {
        return MS_GRAPH_BASE_API + path;
    }

    public <T> T fetchObject(String objectId, Class<T> type) {
        URI uri = URIBuilder.fromUri(getGraphAPI(objectId)).build();
        return getRestTemplate().getForObject(uri, type);
    }

    @Override
    public UserOperations userOperations() {
        return userOperations;
    }

    private void initialize() {
        super.setRequestFactory(ClientHttpRequestFactorySelector.bufferRequests(getRestTemplate().getRequestFactory()));
        initSubApis();
    }

    private void initSubApis() {
        userOperations = new UserTemplate(this, isAuthorized());
    }
}
