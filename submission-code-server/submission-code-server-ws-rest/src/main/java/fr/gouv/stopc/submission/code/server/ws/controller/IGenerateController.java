package fr.gouv.stopc.submission.code.server.ws.controller;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeSimpleDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.Produces;

/**
 * VPN Control is made to access to this end-point.
 * JWT or ApiKey is checked in API Gateway.
 */
@RestController
@RequestMapping(value = "${controller.path.prefix}")
@Produces(MediaType.APPLICATION_JSON_VALUE)
public interface IGenerateController {

    /**
     * Generate a new submission code. Codes are one-time use and have a validity date
     * @return
     */
    @GetMapping(value = "/generate/short")
    ResponseEntity<CodeSimpleDto> generateShortCode() throws SubmissionCodeServerException;

}
