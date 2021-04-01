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

    public void addVisit(ScannedQrCode scannedQr){
        this.visits.add(new Visit(scannedQr.getQrCode(), scannedQr.getScanTime()));
    }

    public void addAllVisits(List<ScannedQrCode> localList) {
        localList.forEach(this::addVisit);
    }
}
