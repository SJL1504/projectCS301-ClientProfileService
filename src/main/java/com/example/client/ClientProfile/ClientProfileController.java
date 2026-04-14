package com.example.client.ClientProfile;

import com.example.client.ClientProfile.dto.ClientProfileCreateDTO;
import com.example.client.ClientProfile.dto.ClientProfileUpdateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientProfileController {

    private final ClientProfileService service;

    public ClientProfileController(ClientProfileService service) {
        this.service = service;
    }

    // Create
    @PostMapping("/")
    public ResponseEntity<ClientProfile> createClient(
            @RequestBody ClientProfileCreateDTO clientProfileCreateDTO,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        ClientProfile created = service.createClient(clientProfileCreateDTO, agentId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Full Update (PUT)
    @PutMapping("/{clientId}")
    public ResponseEntity<ClientProfile> updateClient(
            @PathVariable String clientId,
            @RequestBody ClientProfileUpdateDTO clientProfileUpdateDTO,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        ClientProfile updated = service.updateClient(clientId, clientProfileUpdateDTO, agentId);
        return ResponseEntity.ok(updated);
    }

    // Patch status only 
    @PatchMapping("/{clientId}/status")
    public ResponseEntity<ClientProfile> patchClientStatus(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId,
            @RequestParam ClientProfile.Status status
    ) {
        ClientProfile updated = service.patchClientStatus(clientId, status, agentId);
        return ResponseEntity.ok(updated);
    }

    // Soft Delete
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        service.deleteClient(clientId, agentId);
        return ResponseEntity.noContent().build();
    }

    // Get single client
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientProfile> getClient(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        return service.getClient(clientId, agentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get all clients for an agent
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<ClientProfile>> getAllClientsByAgent(@RequestHeader(value = "X-User-Id", required = false) String agentId) {
        List<ClientProfile> clients = service.getAllClientsByAgent(agentId);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{clientId}/exists")
    public ResponseEntity<Boolean> checkClientExists(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        boolean exists = service.existsByClientIdAndAgentId(clientId, agentId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{clientId}/email")
    public ResponseEntity<String> getClientEmail(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId) {

        String email = service.findEmailByClientIdAndAgentId(clientId, agentId);
        return ResponseEntity.ok(email);
    }

    @PostMapping("/{clientId}/verify")
    public ResponseEntity<Void> sendVerificationEmail(
            @PathVariable String clientId,
            @RequestHeader(value = "X-User-Id", required = false) String agentId
    ) {
        service.sendVerificationEmail(clientId, agentId);
        return ResponseEntity.ok().build();
    }

}
