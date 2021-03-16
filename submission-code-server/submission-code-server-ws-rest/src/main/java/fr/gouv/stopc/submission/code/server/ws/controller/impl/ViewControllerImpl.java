package fr.gouv.stopc.submission.code.server.ws.controller.impl;

import fr.gouv.stopc.submission.code.server.ws.controller.IViewController;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.ViewDto;
import fr.gouv.stopc.submission.code.server.ws.service.impl.ViewsServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.vo.ViewVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.inject.Inject;
import javax.validation.Valid;

@Slf4j
@Service
public class ViewControllerImpl implements IViewController {

    private final ViewsServiceImpl viewsService;

    @Inject
    public ViewControllerImpl(ViewsServiceImpl viewsService) {
        this.viewsService = viewsService;
    }

    @Override
    public ResponseEntity<ViewDto.LotInformation> getLotInformation(@PathVariable long lotIdentifier)
            throws SubmissionCodeServerException
    {
        return ResponseEntity.ok(this.viewsService.getLotInformation(lotIdentifier));
    }

    @Override
    public ResponseEntity<ViewDto.CodeValuesForPage> getCodeValuesForPage(
            long lotIdentifier, int page, int elementByPage
    ) throws SubmissionCodeServerException
    {
        return ResponseEntity.ok(this.viewsService.getViewLotCodeDetailListFor(page, elementByPage, lotIdentifier));
    }

    @Override
    public ResponseEntity<ViewDto.CodeGenerationRequest> postCodeGenerationRequest (
            @Valid @RequestBody ViewVo.CodeGenerationRequestBody codeGenerationRequestBody
    ) throws SubmissionCodeServerException
    {
        final ViewDto.CodeGenerationRequest codeGenerationRequest = this.viewsService
                .launchGenerationWith(codeGenerationRequestBody);

        return ResponseEntity.ok(codeGenerationRequest);
    }
}
