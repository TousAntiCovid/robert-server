package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.ws.dto.declaration.GenerateDeclarationTokenRequest;

import java.util.Optional;

public interface DeclarationService {

    Optional<String> generateDeclarationToken(GenerateDeclarationTokenRequest request);
}
