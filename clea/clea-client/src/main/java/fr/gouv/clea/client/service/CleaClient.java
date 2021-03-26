package fr.gouv.clea.client.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.ScannedQrCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class CleaClient {
    private String name;
    private List<ScannedQrCode> localList;

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
     * @param timestamp : timestamp (in seconds) of the scan
     * @return true if qr code has been added to the local list, false otherwise
     */
    public boolean scanQrCode(String qrCode, long timestamp) {
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
        ScannedQrCode scannedQr  = new ScannedQrCode(qrCode, timestamp);

        //Check for duplicate in local list
        for (ScannedQrCode prevQR : this.localList) {
            if(scannedQr.getLocationTemporaryId().equals(prevQR.getLocationTemporaryId()) && Math.abs(prevQR.getScanTime() - timestamp) <= configuration.getDupScanThreshold()){
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
    public void batchScanQrCode(Map<Long, String> qrcodes) {
        for (Entry<Long, String> qrcode : qrcodes.entrySet()) {
            scanQrCode(qrcode.getValue(),qrcode.getKey());
        }
    }
}
