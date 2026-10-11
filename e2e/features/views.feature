@epic:views-journey
Feature: Views journey
  As a data user
  I want to save table queries as views and manage them next to tables
  So that I can build on filtered, reshaped data

  Background:
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    And the project "demo" exists
    And the datasource "sample-data" points to the samples folder
    And the CSV table "user" exists from "clients.csv" in datasource "sample-data"
    And the view "toulouse" exists

  Scenario: 1. Tables and views share one list with badges
    When I open the project tables and views list
    Then I see the table "user" and the view "toulouse" with their badges

  Scenario: 2. Filter the list by tables or views
    When I open the project tables and views list
    And I show only views
    Then the list shows "toulouse" but not "user"
    When I show only tables
    Then the list shows "user" but not "toulouse"
    When I show tables and views
    Then I see the table "user" and the view "toulouse" with their badges

  Scenario: 3. Search the unified list
    When I open the project tables and views list
    And I search the list for "toul"
    Then the list shows "toulouse" but not "user"
    When I clear the list search
    Then I see the table "user" and the view "toulouse" with their badges

  Scenario: 4. Breadcrumb on the view editor
    When I open the view "toulouse"
    Then the breadcrumb shows "Projects, demo, Tables & views, toulouse"

  Scenario: 5. Rename a column from the grid
    When I rename the "city" column to "ville" from the table content
    Then the grid shows the "ville" column and a "Rename columns" step
    When I rename the "first_name" column to "prenom"
    Then the grid shows the "prenom" column and a "Rename columns" step
    And there is a single "Rename columns" step

  Scenario: 6. Collapse the transformation steps panel
    When I open the table content
    And I collapse the transformation steps panel
    Then the grid stays visible without the steps panel
    When I expand the transformation steps panel
    Then the transformation steps panel is visible again

  Scenario: 7. Searching returns to the first page
    Given the project has 11 extra tables
    When I open the project tables and views list
    And I go to the second page
    And I search the list for "e"
    Then the pagination shows "Page 1 of 2"

  Scenario: 8. Edit the last filter step from the steps list
    When I open the table content
    And I filter where "city" equals "Paris"
    Then the query builder shows a filter on "city" with value "Paris"
    When I change the filter value to "Toulouse" and update
    Then every visible row has city "Toulouse"
    And there is a single "Filter" step

  Scenario: 9. Only the last filter step can be edited
    When I open the table content
    And I filter where "city" equals "Paris"
    And I start a new filter
    And I add a filter where "first_name" equals "Camille"
    And I click on the "Filter" step
    Then the query builder is ready for a new filter

  Scenario: 10. Column operations keep merging on a saved view
    When I open the table content
    And I rename the "city" column to "ville"
    And I rename the "first_name" column to "prenom"
    And I save the table as view "renamed"
    And I rename the "last_name" column to "nom"
    Then the grid shows the "nom" column and a "Rename columns" step
    And there is a single "Rename columns" step
