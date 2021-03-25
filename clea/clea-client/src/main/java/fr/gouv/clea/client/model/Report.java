package fr.gouv.clea.client.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Report {

    List<Visit> visits;

    public Report(){
        this.visits = new ArrayList<>();
    }

    public void addVisit(ScannedQrCode qr){
        this.visits.add(new Visit(qr.getLocationTemporaryId(), qr.getScanTime()));
    }

    public void addAllVisits(List<ScannedQrCode> localList) {
        for (ScannedQrCode qr : localList) {
            this.addVisit(qr);
        }
    }
}
