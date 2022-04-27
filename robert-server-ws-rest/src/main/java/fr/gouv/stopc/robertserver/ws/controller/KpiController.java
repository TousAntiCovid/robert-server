package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.api.KpiApi;
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi;
import fr.gouv.stopc.robertserver.ws.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/internal/api/v1")
@RequiredArgsConstructor
public class KpiController implements KpiApi {

    private final TaskExecutor kpiExecutor;

    private final KpiService kpiService;

    @Override
    public ResponseEntity<List<RobertServerKpi>> kpi(LocalDate fromDate, LocalDate toDate) {
        return ResponseEntity.ok(kpiService.getKpis(fromDate, toDate));
    }

    @Override
    public ResponseEntity<Void> computeKpis() {
        log.info("queuing a KPI compute task");
        kpiExecutor.execute(kpiService::computeDailyKpis);
        return ResponseEntity.accepted().build();
    }
}
