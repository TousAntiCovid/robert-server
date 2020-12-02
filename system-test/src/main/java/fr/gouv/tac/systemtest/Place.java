package fr.gouv.tac.systemtest;

import java.util.HashMap;
import java.util.UUID;

import fr.gouv.tac.tacwarning.model.QRCode;

/**
 * This class represents a view of a venue (restaurant, cafe, etc) it modelizes
 * its status (name, generated QRCodes, ...) in order to capture steps state
 * changes.
 */
public class Place {

	private String name;

	private HashMap<String, QRCode> staticQRCodeMap = new HashMap<>();

	public Place(String name) {
		this.name = name;
		// generate a default static QR code for convenience
		this.generateNewStaticQRCode("default");
	}

	/**
	 * create a new Static QRCode with default VenueType, Category and capacity (R,
	 * 20, CAT1)
	 * 
	 * @param qrcodeId
	 */
	public void generateNewStaticQRCode(String qrcodeId) {

		QRCode qrCode = new QRCode();
		qrCode.setUuid(UUID.randomUUID().toString());
		qrCode.setVenueType(QRCode.VenueTypeEnum.R);
		qrCode.setVenueCapacity(20);
		qrCode.setVenueCategory(QRCode.VenueCategoryEnum.CAT1);

		staticQRCodeMap.put(qrcodeId, qrCode);
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public HashMap<String, QRCode> getStaticQRCodeMap() {
		return staticQRCodeMap;
	}

	/**
	 * 
	 * @return the first Static QRCode of the map (supposing this is the only one)
	 */
	public QRCode getDefaultStaticQrCode() {
		return staticQRCodeMap.values().iterator().next();
	}

}