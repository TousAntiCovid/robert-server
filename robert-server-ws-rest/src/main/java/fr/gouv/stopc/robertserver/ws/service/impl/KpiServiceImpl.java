package fr.gouv.stopc.robertserver.ws.service.impl;

import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.database.service.impl.StatisticsService;
import fr.gouv.stopc.robertserver.ws.dto.RobertServerKpi;
import fr.gouv.stopc.robertserver.ws.service.IKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Default implementation of the <code>IKpiService</code>
 */
@Service
@RequiredArgsConstructor
public class KpiServiceImpl implements IKpiService {

    /**
     * The registration management service
     */
    private final IRegistrationService registrationDbService;

    private final StatisticsService statisticsService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RobertServerKpi> computeKpi(LocalDate fromDate, LocalDate toDate) {
        // Retrieve the different kpis of the current date (because of the
        // implementation of the Robert Protocol, kpis of Robert Server can be
        // calculated only for the current date)
        final var nbAlertedUsers = registrationDbService.countNbUsersNotified();
        final var nbExposedUsersNotAtRisk = registrationDbService.countNbExposedUsersButNotAtRisk();
        final var nbInfectedUsersNotNotified = registrationDbService.countNbUsersAtRiskAndNotNotified();
        final var nbNotifiedUsersScoredAgain = registrationDbService.countNbNotifiedUsersScoredAgain();
        final var nbNotifiedTotal = statisticsService.countNbNotifiedTotalBetween(
                fromDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                toDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        );

        return List.of(
                new RobertServerKpi(
                        LocalDate.now(),
                        nbAlertedUsers,
                        nbExposedUsersNotAtRisk,
                        nbInfectedUsersNotNotified,
                        nbNotifiedUsersScoredAgain,
                        nbNotifiedTotal
                )
        );
    }

}
