/*
 * Copyright 2025 Sergei Bushmelev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bu.allure.open.feign.decoder;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Feign {@link Decoder} implementation that captures HTTP request and response details
 * and attaches them to Allure reports as attachments.
 * <p>
 * This decoder wraps an existing Feign decoder and adds Allure reporting capabilities.
 * It intercepts all Feign client responses, extracts request/response details,
 * generates Allure attachments, and then delegates the actual decoding to the underlying decoder.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * Feign.builder()
 *      .decoder(new AllureResponseDecoder(new GsonDecoder()))
 *      .target(MyApi.class, "http://example.com");
 * }</pre>
 */
public class AllureResponseDecoder implements Decoder {

    private final Decoder decoder;

    /**
     * Creates a new AllureResponseDecoder wrapping the specified decoder.
     *
     * @param decoder the underlying decoder to delegate actual decoding to
     */
    public AllureResponseDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        var request = response.request();

        var requestAttachmentBuilder = HttpRequestAttachment.Builder.create("Request", request.url())
                .setMethod(request.httpMethod().name())
                .setHeaders(headers(request.headers()));

        if (Objects.nonNull(request.body())) {
            Charset charset = request.charset() == null ? StandardCharsets.UTF_8 : request.charset();
            requestAttachmentBuilder.setBody(new String(request.body(), charset));
        }

        new DefaultAttachmentProcessor().addAttachment(
                requestAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );

        var responseAttachmentBuilder = HttpResponseAttachment.Builder.create("Response")
                .setResponseCode(response.status())
                .setHeaders(headers(response.headers()));

        Response.Builder builder = response.toBuilder();

        if (Objects.nonNull(response.body())) {
            try (InputStream bodyStream = response.body().asInputStream()) {
                byte[] body = bodyStream.readAllBytes();
                Charset charset = response.charset() == null ? StandardCharsets.UTF_8 : response.charset();
                responseAttachmentBuilder.setBody(new String(body, charset));
                builder.body(body);
            } catch (IOException e) {
                throw new DecodeException(response.status(), "Failed to read response body", request, e);
            }
        }

        new DefaultAttachmentProcessor().addAttachment(
                responseAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-response.ftl")
        );

        return decoder.decode(builder.build(), type);
    }

    /**
     * Converts HTTP headers from Map<String, Collection<String>> format to Map<String, String> format
     * by joining multiple header values with commas.
     *
     * @param headers the headers map to convert
     * @return a new map with header values joined by commas
     */
    private Map<String, String> headers(Map<String, Collection<String>> headers) {
        if (headers == null) {
            return Map.of();
        } else {
            return headers.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> "Set-Cookie".equalsIgnoreCase(entry.getKey())
                                    ? String.join("\n", entry.getValue())
                                    : String.join(", ", entry.getValue())
                    ));
        }
    }

}
