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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static feign.Util.ensureClosed;

public class AllureResponseDecoder implements Decoder {

    private final Decoder decoder;

    private final DefaultAttachmentProcessor processor;

    public AllureResponseDecoder(Decoder decoder) {
        this.decoder = decoder;
        this.processor = new DefaultAttachmentProcessor();
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        var request = response.request();

        var requestAttachmentBuilder = HttpRequestAttachment.Builder.create("Request", request.url())
                .setMethod(request.httpMethod().name())
                .setHeaders(headers(request.headers()));

        if (Objects.nonNull(request.body())) {
            requestAttachmentBuilder.setBody(new String(request.body(), request.charset()));
        }

        processor.addAttachment(
                requestAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );

        var responsetAttachmentBuilder = HttpResponseAttachment.Builder.create("Response")
                .setResponseCode(response.status())
                .setHeaders(headers(response.headers()));

        if (Objects.nonNull(response.body())) {
            var body = response.body().asInputStream().readAllBytes();
            ensureClosed(response.body());

            responsetAttachmentBuilder.setBody(new String(body, response.charset()));

            response = response.toBuilder().body(body).build();
        }

        processor.addAttachment(
                responsetAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-response.ftl")
        );

        return decoder.decode(response, type);
    }

    private Map<String, String> headers(Map<String, Collection<String>> headers) {
        return headers.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        y -> String.join(", ", y.getValue())
                ));
    }

}
