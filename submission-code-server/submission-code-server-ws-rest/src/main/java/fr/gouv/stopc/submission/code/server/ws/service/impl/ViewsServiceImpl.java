package fr.gouv.stopc.submission.code.server.ws.service.impl;


import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.service.ISubmissionCodeService;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.ViewDto;
import fr.gouv.stopc.submission.code.server.ws.service.IFileService;
import fr.gouv.stopc.submission.code.server.ws.service.IViewService;
import fr.gouv.stopc.submission.code.server.ws.vo.ViewVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ViewsServiceImpl implements IViewService {

    private final ISubmissionCodeService submissionCodeService;
    private final IFileService fileExportService;

    /**
     * Default constructor
     * @param submissionCodeService Spring-injection of the shortCodeService giving access to persistence in db.
     */
    @Inject
    public ViewsServiceImpl(ISubmissionCodeService submissionCodeService,
                            IFileService fileExportService)
    {
        this.submissionCodeService = submissionCodeService;
        this.fileExportService = fileExportService;
    }

    public ViewDto.LotInformation getLotInformation(long lotIdentifier) throws SubmissionCodeServerException {
        final Long numOfCodes = this.submissionCodeService
                .getNumberOfCodesForLotIdentifier(lotIdentifier);

        if(numOfCodes == null) {
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.DB_NO_RECORD_FOR_LOT_IDENTIFIER_ERROR
            );
        }

        return ViewDto.LotInformation.builder()
                .lotIdentifier(lotIdentifier)
                .numberOfCodes(numOfCodes)
                .build();
    }

    public ViewDto.CodeValuesForPage getViewLotCodeDetailListFor(
            int page,
            int elementByPage,
            long lotIdentifier
    )
            throws SubmissionCodeServerException
    {
        final Page<SubmissionCode> submissionCodesPage = this.submissionCodeService.getSubmissionCodesFor(
                lotIdentifier,
                page - 1,
                elementByPage
        );

        if(submissionCodesPage == null ||
                submissionCodesPage.getTotalElements() < 1 ||
                submissionCodesPage.getTotalPages() < 1)
        {
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.DB_INVALID_PARAMETERS_ERROR
            );
        }

        return ViewDto.CodeValuesForPage.builder()
                .actualPage(submissionCodesPage.getNumber() + 1)
                .lastPage(submissionCodesPage.getTotalPages())
                .maxByPage(submissionCodesPage.getNumberOfElements())
                .lot(lotIdentifier)
                .codes(
                        submissionCodesPage.toList().stream()
                                .map(sc -> ViewDto.CodeDetail.builder()
                                        .code(sc.getCode())
                                        .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }

    public ViewDto.CodeGenerationRequest launchGenerationWith(
            ViewVo.CodeGenerationRequestBody codeGenerationRequestBody
    ) throws SubmissionCodeServerException
    {

        @NotNull final long codePerDay = codeGenerationRequestBody.getDailyAmount();
        @NotNull final OffsetDateTime from = codeGenerationRequestBody.getFrom();
        @NotNull OffsetDateTime to = codeGenerationRequestBody.getTo();

        this.fileExportService.zipExportAsync(
                codePerDay,
                new Lot(),
                from.toString(),
                to.toString()
        );

        return ViewDto.CodeGenerationRequest.builder()
                .isSubmitted(true)
                .message("data have been successfully saved !")
                .build();
    }
}
