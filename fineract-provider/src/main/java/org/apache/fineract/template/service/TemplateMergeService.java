/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.template.domain.Template;
import org.apache.fineract.template.domain.TemplateFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TemplateMergeService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateMergeService.class);

    // private final FromJsonHelper fromApiJsonHelper;
    private Map<String, Object> scopes;
    private String authToken;

    public void setAuthToken(final String authToken) {
        this.authToken = authToken;
    }

    public String compile(final Template template, final Map<String, Object> scopes) throws IOException {
        this.scopes = scopes;
        this.scopes.put("static", new TemplateFunctions());

        final MustacheFactory mf = new DefaultMustacheFactory();
        final Mustache mustache = mf.compile(new StringReader(template.getText()), template.getName());

        final Map<String, Object> mappers = getCompiledMapFromMappers(template.getMappersAsMap());
        this.scopes.putAll(mappers);

        expandMapArrays(scopes);

        final StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, this.scopes);

        return stringWriter.toString();
    }

    private Map<String, Object> getCompiledMapFromMappers(final Map<String, String> data) {
        final MustacheFactory mf = new DefaultMustacheFactory();

        if (data != null) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                final Mustache mappersMustache = mf.compile(new StringReader(entry.getValue()), "");
                final StringWriter stringWriter = new StringWriter();

                mappersMustache.execute(stringWriter, this.scopes);
                String url = stringWriter.toString();
                if (!url.startsWith("http")) {
                    url = this.scopes.get("BASE_URI") + url;
                }
                try {
                    this.scopes.put(entry.getKey(), getMapFromUrl(url));
                } catch (final IOException e) {
                    LOG.error("getCompiledMapFromMappers() failed", e);
                }
            }
        }
        return this.scopes;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromUrl(final String url) throws IOException {
        final HttpURLConnection connection = getConnection(url);

        final String response = getStringFromInputStream(connection.getInputStream());
        HashMap<String, Object> result = new HashMap<>();
        if (connection.getContentType().equals("text/plain")) {
            result.put("src", response);
        } else {
            result = new ObjectMapper().readValue(response, HashMap.class);
        }
        return result;
    }

    private HttpURLConnection getConnection(final String url) {
        if (this.authToken == null) {
            final String name = SecurityContextHolder.getContext().getAuthentication().getName();
            final String password = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            Authenticator.setDefault(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(name, password.toCharArray());
                }
            });
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (this.authToken != null) {
                connection.setRequestProperty("Authorization", "Basic " + this.authToken);// NOSONAR
            }
            TrustModifier.relaxHostChecking(connection);

            connection.setDoInput(true);

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            LOG.error("getConnection() failed, return null", e);
        }

        return connection;
    }

    // TODO Replace this with appropriate alternative available in Guava
    private static String getStringFromInputStream(final InputStream is) {
        BufferedReader br = null;
        final StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (final IOException e) {
            LOG.error("getStringFromInputStream() failed", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    LOG.error("Problem occurred in getStringFromInputStream function", e);
                }
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void expandMapArrays(Object value) {
        if (value instanceof Map) {
            Map<String, Object> valueAsMap = (Map<String, Object>) value;
            // Map<String, Object> newValue = null;
            Map<String, Object> valueAsMap_second = new HashMap<>();
            for (Map.Entry<String, Object> valueAsMapEntry : valueAsMap.entrySet()) {
                Object valueAsMapEntryValue = valueAsMapEntry.getValue();
                if (valueAsMapEntryValue instanceof Map) { // JSON Object
                    expandMapArrays(valueAsMapEntryValue);
                } else if (valueAsMapEntryValue instanceof Iterable) { // JSON
                                                                       // Array
                    Iterable<Object> valueAsMapEntryValueIterable = (Iterable<Object>) valueAsMapEntryValue;
                    String valueAsMapEntryKey = valueAsMapEntry.getKey();
                    int i = 0;
                    for (Object object : valueAsMapEntryValueIterable) {
                        valueAsMap_second.put(valueAsMapEntryKey + "#" + i, object);
                        ++i;
                        expandMapArrays(object);

                    }
                }

            }
            valueAsMap.putAll(valueAsMap_second);

        }
    }

}
