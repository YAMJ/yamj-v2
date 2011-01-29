When adding or changing a column to one of the Data Type Objects:
1) Update tools/DatabaseTools/createTables
2) Update dto/CLASS and add a getter/setter and update the constuctor
3) Update dbWriter/INSERT_??? field with the new column
4) Update dbWriter/insert??? with the new column