package fr.gouv.stopc.submission.code.server.ws.controller.impl;


import fr.gouv.stopc.submission.code.server.ws.controller.IVerifyController;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.VerifyDto;
import fr.gouv.stopc.submission.code.server.ws.service.impl.VerifyServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.vo.VerifyRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.inject.Inject;
import javax.validation.Valid;


@Service
@Slf4j
public class VerifyControllerImpl implements IVerifyController {

    private VerifyServiceImpl verifyServiceImpl;

    @Inject
    public VerifyControllerImpl(VerifyServiceImpl verifyServiceImpl) {
        this.verifyServiceImpl = verifyServiceImpl;
    }

    @Override
    public ResponseEntity verifySubmissionCode(@ModelAttribute @Valid VerifyRequestVo verifyRequestVo) throws SubmissionCodeServerException {
        log.info("Receiving code : {} and type : {}", verifyRequestVo.getCode(), verifyRequestVo.getType());
        String type = verifyRequestVo.getType();
        String code = verifyRequestVo.getCode();
        boolean result = verifyServiceImpl.verifyCode(code, type);

        return ResponseEntity.ok(VerifyDto.builder().valid(result).build());
    }

}
