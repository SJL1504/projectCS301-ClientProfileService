package com.example.client.ClientProfile.communication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class ClientAccountClient {

    private final WebClient webClient;
    private final String clientAccountServiceUrl;

    public ClientAccountClient(
            WebClient.Builder webClientBuilder,
            @Value("${client.account.service.url:http://client-profile-prod.client-profile-namespace-2025:8081}") String clientAccountServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(clientAccountServiceUrl).build();
        this.clientAccountServiceUrl = clientAccountServiceUrl;
    }

    public void deleteAllAccounts(String clientId, String agentId) {
        log.info("Deleting accounts for client {} using agent {}", clientId, agentId);

        try {
            webClient.delete()
                    .uri("/api/accounts/client/{clientId}", clientId)
                    .header("X-User-Id", agentId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() && status.value() != HttpStatus.NOT_FOUND.value(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("Client Account Service Client Error")
                                    .flatMap(message -> Mono.error(
                                            new WebClientResponseException(
                                                    message,
                                                    response.statusCode().value(),
                                                    response.statusCode().toString(),
                                                    response.headers().asHttpHeaders(),
                                                    null,
                                                    null
                                            )
                                    ))
                    )
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(5));

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Client accounts not found (404) for client: {} and agent: {}. Assuming already deleted.", clientId, agentId);
        } catch (WebClientResponseException e) {
            log.error("WebClient error deleting accounts for client: {} (Status: {})", clientId, e.getStatusCode(), e);
            throw new RuntimeException("Unable to delete client accounts due to downstream error", e);
        } catch (Exception e) {
            log.error("Unknown error deleting accounts for client: {}", clientId, e);
            throw new RuntimeException("Unable to delete client accounts", e);
        }
    }
}
