package fr.gouv.tac.systemtest;

import fr.gouv.tac.systemtest.config.Config;
import org.junit.Test;

import static org.junit.Assert.*;


public class ConfigTest {


    @Test
    public void getPropertyTest() {
        assertEquals(Config.getProperty("SALT_RANGE"),"1000");
    }

}