package fr.gouv.stopc.robert.server.batch.service.impl;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.configuration.ScoringAlgorithmConfiguration;
import fr.gouv.stopc.robert.server.batch.exception.RobertScoringException;
import fr.gouv.stopc.robert.server.batch.model.ScoringResult;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Scoring strategy that implements the algorithm version 2
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "robert.scoring.algo-version", havingValue = "2")
public class ScoringStrategyV2ServiceImpl implements ScoringStrategyService {

    private final IServerConfigurationService serverConfigurationService;

    private final ScoringAlgorithmConfiguration configuration;

    private final PropertyLoader propertyLoader;

    /**
     * Spring injection constructor
     * 
     * @param serverConfigurationService the
     *                                   <code>IServerConfigurationService</code>
     *                                   bean to inject
     * @param configuration              the
     *                                   <code>ScoringAlgorithmConfiguration</code>
     *                                   bean to inject
     */
    public ScoringStrategyV2ServiceImpl(IServerConfigurationService serverConfigurationService,
            ScoringAlgorithmConfiguration configuration,
            PropertyLoader propertyLoader) {
        this.serverConfigurationService = serverConfigurationService;
        this.configuration = configuration;
        this.propertyLoader = propertyLoader;
    }

    @Override
    public int getScoringStrategyVersion() {
        return 2;
    }

    @Override
    public int getNbEpochsScoredAtRiskThreshold() {
        return NB_EPOCHS_SCORED_AT_RISK;
    }

    // IF INCREASED TO VALUE > 1, remove the break; in the loop
    // for (EpochExposition epochExposition : scoresSinceLastNotif) {
    public final static int NB_EPOCHS_SCORED_AT_RISK = 1;
    // https://hal.inria.fr/hal-02641630/document (Table 4)

    // Aggregate (formula n56) taken from https://hal.inria.fr/hal-02641630/document
    public double aggregate(List<Double> scores) {
        // double scoreSum = scores.stream().mapToDouble(Double::doubleValue).sum();

        double scoreSum = 0.0;
        // https://hal.inria.fr/hal-02641630/document (56)

        // These are not actual rssi scores, they are rssiScore * duration in order to
        // be formula 57
        for (Double score : scores) {
            scoreSum += score;
        }

        return (1 - Math.exp(-this.propertyLoader.getR0ScoringAlgorithm() * scoreSum));
    }

