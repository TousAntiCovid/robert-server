package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProcessService implements IProcessService {

    @Override
    public void process(List<DecodedLocationSpecificPart> decodedLocationSpecificParts) {
        // TODO persistence integration
    }
}
