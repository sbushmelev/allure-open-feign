package bu.allure.open.feign.decoder;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.Feign;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AllureResponseDecoderTests {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void jsonBodyTest() {
        wireMock.stubFor(
                get(urlEqualTo("/api/v1/json"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                        "message":"Hello World"
                                        }
                                        """)));

        AtomicReference<HelloWorldRecord> helloWorldRecord = new AtomicReference<>();

        AllureResults allureResults = runWithinTestContext(() -> {
            helloWorldRecord.set(Feign.builder()
                    .decoder(new AllureResponseDecoder(new GsonDecoder()))
                    .target(HelloWorldFeignClient.class, wireMock.baseUrl())
                    .getJsonHelloWorld());
        });

        List<String> attachmentNames = allureResults.getTestResults().stream()
                .flatMap(testResult -> testResult.getAttachments().stream())
                .map(Attachment::getName).toList();

        assertAll(
                () -> assertEquals(new HelloWorldRecord("Hello World"), helloWorldRecord.get()),
                () -> assertTrue(attachmentNames.contains("Response"), "Cannot find attachment with name \"Response\""),
                () -> assertTrue(attachmentNames.contains("Request"), "Cannot find attachment with name \"Request\"")
        );
    }

    interface HelloWorldFeignClient {

        @RequestLine("GET /api/v1/json")
        HelloWorldRecord getJsonHelloWorld();

    }

    record HelloWorldRecord(String message) { }

}
