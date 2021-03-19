package fr.gouv.clea.ws.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import fr.gouv.clea.ws.model.DecodedLocationSpecificPart;
import fr.gouv.clea.ws.service.IProcessService;

import java.util.List;

@Service
@Slf4j
public class ProcessService implements IProcessService {

    @Override
    public void process(List<DecodedLocationSpecificPart> decodedLocationSpecificParts) {
        // TODO persistence integration
    }
}
