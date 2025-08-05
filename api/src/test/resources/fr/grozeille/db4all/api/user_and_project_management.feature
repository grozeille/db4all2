Feature: User and Project Management Flows
  As an administrator and a user, I want to ensure the full user lifecycle and project management works correctly.

  Scenario: A user is created, manages projects, and manages tables
    Given the system is set up with an initial super admin "admin@test.com"
    And I am logged in as the super admin "admin@test.com"
    When I create a new user with email "newuser@test.com" and password "a-secure-password"
    Then the user "newuser@test.com" is created successfully

    When I, as an admin, change the password for "newuser@test.com" to "a-new-secure-password"
    Then the password change is successful

    # --- Switch to acting as the new user ---
    Given I am logged in as the new user "newuser@test.com"
    When I create a new project named "My First Project"
    Then the project "My First Project" is created successfully
    And I see 1 project when I list all projects

    When I create a new table named "My First Table" for this project
    Then the table "My First Table" is created successfully
    And I see 1 table when I list all tables for this project
