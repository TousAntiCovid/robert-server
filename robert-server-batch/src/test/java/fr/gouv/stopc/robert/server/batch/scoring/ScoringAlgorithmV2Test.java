package fr.gouv.stopc.robert.server.batch.scoring;

import com.opencsv.CSVReader;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.configuration.ScoringAlgorithmConfiguration;
import fr.gouv.stopc.robert.server.batch.exception.RobertScoringException;
import fr.gouv.stopc.robert.server.batch.model.ScoringResult;
import fr.gouv.stopc.robert.server.batch.service.impl.ScoringStrategyV2ServiceImpl;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import lombok.SneakyThrows;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.ROBERT_EPOCH;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoringAlgorithmV2Test {

    private static final RobertClock CLOCK = new RobertClock("2021-06-01");

    private ScoringStrategyV2ServiceImpl serviceScoring;

    @BeforeEach
    void setup_scoring_service_and_dependencies() {
        final var serverConfigurationServiceStub = new IServerConfigurationService() {

            @Override
            public long getServiceTimeStart() {
                return CLOCK.atEpoch(0).asNtpTimestamp();
            }

            @Override
            public byte getServerCountryCode() {
                return 33;
            }

            @Override
            public int getEpochDurationSecs() {
                return (int) ROBERT_EPOCH.getDuration().toSeconds();
            }
        };

        final var configuration = new ScoringAlgorithmConfiguration();
        configuration.setDeltas(new String[] { "39.0", "27.0", "23.0", "21.0", "20.0", "15.0" });
        configuration.setRssiMax(-35);
        configuration.setP0(-66.0);
        configuration.setSoftMaxA(4.342);
        configuration.setSoftMaxB(0.2);
        configuration.setEpochTolerance(180);

        final var propertyLoader = new PropertyLoader() {

            @Override
            public Double getR0ScoringAlgorithm() {
                return 0.0071;
            }
        };

        serviceScoring = new ScoringStrategyV2ServiceImpl(
                serverConfigurationServiceStub, configuration,
                propertyLoader
        );
    }

    @Test
    void throw_exception_when_no_hello_messages_in_contact() {
        // Given
        final var emptyContact = Contact.builder().messageDetails(List.of()).build();

        // When
        try (final var logCaptor = LogCaptor.forClass(ScoringStrategyV2ServiceImpl.class)) {
            final var exception = assertThrows(
                    RobertScoringException.class, () -> serviceScoring.execute(emptyContact)
            );

            // Then
            assertThat(exception.getMessage()).isEqualTo("Cannot score contact with no HELLO messages");
            assertThat(logCaptor.getErrorLogs()).contains("Cannot score contact with no HELLO messages");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "PT3M1S", "PT15M" })
    void logs_when_no_hello_messages_exceeded_epoch_limit(final Duration receptionDelay) throws RobertScoringException {
        // Given
        final var now = CLOCK.now();
        final var exceededTime = now
                .plus(1, ROBERT_EPOCH)
                .plus(receptionDelay);

        final var contact = Contact.builder()
                .messageDetails(
                        List.of(
                                // First message
                                HelloMessageDetail.builder()
                                        .rssiCalibrated(-50)
                                        .timeCollectedOnDevice(now.asNtpTimestamp())
                                        .timeFromHelloMessage(now.as16LessSignificantBits())
                                        .mac(String.format("%s:%s", "user___1", now.asEpochId()).getBytes())
                                        .build(),
                                // Second message with timeCollected witch exceeded allowed time drift
                                HelloMessageDetail.builder()
                                        .rssiCalibrated(-50)
                                        .timeCollectedOnDevice(exceededTime.asNtpTimestamp())
                                        .timeFromHelloMessage(now.as16LessSignificantBits())
                                        .mac(String.format("%s:%s", "user___1", now.asEpochId()).getBytes())
                                        .build()
                        )
                )
                .build();

        // When
        try (final var logCaptor = LogCaptor.forClass(ScoringStrategyV2ServiceImpl.class)) {
            serviceScoring.execute(contact);

            // Then
            final var diff = exceededTime.asNtpTimestamp() - now.asNtpTimestamp() + 0.0;
            assertThat(logCaptor.getWarnLogs())
                    .contains(
                            String.format(
                                    "Skip contact because some hello messages are coming too late: %s sec after first message",
                                    diff
                            )
                    );
        }
    }

    @ParameterizedTest
    @CsvSource({
            "/input_v2/C4_20_A/C4_20_A00.csv, 0.11556942265181178    ,  8,  4",
            "/input_v2/C4_20_A/C4_20_A01.csv, 0.3229067671860443     ,  5,  2",
            "/input_v2/C4_20_A/C4_20_A02.csv, 1.0                    ,  8,  9",
            "/input_v2/C4_20_A/C4_20_A03.csv, 0.941392043740963      ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A04.csv, 1.0                    ,  7,  4",
            "/input_v2/C4_20_A/C4_20_A05.csv, 0.35967921691503046    ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A06.csv, 0.7564266561578571     ,  8,  9",
            "/input_v2/C4_20_A/C4_20_A07.csv, 1.0                    ,  3,  4",
            "/input_v2/C4_20_A/C4_20_A08.csv, 1.0                    , 11,  8",
            "/input_v2/C4_20_A/C4_20_A09.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A10.csv, 0.057754450877641854   ,  5,  2",
            "/input_v2/C4_20_A/C4_20_A11.csv, 0.39818091219734414    ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A12.csv, 0.009797860399408692   ,  5,  1",
            "/input_v2/C4_20_A/C4_20_A13.csv, 0.26874716417803873    ,  5,  4",
            "/input_v2/C4_20_A/C4_20_A14.csv, 0.03702302713136917    ,  8,  5",
            "/input_v2/C4_20_A/C4_20_A15.csv, 0.42720215504342757    ,  8,  9",
            "/input_v2/C4_20_A/C4_20_A16.csv, 0.10028595878253897    ,  8,  5",
            "/input_v2/C4_20_A/C4_20_A17.csv, 0.06408216875196332    ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A18.csv, 0.07546335918764831    ,  8,  8",
            "/input_v2/C4_20_A/C4_20_A19.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A20.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A21.csv, 0.005495250780545454   ,  5,  1",
            "/input_v2/C4_20_A/C4_20_A22.csv, 0.006556325324440052   ,  5,  1",
            "/input_v2/C4_20_A/C4_20_A23.csv, 0.016607740845802552   ,  8,  4",
            "/input_v2/C4_20_A/C4_20_A24.csv, 0.041219350276851696   ,  8,  5",
            "/input_v2/C4_20_A/C4_20_A25.csv, 0.004171986838835206   ,  5,  1",
            "/input_v2/C4_20_A/C4_20_A26.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A27.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A28.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A29.csv, 0.02539924468717017    ,  5,  1",
            "/input_v2/C4_20_A/C4_20_A30.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A31.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A32.csv, 0.18147692305337412    ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A33.csv, 0.08987367221784223    ,  5,  2",
            "/input_v2/C4_20_A/C4_20_A34.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A35.csv, 0.004784906247089645   ,  8,  1",
            "/input_v2/C4_20_A/C4_20_A36.csv, 0.12421508579978256    ,  8,  6",
            "/input_v2/C4_20_A/C4_20_A37.csv, 0.13711138208459778    ,  5,  2",
            "/input_v2/C4_20_A/C4_20_A38.csv, 0.01615483358205426    ,  9,  2",
            "/input_v2/C4_20_A/C4_20_A39.csv, 0.6940357271487697     , 14,  7",
            "/input_v2/C4_20_A/C4_20_A40.csv, 0.7249324561015698     ,  0,  1",
            "/input_v2/C4_20_A/C4_20_A41.csv, 0.15590766159792765    ,  8,  5",
            "/input_v2/C4_20_A/C4_20_A42.csv, 0.0                    ,  7,  0",
            "/input_v2/C4_20_A/C4_20_A43.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A44.csv, 0.3354095835431747     ,  3,  4",
            "/input_v2/C4_20_A/C4_20_A45.csv, 4.285074627067092E-4   ,  8,  1",
            "/input_v2/C4_20_A/C4_20_A46.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A47.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A48.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A49.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A50.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A51.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A52.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A53.csv, 0.0                    ,  8,  0",
            "/input_v2/C4_20_A/C4_20_A54.csv, 0.0                    ,  5,  0",
            "/input_v2/C4_20_A/C4_20_A55.csv, 0.0                    ,  0,  0",
            "/input_v2/C4_20_A/C4_20_A56.csv, 1.0                    ,  5,  6",
            "/input_v2/C4_20_A/C4_20_A57.csv, 1.0                    ,  8,  9",

            "/input_v2/R1_AA/R1_AA00.csv, 0.2259471044964842         ,  3,  2",
            "/input_v2/R1_AA/R1_AA01.csv, 0.042329639114379036       , 12,  1",
            "/input_v2/R1_AA/R1_AA02.csv, 0.10650857766721923        , 12,  8",
            "/input_v2/R1_AA/R1_AA03.csv, 1.0                        ,  3,  3",
            "/input_v2/R1_AA/R1_AA04.csv, 0.09273966763679818        , 12,  6",
            "/input_v2/R1_AA/R1_AA05.csv, 0.37133818098372           ,  3,  4",
            "/input_v2/R1_AA/R1_AA06.csv, 0.3311951204731387         , 12,  8",
            "/input_v2/R1_AA/R1_AA07.csv, 0.0789962559100572         ,  3,  3",
            "/input_v2/R1_AA/R1_AA08.csv, 1.0                        ,  3,  2",
            "/input_v2/R1_AA/R1_AA09.csv, 0.273538599276163          , 12,  5",
            "/input_v2/R1_AA/R1_AA10.csv, 0.002744278271541257       , 12,  1",
            "/input_v2/R1_AA/R1_AA11.csv, 0.08118925486303318        ,  3,  2",
            "/input_v2/R1_AA/R1_AA12.csv, 0.27973109082195263        ,  3,  2",
            "/input_v2/R1_AA/R1_AA13.csv, 0.11413323165980317        , 12,  7",
            "/input_v2/R1_AA/R1_AA14.csv, 0.1320433444537647         , 12,  8",
            "/input_v2/R1_AA/R1_AA15.csv, 0.15560317361598303        ,  3,  3",
            "/input_v2/R1_AA/R1_AA16.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA17.csv, 0.0                        ,  2,  0",
            "/input_v2/R1_AA/R1_AA18.csv, 0.03075350717796424        , 12,  2",
            "/input_v2/R1_AA/R1_AA19.csv, 0.01621210327136334        ,  3,  1",
            "/input_v2/R1_AA/R1_AA20.csv, 0.006039454671881747       , 12,  1",
            "/input_v2/R1_AA/R1_AA21.csv, 0.07633639917572335        ,  3,  1",
            "/input_v2/R1_AA/R1_AA22.csv, 0.0733546277032031         , 12,  4",
            "/input_v2/R1_AA/R1_AA23.csv, 0.022966487702374996       ,  3,  1",
            "/input_v2/R1_AA/R1_AA24.csv, 0.13477851024721602        , 12, 10",
            "/input_v2/R1_AA/R1_AA25.csv, 0.4772384344935852         ,  3,  3",
            "/input_v2/R1_AA/R1_AA26.csv, 0.016616187323681017       ,  3,  1",
            "/input_v2/R1_AA/R1_AA27.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA28.csv, 0.0                        ,  2,  0",
            "/input_v2/R1_AA/R1_AA29.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA30.csv, 0.030815354827299996       , 12,  1",
            "/input_v2/R1_AA/R1_AA31.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA32.csv, 0.0                        ,  3,  0",
            "/input_v2/R1_AA/R1_AA33.csv, 0.0                        ,  9,  0",
            "/input_v2/R1_AA/R1_AA34.csv, 0.0                        , 11,  0",
            "/input_v2/R1_AA/R1_AA35.csv, 0.5337258000363373         ,  3,  2",
            "/input_v2/R1_AA/R1_AA36.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA37.csv, 0.0021829966957378354      ,  3,  1",
            "/input_v2/R1_AA/R1_AA38.csv, 0.004833588165450613       , 10,  1",
            "/input_v2/R1_AA/R1_AA39.csv, 0.0                        ,  1,  0",
            "/input_v2/R1_AA/R1_AA40.csv, 0.031191271280426064       ,  3,  1",
            "/input_v2/R1_AA/R1_AA41.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA42.csv, 0.8956579583812733         ,  3,  2",
            "/input_v2/R1_AA/R1_AA43.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA44.csv, 0.025114430265707404       , 12,  1",
            "/input_v2/R1_AA/R1_AA45.csv, 0.0                        ,  3,  0",
            "/input_v2/R1_AA/R1_AA46.csv, 0.0                        ,  8,  0",
            "/input_v2/R1_AA/R1_AA47.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA48.csv, 0.0                        ,  9,  0",
            "/input_v2/R1_AA/R1_AA49.csv, 0.0                        ,  3,  0",
            "/input_v2/R1_AA/R1_AA50.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA51.csv, 0.0                        ,  8,  0",
            "/input_v2/R1_AA/R1_AA52.csv, 0.0                        , 12,  0",
            "/input_v2/R1_AA/R1_AA53.csv, 0.0                        ,  3,  0",
            "/input_v2/R1_AA/R1_AA54.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA55.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA56.csv, 0.0                        ,  4,  0",
            "/input_v2/R1_AA/R1_AA57.csv, 0.0                        , 11,  0",
            "/input_v2/R1_AA/R1_AA58.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA59.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA60.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA61.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA62.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA63.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA64.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA65.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA66.csv, 0.0                        ,  0,  0",
            "/input_v2/R1_AA/R1_AA67.csv, 0.2463398180057588         ,  1,  2",
            "/input_v2/R1_AA/R1_AA68.csv, 0.0                        ,  1,  0"
    })
    void risk_from_hello_messages_contact_must_match_expected_risk(final String path,
            final double expectedRssiScore,
            final int expectedDuration,
            final int expectedContactCount) throws RobertScoringException {

        // Given
        final var contact = readContact(new ClassPathResource(path));

        // When
        final var risk = serviceScoring.execute(contact);

        // Then
        assertThat(risk).isEqualTo(
                ScoringResult.builder()
                        .rssiScore(expectedRssiScore * expectedDuration)
                        .duration(expectedDuration)
                        .nbContacts(expectedContactCount)
                        .build()
        );
    }

    @ParameterizedTest
    @CsvSource({
            "/input_v2/C4_20_A/*.csv, 0.44161884230882675",
            "/input_v2/R1_AA/*.csv  , 0.20799057803577403"
    })
    void aggregateContactRisksShouldReturnExpectedFinalRisk(final String path, final double expectedFinalRisk)
            throws IOException {
        // Given
        final var contacts = new PathMatchingResourcePatternResolver().getResources(path);
        final var scores = Arrays.stream(contacts)
                .map(this::readContact)
                .map(contact -> {
                    try {
                        return serviceScoring.execute(contact);
                    } catch (RobertScoringException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(ScoringResult::getRssiScore)
                .collect(toList());
        // When
        final var finalRisk = serviceScoring.aggregate(scores);

        // Then
        assertThat(finalRisk).isEqualTo(expectedFinalRisk);
    }

    @SneakyThrows
    private Contact readContact(final Resource resource) {
        try (final var reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            // Skip the header
            reader.skip(1);
            return Contact.builder()
                    .messageDetails(
                            reader.readAll().stream()
                                    .map(this::csvLineToHelloMessageDetail)
                                    .collect(toList())
                    )
                    .build();
        }
    }

    private HelloMessageDetail csvLineToHelloMessageDetail(String[] csvLine) {
        return HelloMessageDetail.builder()
                .timeCollectedOnDevice(Long.parseLong(csvLine[1].trim()))
                .timeFromHelloMessage(Integer.parseInt(csvLine[2].trim()))
                .rssiCalibrated(Integer.parseInt(csvLine[3].trim()))
                .build();
    }
}
