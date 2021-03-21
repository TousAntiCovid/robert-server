package fr.gouv.clea.ws.service;

import java.util.List;

import fr.gouv.clea.ws.model.DecodedVisit;

public interface IProcessService {

    void process(List<DecodedVisit> pruned);
}
