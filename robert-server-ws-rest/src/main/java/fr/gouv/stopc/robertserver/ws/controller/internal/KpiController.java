package fr.gouv.stopc.robertserver.ws.controller.internal;

import fr.gouv.stopc.robertserver.ws.api.KpiApi;
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi;
import fr.gouv.stopc.robertserver.ws.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/internal/api/v2")
@RequiredArgsConstructor
public class KpiController implements KpiApi {

    private final KpiService kpiService;

    @Override
    public ResponseEntity<RobertServerKpi> kpis() {

        return ResponseEntity.ok(kpiService.getKpis());
    }

}
