package fr.gouv.stopc.submission.code.server.ws.service.impl;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.ILongCodeService;
import fr.gouv.stopc.submission.code.server.commun.service.IShortCodeService;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.service.ISubmissionCodeService;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeSimpleDto;
import fr.gouv.stopc.submission.code.server.ws.service.IGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenerateServiceImpl implements IGenerateService {
    private final ILongCodeService longCodeService;
    private final ISubmissionCodeService submissionCodeService;
    private final IShortCodeService shortCodeService;

    /**TargetZoneId is the time zone id (in the java.time.ZoneId way) on which the submission code server should deliver the codes.
     * eg.: for France is "Europe/Paris"
     */
    @Value("${stop.covid.qr.code.targetzone}")
    private String targetZoneId;

    /**
     * Number of try to generate a new code in case of the code is already in db
     * it is set in application.properties file
     */
    @Value("${generation.code.maxattempts}")
    private long numberOfTryInCaseOfError;

    /**
     * Interval in days of the validity of a long code
     * it is set in application.properties file
     */
    @Value("${generation.code.longcode.validity}")
    private long timeValidityLongCode;

    /**
     * Interval in minutes of the validity of a short code
     * it is set in application.properties file
     */
    @Value("${generation.code.shortcode.validity}")
    private long timeValidityShortCode;


    /**
     * Default constructor
     * @param longCodeService Spring-injection of the longCodeService generating long codes
     * @param shortCodeService Spring-injection of the shortCodeService generating short codes
     * @param submissionCodeService Spring-injection of the shortCodeService giving access to persistence in db.
     */
    @Inject
    public GenerateServiceImpl(ILongCodeService longCodeService,
                               IShortCodeService shortCodeService,
                               ISubmissionCodeService submissionCodeService)
    {
        this.shortCodeService = shortCodeService;
        this.longCodeService = longCodeService;
        this.submissionCodeService = submissionCodeService;
    }


    @Override
    public CodeSimpleDto generateShortCode()
            throws SubmissionCodeServerException
    {
        final CodeSimpleDto shortCodeInstance = new CodeSimpleDto();
        new ModelMapper().map(
                this.generateCodeGeneric(1, CodeTypeEnum.SHORT, OffsetDateTime.now(), new Lot()).get(0),
                shortCodeInstance
        );
        return shortCodeInstance;
    }

    @Override
    public List<CodeDetailedDto> generateCodeGeneric(final long size,
                                                     final CodeTypeEnum cte,
                                                     final OffsetDateTime validFrom,
                                                      Lot lotObject)
            throws SubmissionCodeServerException
    {

        final ArrayList<CodeDetailedDto> generateResponseList = new ArrayList<>();

        long failCount = 0;

       // The date available/validFrom is date of now in this time
        OffsetDateTime validGenDate= OffsetDateTime.now();

        log.info("Generating an amount of {} {} codes", size, cte);

        for (int i = 0; generateResponseList.size() < size && failCount <= numberOfTryInCaseOfError; i++) {
            SubmissionCodeDto submissionCodeDto = this.preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(cte, validFrom)
                    .dateGeneration(validGenDate)
                    .used(false)
                    .build();
            try {
                final Optional<SubmissionCode> submissionCodeOptional = this.submissionCodeService.saveCode(submissionCodeDto, lotObject);

                if(!submissionCodeOptional.isPresent()) {
                    throw new SubmissionCodeServerException(SubmissionCodeServerException
                            .ExceptionEnum.DB_SAVE_OPTIONAL_EMPTY
                    );
                }

                final SubmissionCode sc = submissionCodeOptional.get();
                lotObject = sc.getLotkey();
                generateResponseList.add(CodeDetailedDto.builder()
                        .code(sc.getCode())
                        .typeAsString(cte.getType())
                        .typeAsInt(Integer.parseInt(cte.getTypeCode()))
                        .validFrom(sc !=null && sc.getDateAvailable() != null ? formatOffsetDateTime(sc.getDateAvailable()) : "")
                        .dateGenerate(sc != null && sc.getDateGeneration()!= null ? formatOffsetDateTime(sc.getDateGeneration()) : "")
                        .validUntil(sc !=null && sc.getDateEndValidity() != null ? formatOffsetDateTime(sc.getDateEndValidity()) : "")
                        .build()
                );

                failCount = 0;
            } catch (DataIntegrityViolationException divException) {
                failCount++;
                log.error("code generated is not unique try {}/{}", failCount, numberOfTryInCaseOfError + 1);
            }
            // In case of tries of generating code were exceeded an error should be raised.
            if(failCount > numberOfTryInCaseOfError) {
                final String message = String.format(
                        "The code index %s has failed to be generated reaching %s counts of fails",
                        i, failCount
                );
                //delete Lot not completed
                submissionCodeService.removeByLot(lotObject);
                log.error(message);
                throw new SubmissionCodeServerException(
                        SubmissionCodeServerException.ExceptionEnum.CODE_GENERATION_FAILED_ERROR
                );
            }
        }
        return generateResponseList;
    }

    public SubmissionCodeDto.SubmissionCodeDtoBuilder preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(final CodeTypeEnum cte,
                                                                                                             final OffsetDateTime validFrom)
            throws SubmissionCodeServerException
    {
        switch (cte) {
            case LONG:
                return SubmissionCodeDto.builder()
                        .code(this.longCodeService.generateCode())
                        .type(cte.getTypeCode())
                        .dateAvailable(validFrom)
                        .dateEndValidity(this.getExpirationDateForLongCodeStartingFrom(validFrom));
            case SHORT:
                return SubmissionCodeDto.builder()
                        .code(this.shortCodeService.generateCode())
                        .type(cte.getTypeCode())
                        .dateAvailable(validFrom)
                        .dateEndValidity(this.getExpirationDateForShortCodeStartingFrom(validFrom));
            default:
                throw new SubmissionCodeServerException(
                        SubmissionCodeServerException.ExceptionEnum.INVALID_CODE_TYPE_ERROR
                );
        }
    }

    @Override
    public List<OffsetDateTime> getListOfValidDatesFor(int size, OffsetDateTime validFromFirstValue)
    {
        final ArrayList<OffsetDateTime> validFromList = new ArrayList<>();


        // convert to zulu zoneoffset
        validFromList.add(validFromFirstValue.withOffsetSameInstant(ZoneOffset.of("Z")));

        final OffsetDateTime nowInParis = OffsetDateTime.now(ZoneId.of(this.targetZoneId));
        final ZoneOffset offsetInParis = nowInParis.getOffset();

        // assuring validFromFirstValue is OffsetInParis
        validFromFirstValue = validFromFirstValue.withOffsetSameInstant(offsetInParis);

        for (int i = 1; i < size; i++) {
            // OffsetDateTime is immutable so it can be copy
            OffsetDateTime oft = validFromFirstValue;

            // set 00 after day (2020-05-03T23:48:24.830+02:00 -> 2020-05-03T00:00+02:00)
            oft = oft.truncatedTo(ChronoUnit.DAYS);

            // add incremental day  e.g. : 2020-05-03T00:00+02:00 + (i = 2) -> 2020-05-05T00:00+02:00 + 2
            oft = oft.plusDays(i);

            // converting to zulu zone offset
            oft = oft.withOffsetSameInstant(ZoneOffset.of("Z"));

            validFromList.add(oft);
        }
        return validFromList;
    }


    @Override
    public List<CodeDetailedDto> generateLongCodesWithBulkMethod(
            final OffsetDateTime validFrom,
            final long dailyAmount,
            final Lot lot,
            final OffsetDateTime validGenDate
    )   throws SubmissionCodeServerException
    {


        final OffsetDateTime validUntil = getExpirationDateForLongCodeStartingFrom(validFrom);

        final List<SubmissionCodeDto> submissionCodeDtos = this.longCodeService
                .generateCodes(dailyAmount)
                .stream()
                .map(code ->
                        SubmissionCodeDto.builder()
                                .code(code)
                                .type(CodeTypeEnum.LONG.getTypeCode())
                                .dateGeneration(validGenDate)
                                .dateAvailable(validFrom)
                                .dateEndValidity(validUntil)
                                .used(false)
                                .build()
                )
                .collect(Collectors.toList());

        Iterable<SubmissionCode> submissionCodes = null;
        try {
            submissionCodes = this.submissionCodeService
                    .saveAllCodes(submissionCodeDtos, lot);

        }  catch (DataIntegrityViolationException divException) {
            submissionCodeService.removeByLot(lot);
            log.error("code generated error integration");
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.CODE_GENERATION_FAILED_ERROR
            );
    }
        return this.submissionCodeListToCodeDetailDtoList(submissionCodes);
    }

    public List<CodeDetailedDto> submissionCodeListToCodeDetailDtoList(Iterable<SubmissionCode> submissionCodes) {
        if(submissionCodes == null){
            return Collections.emptyList();
        }
        return IterableUtils.toList(submissionCodes).stream()
                .map(sc -> CodeDetailedDto.builder()
                        .code(sc.getCode())
                        .typeAsString(CodeTypeEnum.LONG.getType())
                        .typeAsInt(Integer.parseInt(CodeTypeEnum.LONG.getTypeCode()))
                        .validFrom(sc != null && sc.getDateAvailable() != null ? formatOffsetDateTime(sc.getDateAvailable()) : "")
                        .validUntil(sc != null && sc.getDateAvailable() != null ? formatOffsetDateTime(sc.getDateEndValidity()) : "")
                        .dateGenerate(sc != null && sc.getDateGeneration()!= null ? formatOffsetDateTime(sc.getDateGeneration()) : "")
                        .build()
                )
                .collect(Collectors.toList());
    }

    /**
     * Method formatting date with standard API date pattern "AAAA-MM-ddThh:mm:ssZ"
     * @param date value to be stringified.
     * @return ISO instant formatter of date in parameter
     */
    private String formatOffsetDateTime(OffsetDateTime date)
    {
        return date.format(DateTimeFormatter.ISO_INSTANT);
    }

    /**
     * Method gives the validUntil date of long code from the date given in parameter.
     * It calculates the validity end date of the code using value set in application.properties and inject by Spring. {@link #timeValidityLongCode}
     * The ValidUntil should be hh:mm formatted as 23:59 in paris.
     * @param validFrom the OffsetDateTime start validity applied to calculate end of long code validity
     * @return OffsetDateTime corresponding to the "validFrom" plus days in {@link #timeValidityLongCode} at Zulu Offset
     */
    private OffsetDateTime getExpirationDateForLongCodeStartingFrom(OffsetDateTime validFrom)
    {
        // ensuring that validFrom is based
        validFrom =  validFrom.withOffsetSameInstant(this.getTargetOffset());
        return validFrom
                .withOffsetSameInstant(this.getTargetOffset())
                .plusDays(timeValidityLongCode + 1)
                .truncatedTo(ChronoUnit.DAYS)
                .minusMinutes(1)
                .withOffsetSameInstant(this.getZuluOffset());
    }

    /**
     * @return return current offset of targeted zone where the codes should be used
     */
    private ZoneOffset getTargetOffset()
    {
        return OffsetDateTime.now(ZoneId.of(this.targetZoneId)).getOffset();
    }

    /**
     * @return return current offset of zulu
     */
    private ZoneOffset getZuluOffset()
    {
        return ZoneOffset.of("Z");
    }

    /**
     * Method gives the validUntil date of short code from the date given in parameter.
     * It calculates the validity end date of the code using value set in application.properties and inject by Spring. {@link #timeValidityShortCode}
     * @param validFrom the OffsetDateTime start validity applied to calculate end of short code validity
     * @return OffsetDateTime corresponding to the "validFrom" plus minutes in {@link #timeValidityShortCode} at Zulu Offset
     */
    private OffsetDateTime getExpirationDateForShortCodeStartingFrom(OffsetDateTime validFrom)
    {

        return validFrom
                .plusMinutes(timeValidityShortCode)
                .withOffsetSameInstant(this.getZuluOffset());
    }

}
