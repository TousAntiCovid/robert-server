Feature: Covid-19 privacy settings
  As a user
  I want to know that i can manage my privacy settings
  in order to take care of my privacy

  Scenario: User data is deleted after 15 days
    Given fifteen days ago, Sarah and John met and Sarah was at risk following John report
    Then all Sarah's contact and risk data older than 15 days were deleted