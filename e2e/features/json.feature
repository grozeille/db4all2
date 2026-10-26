@epic:json-journey
Feature: JSON tables journey
  As a data user
  I want to map JSON files to tables, optionally through a JSON path
  So that I can explore nested JSON data

  Background:
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    And the project "demo" exists
    And the datasource "sample-data" points to the samples folder

  Scenario: 1. Create a table from a nested JSON array
    When I create a JSON table named "orders" from "orders.json" with path "$.orders" in datasource "sample-data"
    Then the table shows data with the "customer_email" column

  Scenario: 2. Create a table from a root JSON array
    When I create a JSON table named "products" from "products.json" in datasource "sample-data"
    Then the table shows the "product_id" column with 3 rows

  Scenario: 3. Filter the JSON table data
    Given the JSON table "orders" exists from "orders.json" with path "$.orders" in datasource "sample-data"
    When I add a filter where "city" equals "Paris"
    Then the grid shows 2 rows
