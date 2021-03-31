package fr.gouv.clea.client.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Report {

    long pivotDate;
    List<Visit> visits;
    
    public Report(long pivotDate){
        this.visits = new ArrayList<>();
        this.pivotDate = pivotDate;
    }

    public void addVisit(ScannedQrCode scannedQr){
        this.visits.add(new Visit(scannedQr.getQrCode(), scannedQr.getScanTimeAsNtpTimestamp()));
    }

    public void addAllVisits(List<ScannedQrCode> localList) {
        localList.forEach(this::addVisit);
    }
}
