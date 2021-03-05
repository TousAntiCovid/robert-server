package fr.gouv.stopc.robertserver;

import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robertserver.utils.AppMobileClient;
import fr.gouv.stopc.robertserver.utils.DockerUtils;
import fr.gouv.stopc.robertserver.utils.Doctor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class FunctionalTests {

    final DockerUtils dockerUtils;
    final AppMobileClient appMobileClient;
    final AppMobileClient appMobileClient2;
    final Doctor doctor;


    public FunctionalTests() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.appMobileClient = new AppMobileClient();
        this.appMobileClient2 = new AppMobileClient();
        this.doctor = new Doctor();
        this.dockerUtils = new DockerUtils();

    }

    @Test
    public void allEndpointsExceptReports() {

        appMobileClient.register();

        appMobileClient.status();

        appMobileClient.deleteHistory();

        appMobileClient.unregister();

    }


    @Test
    public void reportsWithEmptyContact() {

        appMobileClient.register();

        // the submission code server must be running
        String shortCode = doctor.generateShortCode();

        appMobileClient.reports(shortCode);


        // reports triggers unregister ??? ==> CURRENTLY THIS IS NOT THE CASE

    }


    @Test
    public void testTemp() {
        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(1590015613), 6, timeHelloB, 2, 2);

        System.out.println(ByteUtils.bytesToInt(timeHelloB));
        System.out.println(TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()));


    }

    @Test
    public void reportsWithContact() {

        appMobileClient.register();
        appMobileClient2.register();

        appMobileClient.exchangeEbIdWith(appMobileClient2);

        // the submission code server must be running
        String shortCode = doctor.generateShortCode();

        appMobileClient.reports(shortCode);

        // LANCER LE JOB DE SCORING
        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, true);

        // reports triggers unregister ??? ==> CURRENTLY THIS IS NOT THE CASE

        // status on appMobileClient ==> atRisk = true
        appMobileClient2.status();

    }

}
