package fr.gouv.tac.systemtest;

import fr.gouv.tac.tacwarning.model.QRCode;

import java.util.UUID;

/**
 * This class represents a view of a venue  (restaurant, cafe, etc)
 * it modelizes its status  (name, generated QRCodes, ...)   in order to capture steps state changes.
 */
public class Place{


    private String name ;
    public QRCode getQrCode() {
        return this.qrCode;
    }

    public void setQrCode(final QRCode qrCode) {
        this.qrCode = qrCode;
    }

    QRCode qrCode;

    public Place(String name){
        this.name = name;
        qrCode = new QRCode();
        qrCode.setUuid(UUID.randomUUID().toString());
        qrCode.setVenueType(QRCode.VenueTypeEnum.R);
        qrCode.setVenueCapacity(20);
        qrCode.setVenueCategory(QRCode.VenueCategoryEnum.CAT1);
        setQrCode(qrCode);
    }
    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

}