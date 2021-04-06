package fr.gouv.clea.client.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import fr.gouv.clea.client.model.Report;
import fr.gouv.clea.client.model.ReportResponse;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.utils.HttpClientWrapper;
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
        
        log.info("Reporting {} visits to {}", localList.size(), this.reportEndPoint);
        jsonRequest = new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(reportRequest);
        log.info(jsonRequest);
        this.lastReportResponse = this.post(jsonRequest);
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
