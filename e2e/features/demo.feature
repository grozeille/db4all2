@epic:demo-journey
Feature: Demo journey
  As a first-time user
  I want to set up the app, load a CSV and filter it
  So that I can validate the core flow end to end

  Scenario: 1. Create the administrator account
    Given I am on the setup page
    When I create the administrator account "mathias.kluba@gmail.com" with password "qqQQ11!!"
    Then I am redirected to the login page

  Scenario: 2. Log in
    Given I am on the login page
    When I log in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    Then I see the project list

  Scenario: 3. Create the demo project
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    When I create a project named "demo"
    Then the project settings for "demo" are displayed

  Scenario: 4. Create a table from the clients CSV
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    And the project "demo" exists
    And the datasource "sample-data" points to the samples folder
    When I create a CSV table named "user" from "clients.csv" in datasource "sample-data"
    Then the table shows data with the "email" column

  Scenario: 5. Filter the table data
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    And the project "demo" exists
    And the datasource "sample-data" points to the samples folder
    And the CSV table "user" exists from "clients.csv" in datasource "sample-data"
    When I filter where "city" equals "Paris"
    Then every visible row has city "Paris"
