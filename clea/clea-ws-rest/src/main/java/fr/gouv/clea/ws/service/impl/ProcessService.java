package fr.gouv.clea.ws.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IProcessService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessService implements IProcessService {

    @Override
    public void process(List<DecodedVisit> encryptedLocationSpecificParts) {
        // TODO persistence integration
    }
}
