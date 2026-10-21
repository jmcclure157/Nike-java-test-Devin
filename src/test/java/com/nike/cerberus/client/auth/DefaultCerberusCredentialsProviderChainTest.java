/*
 * Copyright (c) 2018 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.client.auth;

import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;
import uk.org.webcompere.systemstubs.rules.SystemPropertiesRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the DefaultCerberusCredentialsProviderChain class
 */
public class DefaultCerberusCredentialsProviderChainTest {

    @Rule
    public final EnvironmentVariablesRule environmentVariables = new EnvironmentVariablesRule();

    @Rule
    public final SystemPropertiesRule systemProperties = new SystemPropertiesRule();

    private static final String ENV_VALUE = "ENV";

    private static final String SYS_VALUE = "SYS";

    private static final String region = "us-west-2";

    private static final String url = "url";

    private static final String xCerberusClientOverride = "xCerberusClientOverride";

    private static final OkHttpClient httpClient = new OkHttpClient();

    private DefaultCerberusCredentialsProviderChain credentialsProviderChain;

    @Before
    public void setup() {
        credentialsProviderChain = new DefaultCerberusCredentialsProviderChain(url, region);
    }

    @Test
    public void env_set_credentials_always_returned_when_sys_property_is_also_set() {
        environmentVariables.set(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY, ENV_VALUE);
        systemProperties.set(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY, SYS_VALUE);

        CerberusCredentials credentials = credentialsProviderChain.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(ENV_VALUE);
    }

    @Test
    public void sys_value_set_if_env_is_not_set() {
        environmentVariables.set(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY, "");
        systemProperties.set(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY, SYS_VALUE);

        CerberusCredentials credentials = credentialsProviderChain.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(SYS_VALUE);
    }

    @Test
    public void testCreatDefaultCerberusCredentialsProviderChainReturnsValidObject() {
        DefaultCerberusCredentialsProviderChain credentialsProviderChain = new DefaultCerberusCredentialsProviderChain(url, region, xCerberusClientOverride);
        Assert.assertNotNull(credentialsProviderChain);
    }

    @Test
    public void testCreatDefaultCerberusCredentialsProviderChainWithHttpClientReturnsValidObject() {
        DefaultCerberusCredentialsProviderChain credentialsProviderChain = new DefaultCerberusCredentialsProviderChain(url, region, httpClient);
        Assert.assertNotNull(credentialsProviderChain);
    }
}