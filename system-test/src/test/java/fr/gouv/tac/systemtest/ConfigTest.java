package fr.gouv.tac.systemtest;

import org.junit.Test;

import static org.junit.Assert.*;


public class ConfigTest {


    @Test
    public void getPropertyTest() {
        assertEquals(Config.getProperty("SALT_RANGE"),"2");
    }

}