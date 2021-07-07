package fr.gouv.stopc.robert.integrationtests;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:features" }, plugin = { "pretty", "html:cucumber-reports.html" })
public class CucumberTest {
}
