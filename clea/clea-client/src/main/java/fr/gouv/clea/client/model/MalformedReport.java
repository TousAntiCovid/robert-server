package fr.gouv.clea.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class MalformedReport<T> {

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<T> pivotDate;
    List<Visit> visits;

    @JsonIgnore
    boolean qrCodeMalformed;
    @JsonIgnore
    boolean scanTimeMalformed;
    @JsonIgnore
    boolean qrCodeAbsent;
    @JsonIgnore
    boolean scanTimeAbsent;
    
    
    public MalformedReport(){
        this.visits = new ArrayList<>();
        this.pivotDate = Optional.empty();
        this.qrCodeMalformed = false;
        this.scanTimeMalformed = false;
        this.qrCodeAbsent = false;
        this.scanTimeAbsent = false;
    }

    public MalformedReport(T pivotDate){
        this.visits = new ArrayList<>();
        this.pivotDate = Optional.of(pivotDate);
        this.qrCodeMalformed = false;
        this.scanTimeMalformed = false;
        this.qrCodeAbsent = false;
        this.scanTimeAbsent = false;
    }

    public void addVisit(ScannedQrCode scannedQr){
        String qr = scannedQr.getQrCode();
        if(qrCodeMalformed)
            qr = "";
        else if(qrCodeAbsent)
            qr = null;

        Long scantime = scannedQr.getScanTimeAsNtpTimestamp();
        if(scanTimeMalformed)
            scantime = -1L;
        else if(scanTimeAbsent)
            scantime = null;
        this.visits.add(new Visit(qr, scantime));
    }

    public void addAllVisits(List<ScannedQrCode> localList) {
        localList.forEach(this::addVisit);
    }
}
