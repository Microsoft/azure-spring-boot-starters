/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;


@RunWith(MockitoJUnitRunner.class)
public class UserPrincipalTest {

    private AzureADGraphClient graphClient;
    private AADAuthenticationProperties aadAuthProps;

    @Mock
    private ServiceEndpointsProperties endpointsProps;

    @Mock
    private AADGraphHttpClient aadGraphHttpClient;


    @Before
    public void setUp() throws Exception {
        aadAuthProps = new AADAuthenticationProperties();
    }

    @Test
    public void getAuthoritiesByUserGroups() throws Exception {


        setupGraphClient(Collections.singletonList("group1"));

        doReturn(Constants.USERGROUPS_JSON)
                .when(aadGraphHttpClient).getMemberships(Constants.BEARER_TOKEN);
        final Collection<? extends GrantedAuthority> authorities =
                graphClient.getGrantedAuthorities(Constants.BEARER_TOKEN);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_group1");
    }


    @Test
    public void getGroups() throws Exception {
        setupGraphClient(Constants.TARGETED_GROUPS);

        doReturn(Constants.USERGROUPS_JSON)
                .when(aadGraphHttpClient).getMemberships(Constants.BEARER_TOKEN);

        final List<UserGroup> groups = graphClient.getGroups(Constants.BEARER_TOKEN);


        assertThat(groups).hasSize(3).extracting(UserGroup::getDisplayName)
                .containsExactly("group1", "group2", "group3");
    }

    @Test
    public void userPrinciplaIsSerializable() throws ParseException, IOException, ClassNotFoundException {
        final File tmpOutputFile = File.createTempFile("test-user-principal", "txt");

        try (final FileOutputStream fileOutputStream = new FileOutputStream(tmpOutputFile);
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
             final FileInputStream fileInputStream = new FileInputStream(tmpOutputFile);
             final ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);) {

            final JWSObject jwsObject = JWSObject.parse(Constants.JWT_TOKEN);
            final JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().subject("fake-subject").build();
            final UserPrincipal principal = new UserPrincipal(jwsObject, jwtClaimsSet);

            objectOutputStream.writeObject(principal);

            final UserPrincipal serializedPrincipal = (UserPrincipal) objectInputStream.readObject();

            Assert.assertNotNull("Serialized UserPrincipal not null", serializedPrincipal);
            Assert.assertTrue("Serialized UserPrincipal kid not empty",
                    !StringUtils.isEmpty(serializedPrincipal.getKid()));
            Assert.assertNotNull("Serialized UserPrincipal claims not null.", serializedPrincipal.getClaims());
            Assert.assertTrue("Serialized UserPrincipal claims not empty.",
                    serializedPrincipal.getClaims().size() > 0);
        } finally {
            Files.deleteIfExists(tmpOutputFile.toPath());

        }
    }

    /**
     * Sets up a new {@link AzureADGraphClient}. Before initialization
     * sets expected groups on the aadAuthProps.
     *
     * @param expectedGroups - groups that should extracted from the member of call.
     */
    private void setupGraphClient(List<String> expectedGroups) {
        aadAuthProps.setactiveDirectoryGroups(expectedGroups);
        this.graphClient = new AzureADGraphClient("test", "password",
                aadAuthProps, endpointsProps, aadGraphHttpClient);
    }
}
