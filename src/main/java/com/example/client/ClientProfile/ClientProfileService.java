package com.example.client.ClientProfile;

import com.example.client.ClientProfile.dto.ClientProfileCreateDTO;
import com.example.client.ClientProfile.dto.ClientProfileUpdateDTO;

import java.util.List;
import java.util.Optional;

public interface ClientProfileService {
    
    /**
     * Create a new client profile
     */
    ClientProfile createClient(ClientProfileCreateDTO dto, String agentId);
    
    /**
     * Update a client profile (PUT - full update)
     */
    ClientProfile updateClient(String clientId, ClientProfileUpdateDTO dto, String agentId);
    
    /**
     * Patch client status only
     */
    ClientProfile patchClientStatus(String clientId, ClientProfile.Status status, String agentId);
    
    /**
     * Soft delete a client profile (sets status to INACTIVE)
     */
    void deleteClient(String clientId, String agentId);
    
    /**
     * Get a specific client profile
     */
    Optional<ClientProfile> getClient(String clientId, String agentId);
    
    /**
     * Get all client profiles for an agent
     */
    List<ClientProfile> getAllClientsByAgent(String agentId);
    
    /**
     * Send verification email to client via Amazon SES
     */
    void sendVerificationEmail(String clientId, String agentId);


    boolean existsByClientIdAndAgentId(String clientId, String agentId);


    String findEmailByClientIdAndAgentId(String clientId, String agentId);
}
