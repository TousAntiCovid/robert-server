package fr.gouv.stopc.submission.code.server.commun.service;

import java.util.List;

public interface ILongCodeService {

    /**
     * generate and stringify a long code
     * @return  A randomly generated long code
     */
     String generateCode();

    /**
     *
     * @param size number of code to be generated
     * @return
     */
     List<String> generateCodes(long size);

}
