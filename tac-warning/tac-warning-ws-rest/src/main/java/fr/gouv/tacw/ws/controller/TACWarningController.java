package fr.gouv.tacw.ws.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseV1Dto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.BadArgumentsLoggerService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "${controller.path.prefix}")
public class TACWarningController {
    public static final String REPORT_LOG_MESSAGE = "Reporting %d visits while infected";

    public static final String STATUS_LOG_MESSAGE = "Exposure status request for %d visits";

	public static final String MAX_VISITS_FILTER_LOG_MESSAGE = "Filtered out %d visits of %d (max visits reached) while Exposure Status Request";

    public static final String MALFORMED_VISIT_LOG_MESSAGE = "Filtered out %d malformed visits of %d while Exposure Status Request";

    private AuthorizationService authorizationService;

	private WarningService warningService;
	
	private TacWarningWsRestConfiguration configuration;

	private TokenMapper tokenMapper;
	
    private ObjectMapper objectMapper;

    private Validator validator;

    private BadArgumentsLoggerService badArgumentsLoggerService;
	
	public TACWarningController(AuthorizationService authorizationService, WarningService warningService,
            TacWarningWsRestConfiguration configuration, TokenMapper tokenMapper, 
            ObjectMapper objectMapper, Validator validator,
            BadArgumentsLoggerService badArgumentsLoggerService) {
        super();
        this.authorizationService = authorizationService;
        this.warningService = warningService;
        this.configuration = configuration;
        this.tokenMapper = tokenMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.badArgumentsLoggerService = badArgumentsLoggerService;
    }

    @PostMapping(path = UriConstants.API_V1 + UriConstants.STATUS)
    protected ExposureStatusResponseV1Dto getStatusV1(
            @Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo, WebRequest webRequest) {
        ExposureStatusResponseDto esr = this.getStatus(statusRequestVo, webRequest);
        return new ExposureStatusResponseV1Dto(esr.getRiskLevel() != RiskLevel.NONE);
    }
    
    @PostMapping(path = UriConstants.API_V2 + UriConstants.STATUS)
	protected ExposureStatusResponseDto getStatus(
			@Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo, WebRequest webRequest) {
		int nbVisitTokens = statusRequestVo.getVisitTokens().size();
        log.info(String.format(STATUS_LOG_MESSAGE, nbVisitTokens));
		List<VisitTokenVo> visits = getValidVisits(statusRequestVo.getVisitTokens(), webRequest, "VisitTokens");
		int nbRejectedVisitTokens = visits.size() - this.configuration.getMaxVisits();
		if (nbRejectedVisitTokens > 0) {
			log.info(String.format(MAX_VISITS_FILTER_LOG_MESSAGE, nbRejectedVisitTokens, nbVisitTokens));
		}
		Stream<OpaqueVisit> tokens = visits.stream()
				.limit(this.configuration.getMaxVisits())
				.map(tokenVo -> this.tokenMapper.getToken(tokenVo));
		ScoreResult score = this.warningService.getStatus(tokens);
        return new ExposureStatusResponseDto(score.getRiskLevel(), Long.toString(score.getLastContactDate()));
    }

    @PostMapping(path = {UriConstants.API_V1 + UriConstants.REPORT, UriConstants.API_V2 + UriConstants.REPORT})
	protected ReportResponseDto reportVisits(@Valid @RequestBody(required = true) ReportRequestVo reportRequestVo,
			@RequestHeader("Authorization") String jwtToken, WebRequest webRequest) {
		this.authorizationService.checkAuthorization(jwtToken);
		log.info(String.format(REPORT_LOG_MESSAGE, reportRequestVo.getVisits().size()));
		List<VisitVo> visits = getValidVisits(reportRequestVo.getVisits(), webRequest, "Visits");
		this.warningService.reportVisitsWhenInfected(visits);
		return new ReportResponseDto(true, "Report successful");
	}

    protected <T> List<T> getValidVisits(List<T> visits, WebRequest webRequest, String objectName) {
        List<T> validVisits = visits.stream()
                    .filter(visit -> {
                        Set<ConstraintViolation<T>> violations = validator.validate(visit);
                        if ( !violations.isEmpty() ) {
                            this.badArgumentsLoggerService.logValidationErrorMessage(violations, webRequest);
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
        int nbVisits = visits.size();
        int nbFilteredVisits = nbVisits - validVisits.size();
        if (nbFilteredVisits > 0) {
            log.info(String.format(MALFORMED_VISIT_LOG_MESSAGE, nbFilteredVisits, nbVisits));
        }
        return validVisits;
    }

    @PostConstruct
	private void init() {
	    // Avoid exception at Json deserialization.
	    objectMapper.addHandler(new DeserializationProblemHandler() {
	        @Override
	        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) throws IOException {
	            return null;
	        }

	        @Override
	        public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg) throws IOException {
	            return null;
	        }
	    });
	}
}
