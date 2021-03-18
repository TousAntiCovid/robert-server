package fr.gouv.tac.systemtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.tac.robert.model.*;
import fr.gouv.tac.systemtest.model.MyVisit;
import fr.gouv.tac.systemtest.robert.AuthRequestDto;
import fr.gouv.tac.systemtest.robert.ClientIdentifierBundle;
import fr.gouv.tac.systemtest.robert.EphemeralTupleJson;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.auth.HttpBearerAuth;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static fr.gouv.tac.systemtest.utils.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.tac.systemtest.utils.EcdhUtils.generateKeyPair;

/**
 * This class represents a view of a visitor device (phone) it modelizes its
 * status (scanned QRCodes, robert status, ...) in order to capture steps state
 * changes.
 */
public class User {

	/**
	 * SEE TestHSM.class from hsm-tools project
	 */
	public static final String ROBERT_CRYPTO_SERVER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwpDgJdE0aRop5uibRYqCOLK7CnZ+DAyQhGrVD6XYXC/5LeNtLwqSxPAVcswqtiZyPs68h2Y3KEQn2y2bRxRzQg==";

	private static Logger logger = LoggerFactory.getLogger(User.class);

	private static final int MAX_SALT = 1000;
	private static Random random = new Random();

	@Getter
	@Setter
	private String name = "";

	private List<Visit> visitList = new ArrayList<Visit>();

	@Getter
	@Setter
	private List<VisitToken> tokens = new ArrayList<VisitToken>();

	private fr.gouv.tac.robert.model.RegisterRequest registerRequest = null;

	@Getter
	@Setter
	private RegisterSuccessResponse lastRegisterSuccessResponse = null;
	@Getter
	@Setter
	private ExposureStatusResponse lastExposureStatusResponse = null;
	@Getter
	@Setter
	private fr.gouv.tac.robert.model.ExposureStatusResponse lastRobertExposureStatusResponse = null;
	@Getter
	@Setter
	private ReportResponse lastTACWarningReportResponse = null;
	@Getter
	@Setter
	private ReportBatchResponse lastRobertReportsResponse = null;
	@Setter
	@Getter
	private SuccessResponse lastDeleteHistoryResponse = null;
	@Setter
	@Getter
	private SuccessResponse lastUnregisterResponse = null;

	@Getter
	@Setter
	private Boolean covidStatus = false;

	@Getter
	@Setter
	private String outcome;
	@Setter
	private String jwt;

	@Getter(AccessLevel.PRIVATE)
	List<Contact> contacts;

	@Getter(AccessLevel.PRIVATE)
	List<EphemeralTupleJson> decodedTuples;

	KeyPair keyPair;

	@Getter
	Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

