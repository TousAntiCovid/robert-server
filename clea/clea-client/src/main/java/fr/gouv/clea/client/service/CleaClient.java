package fr.gouv.clea.client.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class CleaClient {
    private String name;
    private List<ScannedQrCode> localList;
    @ToString.Exclude
    private Optional<StatusService> statusService;
    @ToString.Exclude
    private Optional<ReportService> reportService;

    public CleaClient(String name) {
        this.name = name;
        this.localList = new ArrayList<>();
    }

    public List<ScannedQrCode> getLocalList() {
        return this.localList;
    }

    /**
     * verify the validity of the qr code and add it to the local list
     * @param qrCode : the plain-text  representing the scanned qr code. format : PREFIX + BASE64(LSP)
     * @param scanTime : Instant of the scan
     * @return true if qr code has been added to the local list, false otherwise
     */
    public boolean scanQrCode(String qrCode, Instant scanTime) {
        CleaClientConfiguration configuration;
        try {
             configuration = CleaClientConfiguration.getInstance();
        } catch (IOException e) {
            log.error("Can't access config file, scanning can't proceed.");
            return false;
        }

        //check if prefix is present then removes it
        if (!qrCode.startsWith(configuration.getQrPrefix())) {
            return false;
        }
        qrCode = qrCode.substring(configuration.getQrPrefix().length());
        ScannedQrCode scannedQr  = new ScannedQrCode(qrCode, scanTime);

        //Check for duplicate in local list
        for (ScannedQrCode prevQR : this.localList) {
            if(scannedQr.getLocationTemporaryId().equals(prevQR.getLocationTemporaryId())
                    && (Duration.between(prevQR.getScanTime(), scanTime).abs().toSeconds() <= configuration.getDupScanThreshold())) {
                return false;
            }
        }
        localList.add(scannedQr);
        return true;
    }

    /**
     * Add a batch of Qr code to the local list, verifying their validity beforehand 
     * @param qrcodes : list of qr code and their timestamp to add to the local list
     */
    public void batchScanQrCode(Map<Instant, String> qrcodes) {
        for (Entry<Instant, String> qrcode : qrcodes.entrySet()) {
            this.scanQrCode(qrcode.getValue(), qrcode.getKey());
        }
    }

    public boolean sendReport(Instant pivotDate) throws IOException, InterruptedException{
        return this.getReportService().report(localList, TimeUtils.ntpTimestampFromInstant(pivotDate)).isSuccess(); //TODO: NTP Time with TimeUtils from Clea-Crypto
    }

    public boolean sendReport() throws IOException, InterruptedException{
        return this.sendReport(Instant.now());
    }

    public boolean getLastReportSuccess() throws IOException{
        return this.getReportService().getLastReportResponse().isSuccess();
    }

    public float getStatus() throws IOException {
        return this.getStatusService().status(localList);
    }

    private ReportService getReportService() throws IOException{  
        return reportService.orElse(this.createReportService());
    }

    private ReportService createReportService() throws IOException{
        CleaClientConfiguration configuration = CleaClientConfiguration.getInstance();
        this.reportService = Optional.of(new ReportService(configuration.getBackendUrl() + configuration.getReportPath()));
        this.reportService.get().setAuthorizationToken(this.getAuthorizationToken());
        return reportService.get();
    }

    private StatusService getStatusService() throws IOException{
        return statusService.orElse(this.createStatusService());
    }

    private StatusService createStatusService() throws IOException {
       statusService = Optional.of(new StatusService());
       return statusService.get();
    }

    private String getAuthorizationToken(){
        //TODO: get/generate authorization Token
        return "dummy";
    }
}
