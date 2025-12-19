package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.request.UpdateDoctorProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorKeycloakSyncService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm:health-app-realm}")
    private String realm;

    public void updateDoctorInKeycloak(String keycloakUserId, UpdateDoctorProfileRequest request) {

        UserResource userResource = keycloak.realm(realm)
                .users()
                .get(keycloakUserId);

        UserRepresentation user = userResource.toRepresentation();

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            user.setLastName(request.getLastName());

        userResource.update(user);

        log.info("✅ Keycloak user updated: {}", keycloakUserId);
    }
}

