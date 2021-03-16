package fr.gouv.stopc.submission.code.server.ws.service;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.ViewDto;
import fr.gouv.stopc.submission.code.server.ws.vo.ViewVo;

public interface IViewService {

    /**
     * The method returns the information as number of codes .
     * @param lotIdentifier
     * @return
     * @throws SubmissionCodeServerException
     */
    ViewDto.LotInformation getLotInformation(long lotIdentifier) throws SubmissionCodeServerException;

    /**
     * The method returns the information as number page, as number elements for page and list of codes.
     * @param page
     * @param elementByPage
     * @param lotIdentifier
     * @return
     * @throws SubmissionCodeServerException
     */
    ViewDto.CodeValuesForPage getViewLotCodeDetailListFor(int page, int elementByPage, long lotIdentifier) throws SubmissionCodeServerException;

    /**
     * The method launch the generation code with the dates from and to and number of codes for day.
     * @return ViewDto.CodeGenerationRequest defining if the request has been successfully or not successfully submitted.
     */
    ViewDto.CodeGenerationRequest launchGenerationWith(ViewVo.CodeGenerationRequestBody codeGenerationRequestBody) throws SubmissionCodeServerException;
}
