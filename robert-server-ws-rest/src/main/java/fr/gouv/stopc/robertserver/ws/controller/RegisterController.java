package fr.gouv.stopc.robertserver.ws.controller;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse;
import fr.gouv.stopc.robert.server.common.service.ServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.ApplicationConfigurationModel;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.ws.dto.ClientConfigDto;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robertserver.database.service.IApplicationConfigService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.dto.RegisterResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.service.CaptchaService;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V2, "${controller.path.prefix}" + UriConstants.API_V3,
        "${controller.path.prefix}" + UriConstants.API_V4, "${controller.path.prefix}" + UriConstants.API_V5, "${controller.path.prefix}" + UriConstants.API_V6 })
@Consumes(MediaType.APPLICATION_JSON_VALUE)
@Produces(MediaType.APPLICATION_JSON_VALUE)
public class RegisterController {

    private final IRegistrationService registrationService;
    private final ServerConfigurationService serverConfigurationService;
    private final IApplicationConfigService applicationConfigService;
    private final ICryptoServerGrpcClient cryptoServerClient;
    private final IRestApiService restApiService;

    private final WsServerConfiguration wsServerConfiguration;

    private final CaptchaService captchaService;

    @PostMapping(value = UriConstants.REGISTER)
	public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterVo registerVo)
			throws RobertServerException {

        if (!this.captchaService.verifyCaptcha(registerVo)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return postCheckRegister(registerVo);
	}


    private ResponseEntity<RegisterResponseDto> postCheckRegister(RegisterVo registerVo) throws RobertServerException {

        byte[] clientPublicECDHKey = Base64.decode(registerVo.getClientPublicECDHKey());
        byte[] serverCountryCode = new byte[1];
        serverCountryCode[0] = this.serverConfigurationService.getServerCountryCode();

        CreateRegistrationRequest request = CreateRegistrationRequest.newBuilder()
                .setClientPublicKey(ByteString.copyFrom(clientPublicECDHKey))
                .setNumberOfDaysForEpochBundles(this.wsServerConfiguration.getEpochBundleDurationInDays())
                .setServerCountryCode(ByteString.copyFrom(serverCountryCode))
                .setFromEpochId(TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart()))
                .build();

        Optional<CreateRegistrationResponse> response = this.cryptoServerClient.createRegistration(request);

        if(!response.isPresent() || response.get().hasError()) {
            log.error("Unable to generate an identity for the client");
            throw new RobertServerException(MessageConstants.ERROR_OCCURED);
        }

        CreateRegistrationResponse identity = response.get();

        Registration registration = Registration.builder()
                .permanentIdentifier(identity.getIdA().toByteArray())
                .build();

        Optional<Registration> registered = this.registrationService.saveRegistration(registration);

        if (registered.isPresent()) {
            RegisterResponseDto registerResponseDto = new RegisterResponseDto();

            List<ApplicationConfigurationModel> serverConf = this.applicationConfigService.findAll();
            if (CollectionUtils.isEmpty(serverConf)) {
                registerResponseDto.setConfig(Collections.emptyList());
            } else {
                registerResponseDto.setConfig(serverConf
                        .stream()
                        .map(item -> ClientConfigDto.builder().name(item.getName()).value(item.getValue()).build())
                        .collect(Collectors.toList()));
            }

            registerResponseDto.setTuples(Base64.encode(identity.getTuples().toByteArray()));
            registerResponseDto.setTimeStart(this.serverConfigurationService.getServiceTimeStart());

            Optional.ofNullable(registerVo.getPushInfo()).ifPresent(this.restApiService::registerPushNotif);

            return ResponseEntity.status(HttpStatus.CREATED).body(registerResponseDto);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
