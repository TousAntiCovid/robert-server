package fr.gouv.stopc.submission.code.server.database.service.impl;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.repository.SubmissionCodeRepository;
import fr.gouv.stopc.submission.code.server.database.service.ISubmissionCodeService;
import org.apache.logging.log4j.util.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Valid
public class SubmissionCodeServiceImpl implements ISubmissionCodeService {

    private SubmissionCodeRepository submissionCodeRepository;


    @Value("${generation.code.security.shortcode.hours}")
    private Integer securityTimeBetweenTwoUsagesOfShortCode;

    @Inject
    public SubmissionCodeServiceImpl(SubmissionCodeRepository submissionCodeRepository){
        this.submissionCodeRepository = submissionCodeRepository;
    }

    @Override
    public Optional<SubmissionCodeDto> getCodeValidity(String code, CodeTypeEnum type) {
        if(Strings.isBlank(code)){
            return Optional.empty();
        }
        String typeCode = type.getTypeCode();
        SubmissionCode submissionCode = submissionCodeRepository.findByCodeAndType(code,typeCode);
        if(Objects.isNull(submissionCode)){
            return Optional.empty();
        }
        ModelMapper modelMapper = new ModelMapper();
        SubmissionCodeDto submissionCodeDto = modelMapper.map(submissionCode, SubmissionCodeDto.class);
        return Optional.of(submissionCodeDto);
    }

    @Override
    public Iterable<SubmissionCode> saveAllCodes(List<SubmissionCodeDto> submissionCodeDtos) {
        return this.saveAllCodes(submissionCodeDtos, new Lot());
    }

    @Override
    public Iterable<SubmissionCode> saveAllCodes(List<SubmissionCodeDto> submissionCodeDtos, Lot lot) {
        if(submissionCodeDtos.isEmpty()) {
            return Collections.emptyList();
        }
        ModelMapper modelMapper = new ModelMapper();
        List<SubmissionCode> submissionCodes = submissionCodeDtos.stream()
                .map(tmp -> {
                    final SubmissionCode sc = modelMapper.map(tmp, SubmissionCode.class);
                    sc.setLotkey(lot);
                    return sc;
                })
                .collect(Collectors.toList());
        return submissionCodeRepository.saveAll(submissionCodes);
    }

    @Override
    public Optional<SubmissionCode> saveCode(SubmissionCodeDto submissionCodeDto) {
        if (Objects.isNull(submissionCodeDto)) {
            return Optional.empty();
        }
        ModelMapper modelMapper = new ModelMapper();
        SubmissionCode submissionCode = modelMapper.map(submissionCodeDto, SubmissionCode.class);
        return Optional.of(submissionCodeRepository.save(submissionCode));
    }

    @Override
    public Optional<SubmissionCode> saveCode(SubmissionCodeDto submissionCodeDto, Lot lot) {
        if (Objects.isNull(submissionCodeDto)) {
            return Optional.empty();
        }
        ModelMapper modelMapper = new ModelMapper();

        SubmissionCode submissionCodeToSave = modelMapper.map(submissionCodeDto, SubmissionCode.class);
        submissionCodeToSave.setLotkey(lot);


        try {
            // try to save data
            return Optional.of(submissionCodeRepository.save(submissionCodeToSave));

        } catch (DataIntegrityViolationException divExcetion) {

            // if Unique code exists for short code try to update
            if(securityTimeBetweenTwoUsagesOfShortCode != null
                    && CodeTypeEnum.SHORT.isTypeOrTypeCodeOf(submissionCodeToSave.getType()))
            {
                SubmissionCode sc = this.submissionCodeRepository.findByCodeAndTypeAndAndDateEndValidityLessThan(
                        submissionCodeToSave.getCode(),
                        submissionCodeToSave.getType(),
                        submissionCodeToSave.getDateAvailable()
                                .minusHours(
                                        securityTimeBetweenTwoUsagesOfShortCode
                                )
                );
                if(sc != null) {
                    // replace actual line by new code
                    submissionCodeToSave.setId(sc.getId());
                    return Optional.of(this.submissionCodeRepository.save(submissionCodeToSave));
                }
            }
            // if update is not made throw the original exception
            throw divExcetion;
        }


    }

    @Override
    public boolean updateCodeUsed(SubmissionCodeDto submissionCodeDto) {
        String code = submissionCodeDto.getCode();
        String type = submissionCodeDto.getType();
        SubmissionCode submissionCode = submissionCodeRepository.findByCodeAndType(code, type);
        if (Objects.isNull(submissionCode)){
            return false;
        }
        submissionCode.setDateUse(submissionCodeDto.getDateUse());
        submissionCode.setUsed(true);
        submissionCodeRepository.save(submissionCode);
        return true;
    }

    @Override
    public long getNumberOfCodesForLotIdentifier(long lotIdentifier) {
        return this.submissionCodeRepository.countSubmissionCodeByLotkeyId(lotIdentifier);
    }

    @Override
    public Page<SubmissionCode> getSubmissionCodesFor(long lotIdentifier, int page, int elementsByPage) {
        return this.submissionCodeRepository
                .findAllByLotkeyId(lotIdentifier, PageRequest.of(page, elementsByPage));
    }

    @Override
    public void removeByLot(Lot lot) {
        this.submissionCodeRepository.deleteAllByLotkey(lot);
    }

}
