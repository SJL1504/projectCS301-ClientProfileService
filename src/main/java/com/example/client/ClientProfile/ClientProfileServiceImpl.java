package com.example.client.ClientProfile;

import com.example.client.ClientProfile.dto.ClientProfileCreateDTO;
import com.example.client.ClientProfile.dto.ClientProfileUpdateDTO;
import com.example.client.ClientProfile.communication.ClientAccountClient;
import com.example.client.exception.ClientDeletedException;
import com.example.client.exception.ClientNotFoundException;
import com.example.client.exception.UnauthorizedAgentAccessException;
import com.example.client.logging.LogEvent;
import com.example.client.logging.LogPublisher;
import com.example.client.cache.HybridCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientProfileRepository repository;
    private final LogPublisher logPublisher;
    private final ClientAccountClient clientAccountClient;
    private final HybridCacheService cacheService;

    @Value("${aws.sns.topic.arn}")
    private String topicArn;

    private static final String CACHE_CLIENTS = "clientProfiles";
    private static final String CACHE_CLIENTS_BY_AGENT = "clientProfilesByAgent";

    public ClientProfileServiceImpl(ClientProfileRepository repository,
                                    LogPublisher logPublisher,
                                    ClientAccountClient clientAccountClient,
                                    HybridCacheService cacheService) {
        this.repository = repository;
        this.logPublisher = logPublisher;
        this.clientAccountClient = clientAccountClient;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    public ClientProfile createClient(ClientProfileCreateDTO dto, String agentId) {
        log.info("Creating client profile for agent: {}", agentId);

        String clientId = "CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ClientProfile profile = ClientProfile.builder()
                .clientId(clientId)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .agentId(agentId)
                .status(ClientProfile.Status.PENDING)
                .build();

        ClientProfile created = repository.save(profile);

        cacheService.putAsync(CACHE_CLIENTS, created.getClientId(), created);
        cacheService.evictAsync(CACHE_CLIENTS_BY_AGENT, agentId);

        logPublisher.publishFireAndForget(topicArn, new LogEvent(
                "CREATE", clientId, null, clientId, agentId, clientId, created.getEmail()
        ));

        return created;
    }


    @Override
    @Transactional
    public ClientProfile updateClient(String clientId, ClientProfileUpdateDTO dto, String agentId) {
        log.info("Updating client profile: {} for agent: {}", clientId, agentId);

        ClientProfile existing = getAuthorizedClient(clientId, agentId);
        String email = existing.getEmail(); // Get email once for all log events

        if (dto.getFirstName() != null && !dto.getFirstName().equals(existing.getFirstName())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "firstName", existing.getFirstName(), dto.getFirstName(), agentId, clientId, email
            ));
            existing.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().equals(existing.getLastName())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "lastName", existing.getLastName(), dto.getLastName(), agentId, clientId, email
            ));
            existing.setLastName(dto.getLastName());
        }

        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().equals(existing.getDateOfBirth())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "dateOfBirth", existing.getDateOfBirth().toString(), dto.getDateOfBirth().toString(), agentId, clientId, email
            ));
            existing.setDateOfBirth(dto.getDateOfBirth());
        }

        if (dto.getGender() != null && !dto.getGender().equals(existing.getGender())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "gender", existing.getGender(), dto.getGender(), agentId, clientId, email
            ));
            existing.setGender(dto.getGender());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(existing.getEmail())) {
            if (repository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already exists in the system");
            }
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "email", existing.getEmail(), dto.getEmail(), agentId, clientId, dto.getEmail()
            ));
            existing.setEmail(dto.getEmail());
            email = dto.getEmail(); // Update email variable for subsequent logs
        }

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().equals(existing.getPhoneNumber())) {
            if (repository.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new IllegalArgumentException("Phone number already exists in the system");
            }
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "phoneNumber", existing.getPhoneNumber(), dto.getPhoneNumber(), agentId, clientId, email
            ));
            existing.setPhoneNumber(dto.getPhoneNumber());
        }

        if (dto.getAddress() != null && !dto.getAddress().equals(existing.getAddress())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "address", existing.getAddress(), dto.getAddress(), agentId, clientId, email
            ));
            existing.setAddress(dto.getAddress());
        }

        if (dto.getCity() != null && !dto.getCity().equals(existing.getCity())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "city", existing.getCity(), dto.getCity(), agentId, clientId, email
            ));
            existing.setCity(dto.getCity());
        }

        if (dto.getState() != null && !dto.getState().equals(existing.getState())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "state", existing.getState(), dto.getState(), agentId, clientId, email
            ));
            existing.setState(dto.getState());
        }

        if (dto.getCountry() != null && !dto.getCountry().equals(existing.getCountry())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "country", existing.getCountry(), dto.getCountry(), agentId, clientId, email
            ));
            existing.setCountry(dto.getCountry());
        }

        if (dto.getPostalCode() != null && !dto.getPostalCode().equals(existing.getPostalCode())) {
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "UPDATE", "postalCode", existing.getPostalCode(), dto.getPostalCode(), agentId, clientId, email
            ));
            existing.setPostalCode(dto.getPostalCode());
        }

        ClientProfile updated = repository.save(existing);

        // Update caches
        cacheService.putAsync(CACHE_CLIENTS, clientId, updated);
        cacheService.evictAsync(CACHE_CLIENTS_BY_AGENT, agentId);

        log.info("Client profile updated successfully: {}", clientId);
        return updated;
    }

    @Override
    @Transactional
    public ClientProfile patchClientStatus(String clientId, ClientProfile.Status status, String agentId) {
        log.info("Patching status for client profile: {} to {} by agent: {}", clientId, status, agentId);

        ClientProfile existing = getAuthorizedClient(clientId, agentId);
        String oldStatus = existing.getStatus() != null ? existing.getStatus().toString() : "null";

        logPublisher.publishFireAndForget(topicArn, new LogEvent(
                "UPDATE", "status", oldStatus, status.toString(), agentId, clientId, existing.getEmail()
        ));

        existing.setStatus(status);
        ClientProfile updated = repository.save(existing);

        // Update caches
        cacheService.putAsync(CACHE_CLIENTS, clientId, updated);
        cacheService.evictAsync(CACHE_CLIENTS_BY_AGENT, agentId);

        log.info("Client profile status updated successfully: {}", clientId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteClient(String clientId, String agentId) {
        log.info("Soft deleting client profile: {} by agent: {}", clientId, agentId);

        ClientProfile existing = getAuthorizedClient(clientId, agentId);

        clientAccountClient.deleteAllAccounts(clientId, agentId);

        existing.setStatus(ClientProfile.Status.DELETED);
        repository.save(existing);

        // Evict caches
        cacheService.evictAsync(CACHE_CLIENTS, clientId);
        cacheService.evictAsync(CACHE_CLIENTS_BY_AGENT, agentId);

        logPublisher.publishFireAndForget(topicArn, new LogEvent(
                "DELETE", clientId, null, "DELETED", agentId, clientId, existing.getEmail()
        ));

        log.info("Client profile soft deleted successfully: {}", clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientProfile> getClient(String clientId, String agentId) {
        log.info("Retrieving client profile: {} for agent: {}", clientId, agentId);

        ClientProfile cached = cacheService.getWithTimeout(CACHE_CLIENTS, clientId, ClientProfile.class);
        if (cached != null && cached.getAgentId().equals(agentId)) {
            log.debug("Cache hit for client: {}", clientId);
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "READ", clientId, null, null, agentId, clientId, cached.getEmail()
            ));
            return Optional.of(cached);
        }

        Optional<ClientProfile> client = repository.findByClientIdAndAgentId(clientId, agentId);
        client.ifPresent(c -> {
            cacheService.putAsync(CACHE_CLIENTS, clientId, c);
            logPublisher.publishFireAndForget(topicArn, new LogEvent(
                    "READ", clientId, null, null, agentId, clientId, c.getEmail()
            ));
        });

        return client;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientProfile> getAllClientsByAgent(String agentId) {
        log.info("Retrieving all client profiles for agent: {}", agentId);

        List<ClientProfile> cached = cacheService.getWithTimeout(CACHE_CLIENTS_BY_AGENT, agentId, List.class);
        if (cached != null && !cached.isEmpty()) {
            log.debug("Cache hit for agent clients: {}", agentId);
            return cached;
        }

        List<ClientProfile> clients = repository.findAllByAgentId(agentId);
        if (!clients.isEmpty()) {
            cacheService.putAsync(CACHE_CLIENTS_BY_AGENT, agentId, clients);
        }

        return clients;
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String clientId, String agentId) {
        log.info("Sending verification email for client: {} by agent: {}", clientId, agentId);

        ClientProfile profile = getAuthorizedClient(clientId, agentId);

        logPublisher.publishFireAndForget(topicArn, new LogEvent(
                "VERIFY", null, null, profile.getEmail(), agentId, clientId, profile.getEmail()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClientIdAndAgentId(String clientId, String agentId) {
        log.info("Checking if client exists: {} for agent: {}", clientId, agentId);

        return repository.existsVerifiedByClientIdAndAgentId(clientId, agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public String findEmailByClientIdAndAgentId(String clientId, String agentId) {
        log.info("Fetching email for clientId={} agentId={}", clientId, agentId);

        boolean exists = repository.existsVerifiedByClientIdAndAgentId(clientId, agentId);
        if (!exists) {
            throw new IllegalArgumentException("Client does not exist or is not assigned to this agent.");
        }

        Optional<String> emailOpt = repository.findEmailByClientIdAndAgentId(clientId, agentId);

        if (emailOpt.isEmpty() || emailOpt.get().isBlank()) {
            throw new IllegalArgumentException("Client exists but email is missing.");
        }

        return emailOpt.get();
    }


    private ClientProfile getAuthorizedClient(String clientId, String agentId) {
        
        log.info("Fetching authorized client for clientId={} agentId={}", clientId, agentId);

        ClientProfile client = repository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found."));

        if (!client.getAgentId().equals(agentId)) {
            throw new UnauthorizedAgentAccessException("Agent not authorized to access this client.");
        }

        if (client.getStatus().equals(ClientProfile.Status.DELETED)) {
            throw new ClientDeletedException("Client with ID " + clientId + " is no longer active.");
        }

        return client;
    }
}