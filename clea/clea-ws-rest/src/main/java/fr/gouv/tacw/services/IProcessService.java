package fr.gouv.tacw.services;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;

import java.util.List;

public interface IProcessService {

    void process(List<DecodedLocationSpecificPart> decodedLocationSpecificParts);
}
