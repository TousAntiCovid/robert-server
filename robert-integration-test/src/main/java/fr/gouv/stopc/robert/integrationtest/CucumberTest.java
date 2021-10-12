package fr.gouv.stopc.robert.integrationtest;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("fr/gouv/stopc/robert/integrationtest")
@IncludeEngines("cucumber")
public class CucumberTest {
}