	public User() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		this.keyPair = generateKeyPair();
		this.contacts = new ArrayList<>();
		generateKeyForTuples();
	}

	public User(String name, String place, String time, String status) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		this.keyPair = generateKeyPair();
		this.contacts = new ArrayList<>();
		generateKeyForTuples();
		this.name = name;

		Visit visit = new MyVisit(place);
		Long time2 = new PrettyTimeParser().parse(time).get(0).getTime();
		visit.setTimestamp(Long.toString(time2));
	}

	public void addVisit(QRCode qrCode, Long timestamp) {
		Visit visit = new MyVisit("unknown");
		visit.setTimestamp(Long.toString(timestamp));
		visit.setQrCode(qrCode);
		this.visitList.add(visit);
		VisitToken visitToken = new VisitToken();
		visitToken.setType(VisitToken.TypeEnum.STATIC);
		visitToken.setPayload(DigestUtils.sha256Hex(random.nextInt(MAX_SALT) + qrCode.getUuid()));
		visitToken.setTimestamp(visit.getTimestamp());
		this.tokens.add(visitToken);
	}

	public void addMultipleVisit(QRCode qrCode, List<Long> list) {
		for (Long temp : list) {
			// addVisit(qrCode,temp);
		}
	}

	public User(String name) throws RobertServerCryptoException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
		this.keyPair = generateKeyPair();
		this.contacts = new ArrayList<>();
		generateKeyForTuples();
		this.setName(name);
	}

	public void setCovidStatus(final String covidStatus) {
		this.covidStatus = covidStatus.equals("positive");
	}

	public fr.gouv.tac.robert.model.ExposureStatusResponse status(final fr.gouv.tac.robert.api.DefaultApi robertApi) throws fr.gouv.tac.robert.ApiException {

		AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.STATUS, 0);
		final fr.gouv.tac.robert.model.ExposureStatusRequest statusRequest = new fr.gouv.tac.robert.model.ExposureStatusRequest();
		statusRequest.setEbid(authRequestDto.getEbid());
		statusRequest.setEpochId(authRequestDto.getEpochId());
		statusRequest.setTime(authRequestDto.getTime());
		statusRequest.setMac(authRequestDto.getMac());
		final fr.gouv.tac.robert.model.ExposureStatusResponse result = robertApi.eSR(statusRequest);
		this.setLastRobertExposureStatusResponse(result);
		logger.debug("#### sendTacWarningStatus atRisk={}", result.getRiskLevel());
		return result;
	}

	public Integer sendTacWarningStatus(fr.gouv.tac.tacwarning.api.DefaultApi apiInstance) {
		logger.debug("{}.sendTacWarningStatus", this.name);
		Integer evaluatedRiskLevel = null;
		ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
		for (VisitToken token : tokens) {
			exposureStatusRequest.addVisitTokensItem(token);
		}
		try {
			ExposureStatusResponse result = apiInstance.eSR(exposureStatusRequest);
			this.setLastExposureStatusResponse(result);
			evaluatedRiskLevel = result.getRiskLevel();
			logger.debug("#### sendTacWarningStatus atRisk={}", result.getRiskLevel());
		} catch (ApiException e) {
			logger.error(e.getMessage(), e);
		}
		return evaluatedRiskLevel;
	}

	public ReportBatchResponse sendRobertReportBatch(final String qrCode, final fr.gouv.tac.robert.api.DefaultApi apiInstance) throws fr.gouv.tac.robert.ApiException {
		logger.debug("{}.sendRobertReportBatch", this.name);
		fr.gouv.tac.robert.model.ReportBatchRequest reportBatchRequest = new fr.gouv.tac.robert.model.ReportBatchRequest();
		reportBatchRequest.token(qrCode);
		reportBatchRequest.setContacts(contacts);
		// TODO add natural language cucumber API to define Bluetooth contacts
		ReportBatchResponse reportBatchResponse = apiInstance.reportBatch(reportBatchRequest);
		this.setLastRobertReportsResponse(reportBatchResponse);
		this.setJwt(reportBatchResponse.getReportValidationToken());
		if (reportBatchResponse.getReportValidationToken() == null) {
			logger.warn("Robert reportBatch returned a null JWT ReportValidationToken. \n{}", reportBatchResponse);
		}
		return reportBatchResponse;
	}


	public Boolean sendTacWarningReport(fr.gouv.tac.tacwarning.api.DefaultApi apiInstance) {
		logger.debug("{}.sendTacWarningReport", this.name);
		Boolean isResponseSuccess = null;
		ReportRequest reportRequest = new fr.gouv.tac.tacwarning.model.ReportRequest();
		if (this.jwt == null) {
			logger.warn("JWT token is null, cannot correctly authenticate TAC Warning Report request");
			logger.warn("Sending empty string as bearerAuth");
			((HttpBearerAuth) (apiInstance.getApiClient().getAuthentication("bearerAuth"))).setBearerToken("");
		} else {
			((HttpBearerAuth) (apiInstance.getApiClient().getAuthentication("bearerAuth"))).setBearerToken(this.jwt);
		}
		for (Visit visit : visitList) {
			reportRequest.addVisitsItem(visit);
		}
		try {
			ReportResponse response = apiInstance.report(reportRequest);
			this.setLastTACWarningReportResponse(response);
			isResponseSuccess = response.getSuccess();
		} catch (ApiException e) {
			logger.error("Exception when calling TACWarningDefaultApi#report", e);
			logger.error("Status code: {}", e.getCode());
			logger.error("Reason: {}", e.getResponseBody());
			logger.error("Response headers: {}", e.getResponseHeaders());
			logger.error("Request was:\n{}", reportRequest);
		}
		return isResponseSuccess;
	}

	public RegisterRequest getRegisterRequest() {
		if (registerRequest == null) {
			registerRequest = new RegisterRequest();
			registerRequest.setCaptcha("string");
			registerRequest.setCaptchaId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			registerRequest.setClientPublicECDHKey(this.getPublicKey());
			PushInfo pushInfo = new PushInfo();
			pushInfo.setLocale("fr");
			pushInfo.setTimezone("Europe/Paris");
			pushInfo.setToken("string");
			registerRequest.setPushInfo(pushInfo);
		}
		return this.registerRequest;
	}

	public RegisterSuccessResponse tacRobertRegister(fr.gouv.tac.robert.api.DefaultApi apiInstance) throws fr.gouv.tac.robert.ApiException {
		RegisterSuccessResponse message;
		try {
			RegisterSuccessResponse registerSuccessResponse = apiInstance.register(getRegisterRequest());
			message = registerSuccessResponse;
			this.decryptRegisterResponse(registerSuccessResponse);
		} catch (fr.gouv.tac.robert.ApiException e) {
			System.err.println("Exception when calling RobertDefaultApi#register");
			System.err.println("Status code: " + e.getCode());
			System.err.println("Reason: " + e.getResponseBody());
			System.err.println("Response headers: " + e.getResponseHeaders());
			e.printStackTrace();
			throw e;
		}
		return message;
	}











	private void decryptRegisterResponse(RegisterSuccessResponse registerData) {

		CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.get().getKeyForTuples());
		try {
			byte[] decryptedTuples = aesGcm.decrypt(registerData.getTuples());
			ObjectMapper objectMapper = new ObjectMapper();
			this.decodedTuples = objectMapper.readValue(
					decryptedTuples,
					new TypeReference<List<EphemeralTupleJson>>() {
					});
		} catch (IOException | RobertServerCryptoException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void exchangeEbIdWith(User otherAppMobile) {

		List<HelloMessageDetail> otherHelloMessageDetailVos = new ArrayList<>();

		//TODO REPLACE THIS VALUE WITH CONFIGURATION
		otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
				TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()), -7));

		otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
				TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 100000), -5));

		otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
				TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 102000), -5));

		otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
				TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 150000), -5));

		Contact otherContact = new Contact();
		otherContact.setEcc(otherAppMobile.getDecodedTuples().get(0).getKey().getEcc());
		otherContact.setEbid(otherAppMobile.getDecodedTuples().get(0).getKey().getEbid());
		otherContact.setIds(otherHelloMessageDetailVos);

		addNewContact(otherContact);

	}

	public void addNewContact(Contact contactVo) {
		this.getContacts().add(contactVo);
	}

	private HelloMessageDetail generateHelloMessage(User user, long timeAsNtpSeconds, int rssiCalibrated) {

		byte[] ebid = user.getDecodedTuples().get(0).getKey().getEbid();
		byte[] ecc = user.getDecodedTuples().get(0).getKey().getEcc();

		byte[] timeOfDevice = new byte[4];
		System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeOfDevice, 0, 4);

		byte[] timeHelloB = new byte[4];
		System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeHelloB, 0, 4);

		// Clear out the first two bytes
		timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
		timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

		int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
		int timeHello = ByteUtils.bytesToInt(timeHelloB);

		final HelloMessageDetail helloMessageDetail = new HelloMessageDetail();

		helloMessageDetail.setTimeCollectedOnDevice(Integer.toUnsignedLong(timeReceived));
		helloMessageDetail.setTimeFromHelloMessage(timeHello);
		helloMessageDetail.setMac(user.generateMACforHelloMessage(ebid, ecc, timeHelloB));
		helloMessageDetail.setRssiCalibrated(rssiCalibrated);

		return helloMessageDetail;
	}

	private void generateKeyForTuples() throws RobertServerCryptoException {
		this.clientIdentifierBundleWithPublicKey = deriveKeysFromBackendPublicKey(java.util.Base64.getDecoder().decode(ROBERT_CRYPTO_SERVER_PUBLIC_KEY), keyPair);
	}

	private AuthRequestDto prepareAuthRequestDto(final int adjustTimeInSeconds, final DigestSaltEnum saltEnum, final int tupleIndex) {

		byte[] ebid = getDecodedTuples().get(tupleIndex).getKey().getEbid();
		int epochId = getDecodedTuples().get(tupleIndex).getEpochId();
		byte[] time = generateTime32(System.currentTimeMillis(), adjustTimeInSeconds);
		byte[] mac = generateMAC(ebid, epochId, time, saltEnum);

		return AuthRequestDto.builder()
				.ebid(ebid)
				.epochId(epochId)
				.time(time)
				.mac(mac).build();
	}

	private String getPublicKey() {
		return org.bson.internal.Base64.encode(keyPair.getPublic().getEncoded());
	}

	private byte[] generateTime32(long unixTimeInMillis, int adjustTimeInSeconds) {
		long tsInSeconds = TimeUtils.convertUnixMillistoNtpSeconds(unixTimeInMillis);
		tsInSeconds += adjustTimeInSeconds;
		byte[] tsInSecondsB = ByteUtils.longToBytes(tsInSeconds);
		byte[] time = new byte[4];

		System.arraycopy(tsInSecondsB, 4, time, 0, 4);

		return time;
	}

	private byte[] generateMAC(byte[] ebid, int epochId, byte[] time, DigestSaltEnum saltEnum) {
		// Merge arrays
		// HMAC-256
		// return hash
		byte[] agg = new byte[8 + 4 + 4];
		System.arraycopy(ebid, 0, agg, 0, ebid.length);
		System.arraycopy(ByteUtils.intToBytes(epochId), 0, agg, ebid.length, Integer.BYTES);
		System.arraycopy(time, 0, agg, ebid.length + Integer.BYTES, time.length);

		byte[] mac = new byte[32];
		try {
			mac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), agg, saltEnum);
		} catch (Exception e) {
			logger.info("Problem generating SHA256");
		}
		return mac;
	}

	private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument, final DigestSaltEnum salt)
			throws Exception {

		final byte[] prefix = new byte[]{salt.getValue()};

		// HMAC-SHA256 processing
		return cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));
	}

	public byte[] generateMACforHelloMessage(byte[] ebid, byte[] ecc, byte[] timeHelloMessage) {
		// TODO A FINIR
		// Merge arrays
		// HMAC-256
		// return hash
		byte[] mai = new byte[ebid.length + ecc.length + 2];
		System.arraycopy(ecc, 0, mai, 0, ecc.length);
		System.arraycopy(ebid, 0, mai, ecc.length, ebid.length);
		//take into account the 2 last bytes
		System.arraycopy(timeHelloMessage, 2, mai, ecc.length + ebid.length, 2);


		byte[] encryptedMac = new byte[32];
		try {
			encryptedMac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), mai, DigestSaltEnum.HELLO);
		} catch (Exception e) {
			logger.info("Problem generating SHA256");
		}

		// truncate the result from 0 to 40-bits
		return Arrays.copyOfRange(encryptedMac, 0, 5);
	}

	public SuccessResponse deleteHistory(fr.gouv.tac.robert.api.DefaultApi robertApi) throws fr.gouv.tac.robert.ApiException {

		AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.DELETE_HISTORY, 0);
		AuthentifiedRequest statusRequest = new AuthentifiedRequest();
		statusRequest.setEbid(authRequestDto.getEbid());
		statusRequest.setEpochId(authRequestDto.getEpochId());
		statusRequest.setTime(authRequestDto.getTime());
		statusRequest.setMac(authRequestDto.getMac());

		SuccessResponse deleteHistoryResponse = robertApi.deleteExposureHistory(statusRequest);
		this.setLastDeleteHistoryResponse(deleteHistoryResponse);
		return deleteHistoryResponse;
	}

	public SuccessResponse unregister(fr.gouv.tac.robert.api.DefaultApi robertApi) {

		AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.UNREGISTER, 0);
		UnregisterRequest unregisterRequest = new UnregisterRequest();
		unregisterRequest.setEbid(authRequestDto.getEbid());
		unregisterRequest.setEpochId(authRequestDto.getEpochId());
		unregisterRequest.setTime(authRequestDto.getTime());
		unregisterRequest.setMac(authRequestDto.getMac());
		try {
			SuccessResponse unregisterResponse = robertApi.unregister(unregisterRequest);
			this.setLastUnregisterResponse(unregisterResponse);
			return unregisterResponse;
		} catch (fr.gouv.tac.robert.ApiException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
}