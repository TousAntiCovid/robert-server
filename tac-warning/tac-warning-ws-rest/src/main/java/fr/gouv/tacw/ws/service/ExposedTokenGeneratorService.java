package fr.gouv.tacw.ws.service;

import java.util.stream.Stream;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.ws.vo.VisitVo;

public interface ExposedTokenGeneratorService {
    public int numberOfGeneratedTokens();
    public Stream<ExposedStaticVisitEntity> generateAllExposedTokens(VisitVo visit);
}
