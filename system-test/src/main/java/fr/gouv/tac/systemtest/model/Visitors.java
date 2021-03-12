package fr.gouv.tac.systemtest.model;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.systemtest.User;
import lombok.Getter;
import lombok.Setter;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Visitors {

    @Getter
    @Setter
    private List<User> list = new ArrayList<>();

    public User getUserByName(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        if (!list.isEmpty()) {
            for (User user : list) {
                if (user.getName().equals(name)) {
                    return user;
                }
            }
        }
        User newUser = new User(name);
        list.add(newUser);
        return newUser;
    }
}