    /**
     * c {@inheritDoc}
     */
    @Override
    public ScoringResult execute(Contact contact) throws RobertScoringException {

        List<HelloMessageDetail> messageDetails = contact.getMessageDetails();
        if (messageDetails.size() == 0) {
            String errorMessage = "Cannot score contact with no HELLO messages";
            log.error(errorMessage);
            throw new RobertScoringException(errorMessage);
        }

        final int epochDurationInMinutes = this.serverConfigurationService.getEpochDurationSecs() / 60;

        // Variables
        final List<Number>[] rssiGroupedByMinutes = new ArrayList[epochDurationInMinutes];
        double[] maxRssiByMinutes = new double[epochDurationInMinutes];
        int[] numberOfRssiByMinutes = new int[epochDurationInMinutes];
        for (int k = 0; k < epochDurationInMinutes; k++) {
            rssiGroupedByMinutes[k] = new ArrayList<>();
        }

        // Phase 1 : fading compensation
        // First index corresponds to the first minute of the first EBID emission
        double firstTimeCollectedOnDevice = messageDetails.get(0).getTimeCollectedOnDevice();
        double lastTimeCollectedOnDevice = messageDetails.get(messageDetails.size() - 1).getTimeCollectedOnDevice();

        // Create zero scoring if contacts helloMessages bounds exceed epoch tolerance
        if ((lastTimeCollectedOnDevice - firstTimeCollectedOnDevice) > (epochDurationInMinutes * 60
                + configuration.getEpochTolerance())) {
            String errorMessage = String.format(
                    "Skip contact because some hello messages are coming too late: %s sec after first message",
                    lastTimeCollectedOnDevice - firstTimeCollectedOnDevice
            );
            log.warn(errorMessage);

            // Initializing values to 0.0 will ignore this problematic contact in the
            // overall summation
            return ScoringResult.builder()
                    .rssiScore(0.0)
                    .duration(0)
                    .nbContacts(0)
                    .build();
        }

        // Tri et lissage des RSSI
        messageDetails.forEach(messageDetail -> {
            // On cherche la bonne minute dans l'époque
            double timestampDelta = messageDetail.getTimeCollectedOnDevice() - firstTimeCollectedOnDevice;
            int minuteInEpoch = (int) Math.floor(timestampDelta / 60.0);
            minuteInEpoch = minuteInEpoch > epochDurationInMinutes ? epochDurationInMinutes - 1 : minuteInEpoch;

            // Si la minute est comprise dans l'époque
            if ((minuteInEpoch >= 0) && (minuteInEpoch < epochDurationInMinutes)) {

                // Si le rssi (<= -5)
                // si il est > rssiMax (-35) alors il est ramené à rssiMax (-35) sinon on garde
                // la valeur
                // Sinon il est ignoré
                if (messageDetail.getRssiCalibrated() <= -5) {
                    // Cutting peaks / lissage
                    int rssi = Math.min(messageDetail.getRssiCalibrated(), configuration.getRssiMax());
                    rssiGroupedByMinutes[minuteInEpoch].add(rssi); // Note : On a donc que des rssi <=-35
                }
            }
        }
        );

        // Phase 2: Average RSSI
        for (int k = 0; k < epochDurationInMinutes - 1; k++) {
            ArrayList<Number> rssiOfTwoMinutes = new ArrayList<>();
            rssiOfTwoMinutes.addAll(rssiGroupedByMinutes[k]);
            rssiOfTwoMinutes.addAll(rssiGroupedByMinutes[k + 1]);

            maxRssiByMinutes[k] = softMax(rssiOfTwoMinutes, configuration.getSoftMaxA()); // calcul Rssi des deux min
            numberOfRssiByMinutes[k] = rssiGroupedByMinutes[k].size() + rssiGroupedByMinutes[k + 1].size(); // Nombre de
                                                                                                            // rssi dans
                                                                                                            // les deux
                                                                                                            // min
        }
        // Only one window for the last sample
        maxRssiByMinutes[epochDurationInMinutes - 1] = softMax(
                rssiGroupedByMinutes[epochDurationInMinutes - 1], configuration.getSoftMaxA()
        );
        numberOfRssiByMinutes[epochDurationInMinutes - 1] = rssiGroupedByMinutes[epochDurationInMinutes - 1].size();

        // Phase 3: Risk scoring
        // https://hal.inria.fr/hal-02641630/document - eq (52)

        int minuteMax = 0;
        int nbcontacts = 0;
        List<Number> risk = new ArrayList<>();

        for (int minuteInEpoch = 0; minuteInEpoch < epochDurationInMinutes; minuteInEpoch++) {
            if (numberOfRssiByMinutes[minuteInEpoch] > 0) {
                minuteMax = minuteInEpoch;
                int dd = Math.min(numberOfRssiByMinutes[minuteInEpoch], configuration.getDeltas().length - 1);
                double gamma = (maxRssiByMinutes[minuteInEpoch] - configuration.getP0())
                        / Double.parseDouble(configuration.getDeltas()[dd]);
                double vrisk = (gamma <= 0.0) ? 0.0 : (gamma >= 1) ? 1.0 : gamma;
                if (vrisk > 0) {
                    nbcontacts++;
                }
                risk.add(vrisk);
            }
        }

        return ScoringResult.builder()
                .rssiScore(Math.min(softMax(risk, configuration.getSoftMaxB()) * 1.2, 1.0) * minuteMax) // multiplying
                                                                                                        // by
                // duration because
                // we do not store it
                // yet in list of
                // exposed epochs
                .duration(minuteMax)
                .nbContacts(nbcontacts)
                .build();

    }

    /**
     * @param listValues
     * @param softmaxCoef
     * @return
     */
    private Double softMax(List<Number> listValues, double softmaxCoef) {
        int ll = listValues.size();
        double vm = 0.0;

        if (ll > 0) {
            double vlog = 0.0;
            for (int i = 0; i < ll; i++) {
                vlog += Math.exp(listValues.get(i).doubleValue() / softmaxCoef);
            }
            vm = softmaxCoef * Math.log(vlog / ll);
        }
        return vm;
    }
}
