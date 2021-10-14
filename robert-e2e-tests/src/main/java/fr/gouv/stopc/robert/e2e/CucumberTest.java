package fr.gouv.stopc.robert.e2e;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("fr/gouv/stopc/robert/e2e")
@IncludeEngines("cucumber")
public class CucumberTest {
}
