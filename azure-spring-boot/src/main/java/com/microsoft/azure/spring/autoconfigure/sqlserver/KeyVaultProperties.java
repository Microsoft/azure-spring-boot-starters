/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.sqlserver;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

@Getter
@Setter
@ConfigurationProperties("azure.keyvault")
public class KeyVaultProperties {
    private String clientId;
    private String clientSecret;


    @PostConstruct
    public void validate() {
        Assert.hasText(clientId, "azure.keyvault.client-id must be provided");
        Assert.hasText(clientSecret, "azure.keyvault.client-secret must be provided");

    }
}
