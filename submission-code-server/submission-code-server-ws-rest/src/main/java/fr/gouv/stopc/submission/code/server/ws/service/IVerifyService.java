package fr.gouv.stopc.submission.code.server.ws.service;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;

/**
 * Bean service used to proceed data from "/verify" endpoint.
 */
public interface IVerifyService {

    /**
     * Method should be used to know if the code in parameter has been used or if it is still valid.
     * @param code The code value to verify
     * @param type The type of the provided code (see CodeTypeEnum)
     * @return return the validity of the code.
     */
     boolean verifyCode(String code, String type) throws SubmissionCodeServerException;
}
