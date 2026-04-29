# Specifications related to table manipulations

## Table List view

### List all tables

In the project tab with the url /projects/{project_id}/tables, the UI displays a list of tables using cards, limited by 25 first item with buttons to navigate to the pages.

It has also a search textbox, calling the API to apply the filter after 2sec after the last input key.

### Table detail view

When we click on a table, we go to the page /projects/{project_id}/tables/{table_id}/content

It displays a grid (AG Grid) with the data.

For the MVP, the grid shows at most 1000 rows per query preview.

On the top right, if I have writer/administrator role, I can see a button "Configure" and "Add" button.

There is also an "Export CSV" action to download the full result of the current query.

### Map CSV file to a table

When I click on "Add" button, the content of the table is replaced by a new screen.
I can provide a name and a type using a select box:
- CSV
- Parquet files

For the MVP, only CSV and Parquet files are available in this selector.

Excel and PostgreSQL mappings are postponed to a later version.

When I select CSV, I must choose a datasource from the list of datasources configured for the project.

I now have a section "configuration" with the folowing options:
- Path (textbox) Ex: *.csv, or subfolder/file.csv
- Separator (textbox)
- First row as header (checkbox)

If the data-source is not read-only, I can also click on a "upload" button to upload a file to the selected path, the path will be renamed to target the new file uploaded. A popup is displayed "please wait" to wait for the upload to be completed.

I can click on the botton of the screen on "Cancel" or "Save".
When "save", it will call the API POST /projects/{project_id}/tables which will return a JSON response with the id of the created table, so the UI can redirect to /projects/{project_id}/tables/{table_id}/content

When the user click on "Configure" button in the table view, it displays the same screen but can't change the type and data-source, but can change the name, the fields of the "configuration" section.

More details about datasource types and upload policy are defined in SPECS_DATASOURCES.md.

### Map Excel file to a table

Same as CSV, but the configuration section is different.
There's a button "scan" which will call the API POST /project/{project_id}/datasources/{name}/do-scan which will returns a list of tables/tabs of the excel file.
The user must then select a tab/table using a select box.

### Configure a PostgreSQL table to a table

Same as CSV, but the configuration section is different.
There's a button "scan" which will call the API POST /project/{project_id}/datasources/{name}/do-scan which will returns a list of tables of the database.
The user must then select a table using a select box.

### Apply filters to a table

On top of the table content, there's a collopsed section "Filter"
When we click on it to expend it, it displays a query builder.
I can add a new filter by clicking "add a condition".
I can select a column (using a select box) of all columns of the data (not only the ones hidden). I have a nother select box for the operator: it will be a different list based on the type of the column.
For String:
- Begins with, ends with, content, equals, Not begins with, Not ends with, Not content, Not equals.
For numeric or date:
- >, >=, <, <=, !=, =
For boolean:
- is true, is false (in these cases, the text box for the value is grayed)
After selecting an operation, we must input a value in a text box.

Next to the "Add a condition" button, we can also clic on "Add a group".
On the left of "Add a condition" we have a select box to choose "And" or "Or", to use that AND/OR between all conditions of the group. There's also a "remove" button to remove a condition or a group.

On the top right, next to "Configure" button, there's a "Refresh" button: it will refresh the data and apply the filter.

If the table uses a cache, the Refresh button refreshes the displayed data from the current active cache or source. Cache refresh scheduling and cache invalidation are configured in the table settings.

The preview grid does not display a total result count and does not rely on a hasMore indicator.

### Apply aggregation to a table

Similar to filters, there's a "aggregation" section to expend below the "filter" section.
There's a sub-section "aggregated by" with a button "add grouping column".
When we click on this "add grouping column' button, it adds to the begining of a list a select-box to select a column and a "remove" button at the end.
Of course, we can't add twice the same column, the select-box displays all columns not already in the grouping list.

There's a sub-section "aggregation function" with a button "add function".
It adds a new select-box in the top of the list to choose a columns, another select-box to choose the aggregation method (sum, count, min, max, avg), and there's a "remove" button.

The "Refresh" button is applying the aggregation passed to the API to fetch the content.

### Hide/Show/Rename columns of a table

There's another section "Hide/Rename columns".
Similar to aggregation, there's 2 sub-sections:
- hide
- rename

In hide section, we can select columns to hide with a select-box.

In rename section, we can select a column, and there's a text-box to put the new name.

### Add custom column using arythmetic operators

There's another section "Custom columns".
Similarly to the other sections, we have a button "add" which will add a new line, which contains a "remove" button.
First, the user must choose a type of custom function with select box, and the name of the column in another text-box.
When selected "Arythmetic", it will display sub-section below, specific to this type of custom column.
For Arythmetic, the user must input a syntax like this:
"col1" + "col2" * "col3" / ("col4" - "col5")
After inputing the value, a call to the API will verify the syntax.

### Add custom column using SWITCH CASE

In the same "custom columns" section, when the user choose the "Conditional" custom function, it will display a custom section below.
That section is very similar to the filter. We can click on "Add condition".
It will add to the top of the list a label "value:" with a textbox to input the value.


### Save transformations as a new table

Next to the "Configure" button, there's also a "Save" button. It will open a popup to enter the name of the dataset (red error text if empty or already used) and a Save/Cancel button.
If saved, it will redirect to the new table.
If the "Configure" section, it will displays the same configuration screen as for a CSV/Excel/etc. table, except the "Configuration" section displays a field "Parent table:" with a hyperlink with the name of the parent table.
After that, we can see the "filter" section with the saved filters.
And the other sections (aggregation, columns, custom columns, etc.)

And bellow, we can see the grid with the content.

When we click "save" it will save our filters.

This saved result is considered a logical view unless the user explicitly materializes it as a new physical table.

Views can save:
- filters
- select/hide columns
- rename columns
- joins between 2 sources

Views do not have their own cache.

More details about views are defined in SPECS_DATASOURCES.md.

If the user needs a full extract of the result, the UI uses a dedicated CSV export endpoint rather than loading the full result in the browser.

### Delete a table

In the "Configure" screen, below the name, I have a "danger" section with a button "delete".
When we click on it, it will ask me confirmation with a popup to input the name of the table.
