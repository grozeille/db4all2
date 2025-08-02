# Specifications of the project

## Goal

The project allows users to manipulate data without technical skills, with a no-code interface.

The user can map an external data, CSV/Excel files or SQL tables, to a "Table" in the project, to browse or manipulate.

"Tables" are grouped into "Projects", visibility and permissions are managed at the project's level.

Onced external data are mapped to "tables", user can manipulate data as applying filters, select columns, add custom columns or even join two tables, and save the result as a new "table".

## General information

### UI design

Content of each page are taking all the space, from left to right.
It's using Bootstrap for visual components.

On the top, there's a header which takes all width.
On the top right, there's a drop-down menu to go to personal settings, or sign-out.
When the user is super-administrator, there's also an additional menu "Admin."

When the user has a 403 or 404 or 500 error, he's redirected to a generic error page displaying an error message.

On the top left, there's a breadcrumb. Ex: Projects / project1 / Tables / table1
The user can click on each element to go back to the previous page.

The hierarchy of the website is:

- Authentication Page
- Error page
- User settings
- Admin. page
- Project List
  - Project creation page
  - Project settings
  - Table list
    - Table creation page
    - Table content
    - Table settings

### Security

There's 2 authentication mode:
- login/password
- SSO with Google or Microsoft

If not authenticated, the user is redirected to the authentication screen.
When authenticated from the authentication screen, the user is redirected to the project list.

The user can see the projects/tables based on his role of the projects (admin, write, read).

There's also a "super admin" role. The user for this role can manage login/password, and can be admin role for all projects.

## User Stories

Check SPECS_PROJECTS.md and SPECS_TABLES.md
