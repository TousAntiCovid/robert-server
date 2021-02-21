package fr.gouv.stopc.submission.code.server.ws.service;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;

import java.io.ByteArrayOutputStream;

public interface ISFTPService {
     void transferFileSFTP(ByteArrayOutputStream file) throws SubmissionCodeServerException;
}
