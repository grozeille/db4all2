@epic:excel-journey
Feature: Excel tables journey
  As a data user
  I want to map Excel sheets, named ranges or custom ranges to tables
  So that I can explore spreadsheet data

  Background:
    Given I am logged in as "mathias.kluba@gmail.com" with password "qqQQ11!!"
    And the project "demo" exists
    And the datasource "sample-data" points to the samples folder

  Scenario: 1. Create a table from an Excel sheet
    When I create an Excel table named "excel-sales" from "orders.xlsx" sheet "Orders" in datasource "sample-data"
    Then the table shows the "customer_email" column with 6 rows

  Scenario: 2. Create a table from an Excel named range
    When I create an Excel table named "excel-top-sales" from "orders.xlsx" named range "TopOrders" in datasource "sample-data"
    Then the table shows the "customer_email" column with 3 rows

  Scenario: 3. Create a table from an Excel custom range
    When I create an Excel table named "excel-custom-sales" from "orders.xlsx" range "Orders!A1:D4" in datasource "sample-data"
    Then the table shows the "customer_email" column with 3 rows

  Scenario: 4. Filter the Excel table data
    Given the Excel table "excel-sales" exists from "orders.xlsx" sheet "Orders" in datasource "sample-data"
    When I add a filter where "city" equals "Paris"
    Then the grid shows 3 rows
