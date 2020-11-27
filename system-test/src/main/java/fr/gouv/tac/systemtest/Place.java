package fr.gouv.tac.systemtest;

import fr.gouv.tac.tacwarning.model.QRCode;

import java.util.UUID;

public class Place{
    public QRCode getQrCode() {
        return this.qrCode;
    }

    public void setQrCode(final QRCode qrCode) {
        this.qrCode = qrCode;
    }

    QRCode qrCode;

    public Place(){
        qrCode = new QRCode();
        qrCode.setUuid(UUID.randomUUID().toString());
        qrCode.setVenueType(QRCode.VenueTypeEnum.R);
        qrCode.setVenueCapacity(20);
        qrCode.setVenueCategory(QRCode.VenueCategoryEnum.CAT1);
        setQrCode(qrCode);
    }

}