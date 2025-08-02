# Specifications related to projects

### Create a project

As a user, in the "project list" view in /projects url, I can click on the "Create" button.
It will open a popup asking me for the project name.
If the name is already used, or empty, an error message will be displayed in red under the name field.
The popup has a Save/Cancel buton.
Cancel to close the popup and Save to create the project with this name.
Once the project created, the UI will redirect to the project's detail view /projects/{project_id} (the ID is returned by the API when the project is created).

### List and search projects

As a user, in the "project list" view in /projects url, I can list all the projects.
They are displayed as cards with the name of the project (truncated to the first 10 characters ending with ...).
It also dislpays in gray a description of the project (truncated to 120 characters ending with ...).

It displays the first 25 projects with 2 buttons in the bottom: previous page and next page (grayed if we are on the first/last page).
In the Search textbox, I can search for a project: after 2 sec without typing any character, it will apply the filter (calling the API) and displays the result.

### Open a project

As a user, in the "project list" view, I can click on a project to open its detail view with the url /projects/{project_id}/tables.
If I enter a bad project_id (API result 404), I'll have a page with the message "Invalid project id".
If I enter a project_id on which I don't have permission (403), I'll have a page with the message "You don't have permission to access this project".
If I have the permission, it will display the name of the project as a header on the top left of the page with 2 tabs: Tables (/projects/{project_id}/tables) and Settings (/projects/{project_id}/settings).
Settings tab is displayed only if I'm aministrator of the project (API return "administrator: true" in the response).

For the "Tables" tab, specifications are explained in SPECS_TABLES.md.

### Rename a project and add description

In the settings tab, there's a section with a header "General".
In that section there's a textbox to rename the project, and a text area for the description.
There's a "save" button on the bottom of the section.
We'll have the same error messages in red under the textbox if the name is empty or already used.


### Delete a project

In the bottom of the page, there's a header "Danger zone" in red.
In that section, there's a "delete" button: when hitting the button, it will ask the confirmation with a popup asking me to input in a textbox the name of the project. I can click "cancel" to close the popup, or click "delete" (grayed if the name is not correct in the textbox).
Once deleted, it redirects to the /projects page.


### Grant permission to a project

Under the description, there's a section with a header "Permissions".
It lists all members of the project in table, with the name, a select box with the current role, and a "remove" button.

On the top of the list, there's an "Add user" button. When we click on it, there's a popupwith a label "Name:" and a textbox for the name, with select box for the role and a "Add" and "Cancel" button.
Under the textbox, we'll display a red message if the user is already in the list, or the textbox is empty.

Using the select box in the list of users, we can change the permissions:
- Reader
- Writer
- Administrator
The current user can't change his role or remove him from the project.
That means, we always have at least one administrator: the user who created the project, or he has to grant administrator access to another user which can remove him from the project.

Add or Remove or change the role will call the API POST/DELETE /projects/{project_id}/members/{user_id} so the effect is imediate. When the popup is closed, we call the API GET to /projects/{project_id}/members to refresh the list of members.

### Add a data-source to a project

Under the list of members, there's a data-sources section with a header "Data sources".
Similar to the members, there's a table listing all data sources, with the name and a "remove" button and also a "configure" button.

On top of the table, there's a "Add data source" button.
When we click on "Add data source" it displays a popup (with "Add" and "Cancel" button).
The popup let you choose first the type of data source with a select box: 
- CIFS folder
- S3 bucket
- PostgreSQL database

For CIFS, it asks for a path, a login and a password.
For S3 bucket, it asks for a hostname, a port, a path, a login and a password.
For CIFS and S3, I can select a checkbox "read-only" (true by default).
For PostgreSQL database, it asks for a hostname, a port, a database name, a login and a password, and a textbox to list keys/values with the syntax: key1=value;key2=value

It also ask me for a name of the data source.
When we click on "Add data source", it will call the API POST /project/{project_id}/datasources/{name} which will return 400 with an error message if we are not able to connect to the data source with these credentials.

