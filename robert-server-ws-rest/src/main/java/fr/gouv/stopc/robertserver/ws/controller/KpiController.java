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
@RequestMapping(value = "/internal/api")
@RequiredArgsConstructor
public class KpiController implements KpiApi {

    private final TaskExecutor kpiExecutor;

    private final KpiService kpiService;

    // TODO : Remove this when switch to v2
    @Override
    @RequestMapping(value = "/v1/kpi")
    public ResponseEntity<List<RobertServerKpi>> kpi(LocalDate fromDate, LocalDate toDate) {
        return ResponseEntity.ok(kpiService.getKpis(fromDate, toDate));
    }

    // TODO : Remove this when switch to v2
    @Override
    @RequestMapping(value = "/v1/tasks/compute-daily-kpis")
    public ResponseEntity<Void> computeKpis() {
        log.info("queuing a KPI compute task");
        kpiExecutor.execute(kpiService::computeDailyKpis);
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(value = "/v2/kpis")
    public ResponseEntity<RobertServerKpi> kpis() {
        return ResponseEntity.ok(kpiService.getKpis());
    }
}
