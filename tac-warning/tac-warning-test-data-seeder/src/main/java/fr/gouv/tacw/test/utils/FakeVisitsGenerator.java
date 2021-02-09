package fr.gouv.tacw.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootApplication(scanBasePackages = { "fr.gouv.tacw.ws", "fr.gouv.tacw.database", "fr.gouv.tacw.test.utils" })
public class FakeVisitsGenerator implements ApplicationRunner {
    protected FakeVisitsGeneratorConfiguration configuration;
    protected WarningService warningService;
    /* random set of venues */
    protected final List<UUID> venues;
    protected List<FakeVisitor> visitors = new ArrayList<>();

    FakeVisitsGenerator(FakeVisitsGeneratorConfiguration configuration, WarningService ws){
        this.configuration = configuration;
        this.warningService = ws;
        venues = Stream.generate(UUID::randomUUID).limit(configuration.getNbVenues()).collect(Collectors.toList());
    }

    @Transactional
    void createAndAddVisitsToDataStore() {
        IntStream.rangeClosed(1, configuration.getNbVisitors())
            .forEach( i -> this.warningService.reportVisitsWhenInfected(new FakeVisitor(configuration, venues).generateVisits()) );
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String infoMessage = String.format(
            "Generating %d visits per day for %d visitors during %d days with %d hangouts per visitor and a set of %d venues of type %s and category %s", 
            configuration.getNbVisitsPerVisitorPerDay(),
            configuration.getNbVisitors(),
            configuration.getRetentionDays(),
            configuration.getNbHangouts(),
            configuration.getNbVenues(),
            configuration.getVenueType(),
            configuration.getVenueCategory());
        System.out.println(infoMessage);
        int nbVisits = configuration.getNbVisitors() * configuration.getNbVisitsPerVisitorPerDay() * configuration.getRetentionDays();
        System.out.println("Reporting a total of " + nbVisits + " visits (multiply by SALT value to get the number of recors in database).");
        this.createAndAddVisitsToDataStore();
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(FakeVisitsGenerator.class, args)));
    }
}

class FakeVisitor {
    protected List<UUID> hangouts;
    protected List<VisitVo> visits;
    private FakeVisitsGeneratorConfiguration configuration;
    private Random random;
    
    public FakeVisitor(FakeVisitsGeneratorConfiguration configuration, List<UUID> venues) {
        this.configuration = configuration;
        this.random = new Random();
        // pick a list of hang-outs for each visitor -- with repetition (fix?)
        this.hangouts = random.ints(0, configuration.getNbVenues())
                .limit(configuration.getNbHangouts())
                .mapToObj(venues::get)
                .collect(Collectors.toList());
    }

    protected List<VisitVo> generateVisits() {
        this.visits = new ArrayList<>();
        IntStream.rangeClosed(0, configuration.getRetentionDays() - 1).forEach( day -> 
            IntStream.rangeClosed(1, configuration.getNbVisitsPerVisitorPerDay()).forEach( i -> this.addVisit(random, day) ) );
        return this.visits;
    }
    
    protected void addVisit(Random random, long day) {
        long ts = TimeUtils.roundedCurrentTimeTimestamp() - TimeUtils.SECONDS_PER_DAY*day + this.pickVisitTimeOffset(random);
        int venueCapacity = 42;
        VisitVo visit = new VisitVo(String.valueOf(ts), 
                new QRCodeVo(
                        TokenTypeVo.STATIC, 
                        configuration.getVenueType(),
                        configuration.getVenueCategory(),
                        venueCapacity, 
                        this.hangouts.get(random.nextInt(configuration.getNbHangouts())).toString()));
        this.visits.add(visit);
    }

    protected int pickVisitTimeOffset(Random random) {
        return random.nextInt((int) TimeUtils.SECONDS_PER_DAY);
    }
}
