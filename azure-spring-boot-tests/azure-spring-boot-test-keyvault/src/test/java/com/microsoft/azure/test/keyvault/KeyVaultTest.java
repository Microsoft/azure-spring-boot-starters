/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.test.keyvault;

import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.mgmt.Constants;
import com.microsoft.azure.test.AppRunner;
import com.microsoft.azure.mgmt.ClientSecretAccess;
import com.microsoft.azure.mgmt.KeyVaultTool;
import com.microsoft.azure.mgmt.ResourceGroupTool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static org.junit.Assert.assertEquals;

public class KeyVaultTest {
  
    private static ClientSecretAccess access;
    private static Vault vault;
    
    @BeforeClass
    public static void createKeyVault() {
        access = ClientSecretAccess.load();
        
        final KeyVaultTool tool = new KeyVaultTool(access);
        vault = tool.createVaultInNewGroup(Constants.TEST_RESOURCE_GROUP_NAME, "test-keyvault");
        vault.secrets().define("key").withValue("value").create();
        System.out.println("--------------------->resources provision over");
    }
    
    @AfterClass
    public static void deleteResourceGroup() {
        final ResourceGroupTool tool = new ResourceGroupTool(access);
        tool.deleteGroup(Constants.TEST_RESOURCE_GROUP_NAME);
        System.out.println("--------------------->resources clean over");
    }

    @Test
    public void keyVaultAsPropertySource() {
        try (AppRunner app = new AppRunner(DumbApp.class)) {
            app.property("azure.keyvault.enabled", "true");
            app.property("azure.keyvault.uri", vault.vaultUri());
            app.property("azure.keyvault.client-id", access.clientId());
            app.property("azure.keyvault.client-key", access.clientSecret());
            
            app.start();
            assertEquals("value", app.getProperty("key"));
            System.out.println("--------------------->test over");
        }
    }

    @Test
    public void keyVaultAsPropertySourceWithSpecificKeys() {
        try (AppRunner app = new AppRunner(DumbApp.class)) {
            app.property("azure.keyvault.enabled", "true");
            app.property("azure.keyvault.uri", vault.vaultUri());
            app.property("azure.keyvault.client-id", access.clientId());
            app.property("azure.keyvault.client-key", access.clientSecret());
            app.property("azure.keyvault.secret.keys", "key");

            app.start();
            assertEquals("value", app.getProperty("key"));
            System.out.println("--------------------->test over");
        }
    }


    
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class DumbApp {}
}
