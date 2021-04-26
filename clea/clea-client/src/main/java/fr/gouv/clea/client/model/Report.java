package fr.gouv.clea.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class Report {

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    OptionalLong pivotDate;
    List<Visit> visits;
    
    public Report(){
        this.visits = new ArrayList<>();
        this.pivotDate = OptionalLong.empty();
    }

    public Report(long pivotDate){
        this.visits = new ArrayList<>();
        this.pivotDate = OptionalLong.of(pivotDate);
    }

    public void addVisit(ScannedQrCode scannedQr){
        this.visits.add(new Visit(scannedQr.getQrCode(), scannedQr.getScanTimeAsNtpTimestamp()));
    }

    public void addAllVisits(List<ScannedQrCode> localList) {
        localList.forEach(this::addVisit);
    }
}
