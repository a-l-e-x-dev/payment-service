package com.innowise.payment_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalPaymentClient {

    private final RestTemplate restTemplate;

    @Value("${external.payment-api.url:https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new}")
    private String apiUrl;

    public boolean processPayment() {
        try {
            String url = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null) {
                int randomNumber = Integer.parseInt(response.trim());
                log.info("External API returned random number: {}", randomNumber);

                return randomNumber % 2 == 0;
            }
        } catch (Exception e) {
            log.error("Error calling external payment API: {}", e.getMessage());
        }

        return false;
    }
}