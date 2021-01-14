package fr.gouv.tacw.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootApplication(scanBasePackages = { "fr.gouv.tacw.ws", "fr.gouv.tacw.database" })
public class FakeVisitsGenerator implements ApplicationRunner {
    public final static int NB_VENUES = 140; // 1400
    public final static int NB_VISITORS = 4; // 80000
    public final static int VISITS_PER_VISITOR_PER_DAY = 4; // 20
    public final static int RETENTION_DAYS = 12;
    // venues (random set of NB_VENUES UUIDs)
    protected final static List<UUID> VENUES = Stream.generate(UUID::randomUUID).limit(NB_VENUES).collect(Collectors.toList());

    protected List<FakeVisitor> visitors = new ArrayList<>();
    protected WarningService warningService;

    @Autowired
    FakeVisitsGenerator(WarningService ws){
        this.warningService = ws;
    }

    @Transactional
    void createAndAddVisitsToDataStore() {
        IntStream.rangeClosed(1, NB_VISITORS)
            .forEach( i -> this.warningService.reportVisitsWhenInfected(new FakeVisitor().generateVisits()) );
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {		   
        this.createAndAddVisitsToDataStore();
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(FakeVisitsGenerator.class, args)));
    }

}

class FakeVisitor {
    protected static final int NB_SECONDS_PER_DAY = 86400;
    protected final int nbHangouts = 8;
    protected List<UUID> hangouts;
    protected List<VisitVo> visits;

    protected List<VisitVo> generateVisits() {
        Random random = new Random();
        // pick a list of hang-outs for each visitor -- with repetition (fix?)
        this.hangouts = random.ints(0, FakeVisitsGenerator.NB_VENUES)
                .limit(nbHangouts)
                .mapToObj(FakeVisitsGenerator.VENUES::get)
                .collect(Collectors.toList());

        this.visits = new ArrayList<>();
        IntStream.rangeClosed(0, FakeVisitsGenerator.RETENTION_DAYS - 1).forEach( day -> 
            IntStream.rangeClosed(1, FakeVisitsGenerator.VISITS_PER_VISITOR_PER_DAY).forEach( i -> this.addVisit(random, day) ) );
        return this.visits;
    }
    
    protected void addVisit(Random random, long day) {
        // pick visit time offset
        long visitTimeOffset = random.nextInt(NB_SECONDS_PER_DAY);
        long ts = TimeUtils.roundedCurrentTimeTimestamp() - NB_SECONDS_PER_DAY*day + visitTimeOffset;
        VisitVo visit = new VisitVo(String.valueOf(ts), 
                new QRCodeVo(
                        TokenTypeVo.STATIC, 
                        VenueTypeVo.N, // TODO: randomize?
                        VenueCategoryVo.CAT5, // TODO: randomize?
                        42, 
                        this.hangouts.get(random.nextInt(nbHangouts)).toString()));
        this.visits.add(visit);
    }
}
