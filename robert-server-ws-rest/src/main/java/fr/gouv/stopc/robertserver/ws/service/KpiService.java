package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.database.service.WebserviceStatisticsService;
import fr.gouv.stopc.robertserver.ws.dto.RobertServerKpi;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Default implementation of the <code>IKpiService</code>
 */
@Service
@AllArgsConstructor
public class KpiService {

    /**
     * The registration management service
     */
    private final IRegistrationService registrationDbService;

    private final WebserviceStatisticsService webserviceStatisticsService;

    public List<RobertServerKpi> computeKpi(LocalDate fromDate, LocalDate toDate) {
        // Retrieve the different kpis of the current date (because of the
        // implementation of the Robert Protocol, kpis of Robert Server can be
        // calculated only for the current date)
        final var nbAlertedUsers = registrationDbService.countNbUsersNotified();
        final var nbExposedUsersNotAtRisk = registrationDbService.countNbExposedUsersButNotAtRisk();
        final var nbInfectedUsersNotNotified = registrationDbService.countNbUsersAtRiskAndNotNotified();
        final var nbNotifiedUsersScoredAgain = registrationDbService.countNbNotifiedUsersScoredAgain();
        final var nbNotifiedUsers = webserviceStatisticsService.countNbNotifiedUsersBetween(
                LocalDate.now().minus(1, ChronoUnit.DAYS).atTime(23, 59, 59).toInstant(ZoneOffset.UTC),
                LocalDate.now().plus(1, ChronoUnit.DAYS).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
        return List.of(
                new RobertServerKpi(
                        LocalDate.now(), nbAlertedUsers, nbExposedUsersNotAtRisk,
                        nbInfectedUsersNotNotified, nbNotifiedUsersScoredAgain, nbNotifiedUsers
                )
        );
    }

}
