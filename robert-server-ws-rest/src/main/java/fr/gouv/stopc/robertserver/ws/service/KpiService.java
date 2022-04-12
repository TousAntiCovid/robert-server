package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Range.Bound.exclusive;
import static org.springframework.data.domain.Range.Bound.inclusive;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final IRegistrationService registrationDbService;

    private final WebserviceStatisticsRepository webserviceStatisticsRepository;

    public void computeDailyKpis() {
        final var nbAlertedUsers = registrationDbService.countNbUsersNotified();
        final var nbExposedUsersNotAtRisk = registrationDbService.countNbExposedUsersButNotAtRisk();
        final var nbInfectedUsersNotNotified = registrationDbService.countNbUsersAtRiskAndNotNotified();
        final var nbNotifiedUsersScoredAgain = registrationDbService.countNbNotifiedUsersScoredAgain();

        final var nowAtStartOfDay = LocalDate.now().atStartOfDay().atZone(UTC).toInstant();
        final var todayStatistics = webserviceStatisticsRepository
                .findById(nowAtStartOfDay)
                .orElse(new WebserviceStatistics(nowAtStartOfDay, 0L, 0L, 0L, 0L, 0L));
        final var updatedStatistics = todayStatistics.toBuilder()
                .date(nowAtStartOfDay)
                // override statistics relative to the service start time
                .totalAlertedUsers(nbAlertedUsers)
                .totalExposedButNotAtRiskUsers(nbExposedUsersNotAtRisk)
                .totalInfectedUsersNotNotified(nbInfectedUsersNotNotified)
                .totalNotifiedUsersScoredAgain(nbNotifiedUsersScoredAgain)
                // keep statistics incremented on the fly
                .notifiedUsers(todayStatistics.getNotifiedUsers())
                .build();
        webserviceStatisticsRepository.save(updatedStatistics);
    }

    public List<RobertServerKpi> getKpis(final LocalDate fromDate, final LocalDate toDate) {
        final var range = Range
                .from(inclusive(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .to(exclusive(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        return webserviceStatisticsRepository.getWebserviceStatisticsByDateBetween(range).stream()
                .map(
                        stats -> RobertServerKpi.builder()
                                .date(stats.getDate().atZone(UTC).toLocalDate())
                                .nbAlertedUsers(stats.getTotalAlertedUsers())
                                .nbExposedButNotAtRiskUsers(stats.getTotalExposedButNotAtRiskUsers())
                                .nbInfectedUsersNotNotified(stats.getTotalInfectedUsersNotNotified())
                                .nbNotifiedUsersScoredAgain(stats.getTotalNotifiedUsersScoredAgain())
                                .nbNotifiedUsers(stats.getNotifiedUsers())
                                .build()
                )
                .sorted(comparing(RobertServerKpi::getDate))
                .collect(toList());
    }

    public void updateWebserviceStatistics(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            webserviceStatisticsRepository.increment(Instant.now().truncatedTo(DAYS));
        }
    }
}
