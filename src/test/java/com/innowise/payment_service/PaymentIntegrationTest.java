package com.innowise.payment_service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.payment_service.dto.PaymentCreateRequest;
import com.innowise.payment_service.dto.PaymentResponse;
import com.innowise.payment_service.enums.PaymentStatus;
import com.innowise.payment_service.event.PaymentEvent;
import com.innowise.payment_service.service.PaymentService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    static WireMockServer wireMockServer;

    @Autowired
    private PaymentService paymentService;

    private static final List<PaymentEvent> consumedEvents = new ArrayList<>();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("external.payment-api.url", () -> wireMockServer.baseUrl() + "/random");
    }

    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void clearEvents() {
        consumedEvents.clear();
    }

    @KafkaListener(topics = "payment-events", groupId = "test-group")
    public void listen(PaymentEvent event) {
        consumedEvents.add(event);
    }

    @Test
    void shouldProcessPayment_SaveToMongo_AndSendToKafka() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("2")));

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(100L);
        request.setUserId(50L);
        request.setPaymentAmount(BigDecimal.valueOf(99.99));

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response.getId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertEquals(1, consumedEvents.size());
                    PaymentEvent event = consumedEvents.get(0);
                    assertEquals(100L, event.getOrderId());
                    assertEquals("SUCCESS", event.getPaymentStatus());
                });
    }
}