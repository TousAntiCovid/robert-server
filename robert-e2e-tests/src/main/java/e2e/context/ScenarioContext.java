package e2e.context;

import e2e.appmobile.AppMobile;
import io.cucumber.spring.ScenarioScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, AppMobile> applicationMobileMap = new HashMap<>();

    public AppMobile getOrCreateApplication(String name) {
        return applicationMobileMap.computeIfAbsent(name, this::createApplication);
    }

    private AppMobile createApplication(String userName) {
        return new AppMobile(userName);
    }
}
