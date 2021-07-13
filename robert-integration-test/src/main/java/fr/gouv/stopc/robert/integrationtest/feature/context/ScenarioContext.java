package fr.gouv.stopc.robert.integrationtest.feature.context;

import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import io.cucumber.spring.ScenarioScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, User> users = new HashMap<>(10);

    public User getOrCreateUser(String name) {
        return users.computeIfAbsent(name, this::createUser);
    }

    private User createUser(String userName) {
        return new User(userName);
    }

    public User getUser(String userName) {
        return users.get(userName);
    }

    public void updateUser(String userName, User user) {
        users.replace(userName, user);
    }


}
