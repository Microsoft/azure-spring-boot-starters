/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.aad.adal4j.UserAssertion;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.naming.ServiceUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class AzureADHelper {
    private static final SimpleGrantedAuthority DEFAULT_AUTHORITY = new SimpleGrantedAuthority("ROLE_USER");
    private static final String DEFAULE_ROLE_PREFIX = "ROLE_";

    public static String getUserMembershipsV1(String accessToken, String aadMembershipRestAPI) throws IOException {
        final URL url = new URL(aadMembershipRestAPI);

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // Set the appropriate header fields in the request header.
        conn.setRequestProperty("api-version", "1.6");
        conn.setRequestProperty("Authorization", accessToken);
        conn.setRequestProperty("Accept", "application/json;odata=minimalmetadata");
        final String responseInJson = getResponseStringFromConn(conn);
        final int responseCode = conn.getResponseCode();
        if (responseCode == HTTPResponse.SC_OK) {
            return responseInJson;
        } else {
            throw new IllegalStateException("Response is not " + HTTPResponse.SC_OK +
                    ", response json: " + responseInJson);
        }
    }

    private static String getResponseStringFromConn(HttpURLConnection conn) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            final StringBuilder stringBuffer = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            return stringBuffer.toString();
        }
    }

    public static AuthenticationResult acquireTokenForGraphApi(
            String idToken, AADAuthenticationProperties aadAuthProperties, ServiceEndpoints serviceEndpoints)
            throws MalformedURLException, ServiceUnavailableException, InterruptedException, ExecutionException {

        final ClientCredential credential =
                new ClientCredential(aadAuthProperties.getClientId(), aadAuthProperties.getClientSecret());
        final UserAssertion assertion = new UserAssertion(idToken);

        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            final AuthenticationContext context = new AuthenticationContext(
                    serviceEndpoints.getAadSigninUri() + aadAuthProperties.getTenantId() + "/",
                    true, service);
            final Future<AuthenticationResult> future = context
                    .acquireToken(serviceEndpoints.getAadGraphApiUri(), assertion, credential, null);
            result = future.get();
        } finally {
            if (service != null) {
                service.shutdown();
            }
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "unable to acquire on-behalf-of token for client " + aadAuthProperties.getClientId());
        }
        return result;
    }

    private static List<UserGroup> getGroups(String graphApiToken, String memberUrl) throws IOException {
        return loadUserGroups(graphApiToken, memberUrl);
    }

    private static List<UserGroup> loadUserGroups(String graphApiToken, String memberUrl)
            throws IOException {
        final String responseInJson =
                AzureADHelper.getUserMembershipsV1(graphApiToken, memberUrl);
        final List<UserGroup> lUserGroups = new ArrayList<>();
        final ObjectMapper objectMapper = JacksonObjectMapperFactory.getInstance();
        final JsonNode rootNode = objectMapper.readValue(responseInJson, JsonNode.class);
        final JsonNode valuesNode = rootNode.get("value");
        int i = 0;
        while (valuesNode != null
                && valuesNode.get(i) != null) {
            if (valuesNode.get(i).get("objectType").asText().equals("Group")) {
                lUserGroups.add(new UserGroup(
                        valuesNode.get(i).get("objectId").asText(),
                        valuesNode.get(i).get("displayName").asText()));
            }
            i++;
        }
        return lUserGroups;
    }

    public static Set<GrantedAuthority> mapGroupToAuthorities(String graphApiToken, String memberUrl,
                                                              List<String> targetedGroupNames)
            throws IOException{
        // Fetch the authority information from the protected resource using accessToken
        final List<UserGroup> groups = getGroups(graphApiToken, memberUrl);

        // Map the authority information to one or more GrantedAuthority's and add it to mappedAuthorities
        final Set<GrantedAuthority> mappedAuthorities = groups.stream()
                .filter(group -> targetedGroupNames.contains(group.getDisplayName()))
                .map(userGroup -> new SimpleGrantedAuthority(DEFAULE_ROLE_PREFIX + userGroup.getDisplayName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (mappedAuthorities.isEmpty()) {
            mappedAuthorities.add(DEFAULT_AUTHORITY);
        }

        return mappedAuthorities;
    }
}
