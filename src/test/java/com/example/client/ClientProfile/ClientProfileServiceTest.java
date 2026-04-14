package com.example.client.ClientProfile;

import com.example.client.ClientProfile.dto.ClientProfileCreateDTO;
import com.example.client.ClientProfile.dto.ClientProfileUpdateDTO;
import com.example.client.ClientProfile.communication.ClientAccountClient;
import com.example.client.cache.HybridCacheService;
import com.example.client.exception.ClientDeletedException;
import com.example.client.exception.ClientNotFoundException;
import com.example.client.exception.UnauthorizedAgentAccessException;
import com.example.client.logging.LogEvent;
import com.example.client.logging.LogPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientProfileServiceImplTest {

    @Mock
    private ClientProfileRepository repository;

    @Mock
    private LogPublisher logPublisher;

    @Mock
    private ClientAccountClient clientAccountClient;

    @Mock
    private HybridCacheService cacheService;

    @InjectMocks
    private ClientProfileServiceImpl service;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    private static final String AGENT_ID = "AGENT-100";
    private static final String CLIENT_ID = "CLI-ABC12345";
    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:ClientLogsTopic";

    @BeforeEach
    void setUp() {
        service = new ClientProfileServiceImpl(repository, logPublisher, clientAccountClient, cacheService);
        ReflectionTestUtils.setField(service, "topicArn", TOPIC_ARN);
    }

    // ===================== CREATE CLIENT TESTS =====================

    @Test
    void createClient_Success() {
        // Arrange
        ClientProfileCreateDTO dto = createValidCreateDTO();
        when(repository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientProfile result = service.createClient(dto, AGENT_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getClientId());
        assertTrue(result.getClientId().startsWith("CLI-"));
        assertEquals(dto.getFirstName(), result.getFirstName());
        assertEquals(dto.getLastName(), result.getLastName());
        assertEquals(dto.getEmail(), result.getEmail());
        assertEquals(AGENT_ID, result.getAgentId());
        assertEquals(ClientProfile.Status.PENDING, result.getStatus());

        // Verify repository save
        verify(repository).save(any(ClientProfile.class));
        
        // Verify cache operations
        verify(cacheService).putAsync(eq("clientProfiles"), eq(result.getClientId()), eq(result));
        verify(cacheService).evictAsync(eq("clientProfilesByAgent"), eq(AGENT_ID));
        
        // Verify log event with email
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals("CREATE", logEvent.getCrud());
        assertEquals(dto.getEmail(), logEvent.getEmail());
    }

    // ===================== UPDATE CLIENT TESTS =====================

    @Test
    void updateClient_Success_UpdatesAllFields() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        ClientProfileUpdateDTO dto = createValidUpdateDTO();
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(repository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(false);
        when(repository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientProfile updated = service.updateClient(CLIENT_ID, dto, AGENT_ID);

        // Assert
        assertEquals(dto.getFirstName(), updated.getFirstName());
        assertEquals(dto.getLastName(), updated.getLastName());
        assertEquals(dto.getEmail(), updated.getEmail());
        assertEquals(dto.getPhoneNumber(), updated.getPhoneNumber());
        assertEquals(dto.getAddress(), updated.getAddress());
        assertEquals(dto.getCity(), updated.getCity());
        assertEquals(dto.getState(), updated.getState());
        assertEquals(dto.getCountry(), updated.getCountry());
        assertEquals(dto.getPostalCode(), updated.getPostalCode());

        verify(repository).save(existing);
        verify(cacheService).putAsync(eq("clientProfiles"), eq(CLIENT_ID), eq(updated));
        verify(cacheService).evictAsync(eq("clientProfilesByAgent"), eq(AGENT_ID));
        
        // Should have logged multiple updates (one per field)
        verify(logPublisher, atLeast(10)).publishFireAndForget(eq(TOPIC_ARN), any(LogEvent.class));
    }

    @Test
    void updateClient_EmailAlreadyExists_ThrowsException() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        dto.setEmail("newemail@example.com");
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByEmail(dto.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateClient(CLIENT_ID, dto, AGENT_ID));
        
        assertEquals("Email already exists in the system", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateClient_PhoneNumberAlreadyExists_ThrowsException() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        dto.setPhoneNumber("+9999999999");
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateClient(CLIENT_ID, dto, AGENT_ID));
        
        assertEquals("Phone number already exists in the system", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateClient_UnauthorizedAgent_ThrowsException() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        existing.setAgentId("DIFFERENT-AGENT");
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(UnauthorizedAgentAccessException.class,
                () -> service.updateClient(CLIENT_ID, dto, AGENT_ID));
    }

    @Test
    void updateClient_DeletedClient_ThrowsException() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        existing.setStatus(ClientProfile.Status.DELETED);
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(ClientDeletedException.class,
                () -> service.updateClient(CLIENT_ID, dto, AGENT_ID));
    }

    @Test
    void updateClient_ClientNotFound_ThrowsException() {
        // Arrange
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ClientNotFoundException.class,
                () -> service.updateClient(CLIENT_ID, new ClientProfileUpdateDTO(), AGENT_ID));
    }

    @Test
    void updateClient_OnlyUpdatesChangedFields() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        dto.setFirstName("NewName"); // Only update first name
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientProfile updated = service.updateClient(CLIENT_ID, dto, AGENT_ID);

        // Assert
        assertEquals("NewName", updated.getFirstName());
        assertEquals(existing.getLastName(), updated.getLastName()); // Unchanged
        
        // Only one log event for firstName
        verify(logPublisher, times(1)).publishFireAndForget(eq(TOPIC_ARN), any(LogEvent.class));
    }

    // ===================== PATCH STATUS TESTS =====================

    @Test
    void patchClientStatus_Success() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        ClientProfile.Status newStatus = ClientProfile.Status.VERIFIED;
        
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ClientProfile updated = service.patchClientStatus(CLIENT_ID, newStatus, AGENT_ID);

        // Assert
        assertEquals(newStatus, updated.getStatus());
        verify(repository).save(existing);
        verify(cacheService).putAsync(eq("clientProfiles"), eq(CLIENT_ID), eq(updated));
        verify(cacheService).evictAsync(eq("clientProfilesByAgent"), eq(AGENT_ID));
        
        // Verify log event
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals("UPDATE", logEvent.getCrud());
        assertEquals("status", logEvent.getAttribute());
        assertEquals(existing.getEmail(), logEvent.getEmail());
    }

    // ===================== DELETE CLIENT TESTS =====================

    @Test
    void deleteClient_Success() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.deleteClient(CLIENT_ID, AGENT_ID);

        // Assert
        assertEquals(ClientProfile.Status.DELETED, existing.getStatus());
        verify(clientAccountClient).deleteAllAccounts(CLIENT_ID, AGENT_ID);
        verify(repository).save(existing);
        verify(cacheService).evictAsync(eq("clientProfiles"), eq(CLIENT_ID));
        verify(cacheService).evictAsync(eq("clientProfilesByAgent"), eq(AGENT_ID));
        
        // Verify log event with email
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals("DELETE", logEvent.getCrud());
        assertEquals(existing.getEmail(), logEvent.getEmail());
    }

    // ===================== GET CLIENT TESTS =====================

    @Test
    void getClient_FromCache_Success() {
        // Arrange
        ClientProfile cached = createExistingClientProfile();
        when(cacheService.getWithTimeout(eq("clientProfiles"), eq(CLIENT_ID), eq(ClientProfile.class)))
                .thenReturn(cached);

        // Act
        Optional<ClientProfile> result = service.getClient(CLIENT_ID, AGENT_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(cached, result.get());
        
        // Verify log event with email
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals("READ", logEvent.getCrud());
        assertEquals(cached.getEmail(), logEvent.getEmail());
        
        verify(repository, never()).findByClientIdAndAgentId(anyString(), anyString());
    }

    @Test
    void getClient_FromRepository_WhenCacheMiss() {
        // Arrange
        when(cacheService.getWithTimeout(eq("clientProfiles"), eq(CLIENT_ID), eq(ClientProfile.class)))
                .thenReturn(null);
        ClientProfile existing = createExistingClientProfile();
        when(repository.findByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.of(existing));

        // Act
        Optional<ClientProfile> result = service.getClient(CLIENT_ID, AGENT_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existing, result.get());
        verify(cacheService).putAsync(eq("clientProfiles"), eq(CLIENT_ID), eq(existing));
        
        // Verify log event with email
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals(existing.getEmail(), logEvent.getEmail());
    }

    @Test
    void getClient_NotFound_ReturnsEmpty() {
        // Arrange
        when(cacheService.getWithTimeout(eq("clientProfiles"), eq(CLIENT_ID), eq(ClientProfile.class)))
                .thenReturn(null);
        when(repository.findByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.empty());

        // Act
        Optional<ClientProfile> result = service.getClient(CLIENT_ID, AGENT_ID);

        // Assert
        assertFalse(result.isPresent());
        verify(logPublisher, never()).publishFireAndForget(anyString(), any());
    }

    @Test
    void getClient_CacheHitButWrongAgent_FetchesFromRepository() {
        // Arrange
        ClientProfile cachedForDifferentAgent = createExistingClientProfile();
        cachedForDifferentAgent.setAgentId("DIFFERENT-AGENT");
        
        when(cacheService.getWithTimeout(eq("clientProfiles"), eq(CLIENT_ID), eq(ClientProfile.class)))
                .thenReturn(cachedForDifferentAgent);
        
        ClientProfile correctClient = createExistingClientProfile();
        when(repository.findByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.of(correctClient));

        // Act
        Optional<ClientProfile> result = service.getClient(CLIENT_ID, AGENT_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(correctClient, result.get());
        verify(repository).findByClientIdAndAgentId(CLIENT_ID, AGENT_ID);
    }

    // ===================== GET ALL CLIENTS TESTS =====================

    @Test
    void getAllClientsByAgent_FromCache_Success() {
        // Arrange
        List<ClientProfile> cached = Arrays.asList(
                createExistingClientProfile(),
                createExistingClientProfile()
        );
        when(cacheService.getWithTimeout(eq("clientProfilesByAgent"), eq(AGENT_ID), eq(List.class)))
                .thenReturn(cached);

        // Act
        List<ClientProfile> result = service.getAllClientsByAgent(AGENT_ID);

        // Assert
        assertEquals(2, result.size());
        verify(repository, never()).findAllByAgentId(anyString());
    }

    @Test
    void getAllClientsByAgent_FromRepository_WhenCacheMiss() {
        // Arrange
        when(cacheService.getWithTimeout(eq("clientProfilesByAgent"), eq(AGENT_ID), eq(List.class)))
                .thenReturn(null);
        List<ClientProfile> clients = Arrays.asList(
                createExistingClientProfile(),
                createExistingClientProfile()
        );
        when(repository.findAllByAgentId(AGENT_ID)).thenReturn(clients);

        // Act
        List<ClientProfile> result = service.getAllClientsByAgent(AGENT_ID);

        // Assert
        assertEquals(2, result.size());
        verify(cacheService).putAsync(eq("clientProfilesByAgent"), eq(AGENT_ID), eq(clients));
    }

    @Test
    void getAllClientsByAgent_EmptyList_DoesNotCache() {
        // Arrange
        when(cacheService.getWithTimeout(eq("clientProfilesByAgent"), eq(AGENT_ID), eq(List.class)))
                .thenReturn(null);
        when(repository.findAllByAgentId(AGENT_ID)).thenReturn(Collections.emptyList());

        // Act
        List<ClientProfile> result = service.getAllClientsByAgent(AGENT_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(cacheService, never()).putAsync(anyString(), anyString(), any());
    }

    // ===================== SEND VERIFICATION EMAIL TESTS =====================

    @Test
    void sendVerificationEmail_Success() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        when(repository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));

        // Act
        service.sendVerificationEmail(CLIENT_ID, AGENT_ID);

        // Assert
        verify(logPublisher).publishFireAndForget(eq(TOPIC_ARN), logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();
        assertEquals("VERIFY", logEvent.getCrud());
        assertEquals(existing.getEmail(), logEvent.getAfterValue());
        assertEquals(existing.getEmail(), logEvent.getEmail());
    }


    // ===================== EXISTS TESTS =====================

    @Test
    void existsByClientIdAndAgentId_True() {
        // Arrange
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(true);

        // Act
        boolean result = service.existsByClientIdAndAgentId(CLIENT_ID, AGENT_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByClientIdAndAgentId_False() {
        // Arrange
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(false);

        // Act
        boolean result = service.existsByClientIdAndAgentId(CLIENT_ID, AGENT_ID);

        // Assert
        assertFalse(result);
    }

    // ===================== FIND EMAIL TESTS =====================

    @Test
    void findEmailByClientIdAndAgentId_Success() {
        // Arrange
        ClientProfile existing = createExistingClientProfile();
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(true);
        when(repository.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.of(existing.getEmail()));

        // Act
        String email = service.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID);

        // Assert
        assertEquals(existing.getEmail(), email);
    }

    @Test
    void findEmailByClientIdAndAgentId_ClientNotVerified_ThrowsException() {
        // Arrange
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID));
        
        assertEquals("Client does not exist or is not assigned to this agent.", exception.getMessage());
    }

    @Test
    void findEmailByClientIdAndAgentId_EmailMissing_ThrowsException() {
        // Arrange
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(true);
        when(repository.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID));
        
        assertEquals("Client exists but email is missing.", exception.getMessage());
    }

    @Test
    void findEmailByClientIdAndAgentId_EmailBlank_ThrowsException() {
        // Arrange
        when(repository.existsVerifiedByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(true);
        when(repository.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID))
                .thenReturn(Optional.of("   "));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findEmailByClientIdAndAgentId(CLIENT_ID, AGENT_ID));
        
        assertEquals("Client exists but email is missing.", exception.getMessage());
    }

    // ===================== HELPER METHODS =====================

    private ClientProfile createExistingClientProfile() {
        return ClientProfile.builder()
                .clientId(CLIENT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .address("123 Main St")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .agentId(AGENT_ID)
                .status(ClientProfile.Status.PENDING)
                .build();
    }

    private ClientProfileCreateDTO createValidCreateDTO() {
        ClientProfileCreateDTO dto = new ClientProfileCreateDTO();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setGender("Male");
        dto.setEmail("john.doe@example.com");
        dto.setPhoneNumber("+1234567890");
        dto.setAddress("123 Main St");
        dto.setCity("New York");
        dto.setState("NY");
        dto.setCountry("USA");
        dto.setPostalCode("10001");
        return dto;
    }

    private ClientProfileUpdateDTO createValidUpdateDTO() {
        ClientProfileUpdateDTO dto = new ClientProfileUpdateDTO();
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setDateOfBirth(LocalDate.of(1992, 5, 15));
        dto.setGender("Female");
        dto.setEmail("jane.smith@example.com");
        dto.setPhoneNumber("+0987654321");
        dto.setAddress("456 Oak Ave");
        dto.setCity("Los Angeles");
        dto.setState("CA");
        dto.setCountry("USA");
        dto.setPostalCode("90001");
        return dto;
    }
}