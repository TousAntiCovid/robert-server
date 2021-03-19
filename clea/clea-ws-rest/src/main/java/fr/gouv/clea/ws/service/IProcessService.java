package fr.gouv.clea.ws.service;

import java.util.List;

import fr.gouv.clea.ws.model.DecodedLocationSpecificPart;

public interface IProcessService {

    void process(List<DecodedLocationSpecificPart> decodedLocationSpecificParts);
}
