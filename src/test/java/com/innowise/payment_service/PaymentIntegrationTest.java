package com.innowise.payment_service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.payment_service.dto.PaymentCreateRequest;
import com.innowise.payment_service.dto.PaymentResponse;
import com.innowise.payment_service.enums.PaymentStatus;
import com.innowise.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("external.payment-api.url", () -> wireMockServer.baseUrl() + "/random");
        registry.add("logging.level.org.mongodb.driver.cluster", () -> "WARN");
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }
    }

    @Autowired
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentAndSendKafkaEvent_WhenApiReturnsEvenNumber() {
        wireMockServer.stubFor(get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("2")));

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(100L);
        request.setUserId(200L);
        request.setPaymentAmount(BigDecimal.valueOf(500));

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response.getId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(100L, response.getOrderId());

    }

    @Test
    void shouldFailPayment_WhenApiReturnsOddNumber() {
        wireMockServer.stubFor(get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("3")));

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(101L);
        request.setUserId(200L);
        request.setPaymentAmount(BigDecimal.valueOf(150));

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response.getId());
        assertEquals(PaymentStatus.FAILED, response.getStatus());
    }
}