package fr.gouv.tac.systemtest;

import org.apache.commons.codec.digest.DigestUtils;
import org.openapitools.client.model.QRCode;
import org.openapitools.client.model.Visit;
import org.openapitools.client.model.VisitToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Visitor {
    private static final int MAX_SALT = 1000;
    private static  Random random=new Random();
    List<Visit> visitList = new ArrayList<Visit>() ;
    List<VisitToken>tokens = new ArrayList<VisitToken>();

    public List<VisitToken> getTokens() {
        return this.tokens;
    }

    public void setTokens( List<VisitToken> tokens) {
        this.tokens = tokens;
    }


    public Visitor()  {
    }

    public void addVisit(QRCode qrCode, Long timestamp) {
        Visit visit = new Visit();
        visit.setTimestamp(Long.toString(timestamp));
        visit.setQrCode(qrCode);
        this.visitList.add(visit);
        VisitToken visitToken = new VisitToken();
        visitToken.setType(VisitToken.TypeEnum.STATIC);
        visitToken.setPayload(DigestUtils.sha256Hex(random.nextInt(MAX_SALT)+qrCode.getUuid()).toString());
        this.tokens.add(visitToken);
    }

    public void addMultipleVisit(QRCode qrCode, List<Long> list){
        for (Long temp : list) {
            addVisit(qrCode,temp);
        }
    }


}