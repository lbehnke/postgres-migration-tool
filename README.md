Postgres Migration Tools
------------------------

This tool was created to facilitate modification or migration tasks across multiple Postgres databases.
See the attached example files to see how it works.

Requirements:
- Java 6
- Postgres tools (pg_dump / pg_restore) must be installed
	
Usage
-----

Print out system information:

	CMD> java -jar 	pg-migration-<version>.jar --system
	
Perform a migration using the passed descriptor file.

	CMD> java -jar 	pg-migration-<version>.jar biotyper-migration.xml
	
Perform a migration using the default descriptor file "migration.xml".

	CMD> java -jar 	pg-migration-<version>.jar
	
License
-------

All rights reserved. This program and the accompanying materials are made available under the terms of the GNU Public License v3.0
which accompanies this distribution, and is available at http://www.gnu.org/licenses/gpl.html.