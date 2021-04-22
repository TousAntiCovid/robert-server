package fr.gouv.clea.client.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import fr.gouv.clea.client.model.MalformedReport;
import fr.gouv.clea.client.model.Report;
import fr.gouv.clea.client.model.ReportResponse;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.utils.HttpClientWrapper;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportService {
    private HttpClientWrapper httpClient;
    private String reportEndPoint;
    private ReportResponse lastReportResponse;
    private Report lastReportRequest;

    public ReportService(String reportEndPoint) throws IOException {
        this(reportEndPoint, new HttpClientWrapper());
    }

    public ReportService(String reportEndPoint, HttpClientWrapper httpClient) throws IOException {
        this.httpClient = httpClient;
        this.reportEndPoint = reportEndPoint;
    }

    /**
     * report a list of qr code to the backend server
     */
    public ReportResponse report(List<ScannedQrCode> localList, long pivotDate) throws IOException, InterruptedException {
        String jsonRequest;
        Report reportRequest = new Report(pivotDate);
        reportRequest.addAllVisits(localList);
        this.lastReportRequest = reportRequest;
        
        log.info("Reporting {} visits to {} with pivot date : {}", localList.size(), this.reportEndPoint, pivotDate);
        jsonRequest = new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(reportRequest);
        log.info(jsonRequest);
        this.lastReportResponse = this.post(jsonRequest);
        log.info("REPORTRESPONSE : {}",lastReportResponse);
        return this.lastReportResponse;
    }

    public ReportResponse report(List<ScannedQrCode> localList) throws IOException, InterruptedException {
        String jsonRequest;
        Report reportRequest = new Report();
        reportRequest.addAllVisits(localList);
        this.lastReportRequest = reportRequest;
        log.info("Reporting {} visits to {}", localList.size(), this.reportEndPoint);
        jsonRequest = new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(reportRequest);
        log.info(jsonRequest);
        this.lastReportResponse = this.post(jsonRequest);
        return this.lastReportResponse;
    }

    public ReportResponse reportMalformed(List<ScannedQrCode> localList, boolean pivotDateMalformed, boolean qrCodeMalformed, boolean scanTimeMalformed) throws IOException, InterruptedException {
        String jsonRequest;
        MalformedReport reportRequest;
        if(pivotDateMalformed){
           reportRequest = new MalformedReport<String>("error");
        }else{
           reportRequest = new MalformedReport<Long>(TimeUtils.ntpTimestampFromInstant(Instant.now().minus(Duration.ofDays(14))));
        }
        reportRequest.setQrCodeMalformed(qrCodeMalformed);
        reportRequest.setScanTimeMalformed(scanTimeMalformed);
        reportRequest.addAllVisits(localList);
        
        log.info("Reporting {} visits to {} Using a malformed request", localList.size(), this.reportEndPoint);
        jsonRequest = new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(reportRequest);
        log.info(jsonRequest);
        this.lastReportResponse = this.post(jsonRequest);
        log.info("REPORTRESPONSE : {}",lastReportResponse);
        return this.lastReportResponse;
    }

    public ReportResponse reportEmpty(List<ScannedQrCode> localList, boolean pivotDateEmpty, boolean qrCodeEmpty,
    boolean scanTimeEmpty) throws IOException, InterruptedException {
        String jsonRequest;
        MalformedReport reportRequest;
        if(pivotDateEmpty){
           reportRequest = new MalformedReport<Long>();
        }else{
           reportRequest = new MalformedReport<Long>(TimeUtils.ntpTimestampFromInstant(Instant.now().minus(Duration.ofDays(14))));
        }
        reportRequest.setQrCodeAbsent(qrCodeEmpty);
        reportRequest.setScanTimeAbsent(scanTimeEmpty);
        reportRequest.addAllVisits(localList);
        
        log.info("Reporting {} visits to {} Using a malformed request", localList.size(), this.reportEndPoint);
        jsonRequest = new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(reportRequest);
        log.info(jsonRequest);
        this.lastReportResponse = this.post(jsonRequest);
        log.info("REPORTRESPONSE : {}",lastReportResponse);
        return this.lastReportResponse;
    }


    public ReportResponse getLastReportResponse(){
        return this.lastReportResponse;
    }

    protected ReportResponse post(String jsonRequest) throws IOException, InterruptedException {
        return this.httpClient.post(this.reportEndPoint, jsonRequest, ReportResponse.class);
    }

    public void setAuthorizationToken(String token){
        this.httpClient.addAuthorizationToken(token);
    }

}
