package com.ethercis.vehr;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = {"src/test/resources/features/"},
    glue = {"com.ethercis.vehr"},
    format = { "pretty",
        "html:target/site/cucumber-pretty",
        "json:target/cucumber.json" }
)
public class RunCukesIT {
}
