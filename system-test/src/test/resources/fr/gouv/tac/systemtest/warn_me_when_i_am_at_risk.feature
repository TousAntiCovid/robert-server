Feature: Warn me if I am at risk of contracting covid19
  If I visited a venue along with a number of covid19-positive people,
  I would like my TAC app to warn me that I am at risk of contracting covid19

  Scenario:
    Given that I (Hugo), Stacy and Steeve registered on TAC one week ago
    And that I (Hugo) entered a restaurant at 12:30 pm two days ago
    And that Stacy visited the same venue, entering at 12:15 pm on the same day
    And that Steeve also visited the same venue, entering at 12:03 pm on the same day
    And that Stacy and Steve reported their positive status yesterday
    Then I should be warned that I am at risk

  Scenario:
    Given that I (Heather), Serena and Stephen registered on TAC one week ago
    Given that I (Heather) entered a restaurant at 12:30 pm two days ago
    And that Serena visited the same venue, entering at 11:15 pm on the same day
    And that Stephen also visited the same venue, entering at 13:30 pm on the same day
    And that Serena and Stephen reported their positive status yesterday
    Then I (Heather) should be warned that I am at risk
 
  Scenario:
    Given that I (Henry) and Stephanie registered on TAC one week ago
    Given that I (Henry) entered a restaurant at 12:30 pm two days ago
    And during that time, Stephanie visited the same venue
    And that Stephanie reported her positive status yesterday
    Then I should not be warned that I am at risk

