package com.example.client.ClientProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, String> {
    
    Optional<ClientProfile> findByClientIdAndAgentId(String clientId, String agentId);

    Optional<ClientProfile> findByClientId(String clientId);
    
    List<ClientProfile> findAllByAgentId(String agentId);
    
    Optional<ClientProfile> findByEmail(String email);
    
    Optional<ClientProfile> findByPhoneNumber(String phoneNumber);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByClientIdAndAgentIdAndStatus(String clientId, String agentId, ClientProfile.Status status);

    default boolean existsVerifiedByClientIdAndAgentId(String clientId, String agentId) {
        return existsByClientIdAndAgentIdAndStatus(clientId, agentId, ClientProfile.Status.VERIFIED);
    }

    @Query("SELECT c.email FROM ClientProfile c WHERE c.clientId = :clientId AND c.agentId = agentId")
    Optional<String> findEmailByClientIdAndAgentId(String clientId, String agentId);

}