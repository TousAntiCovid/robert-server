package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.ws.dto.declaration.GenerateDeclarationTokenRequest;

import java.util.Optional;

public interface DeclarationService {

    /**
     * Generate a declaration token (used for CNAM)
     * The custom JTI ensure that two tokens can be consumed with exactly the same exposition context
     * @param request business and technical infos
     * @return the token
     */
    Optional<String> generateDeclarationToken(GenerateDeclarationTokenRequest request);

    /**
     * Generate a simple token (used for Analytics)
     * @return the token
     */
    Optional<String> generateAnalyticsToken();

}
