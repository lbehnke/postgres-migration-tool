/*******************************************************************************
 * Copyright 2013 Lars Behnke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.apporiented.tools.pgmigration.jobs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import com.apporiented.tools.pgmigration.Log;
import com.apporiented.tools.pgmigration.exceptions.MigrationException;


public class SqlTemplateJob extends AbstractJob {
	
	private static Pattern placeHolderPattern = Pattern.compile("%\\{(.+)\\}");
	
	@Override
	protected void doExecute() throws Exception {
	    String jdbcDriver = getXPath().getText("jdbc-driver", "org.postgresql.Driver");
		Class.forName(jdbcDriver);
		String jdbcUrl = getXPath().getText("jdbc-url");
		String jdbcUser = getXPath().getText("jdbc-user");
		String jdbcPassword = getXPath().getText("jdbc-password");
		jdbcPassword = jdbcPassword == "" ? null : jdbcPassword;
		Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
		
		try {
			
		    String pgVersion = getPostgresVersion(conn);
		    
			Map<String, List<String>> selectors = queryObjectSelectors(conn);
			
			List<String> templates = getXPath().getList("statements/sql");
			executeStatements(conn, selectors, templates, null);
			
            templates = getXPath().getList("conditional-statements[@version='" + pgVersion + "']/sql");
            if (templates.size() > 0)
            {
                Log.ln();
                executeStatements(conn, selectors, templates, "Statements specific for database version " + pgVersion);
            }
            
			
	
		} catch (MigrationException e) {
			throw e;
		} catch (SQLException e) {
			SQLException e2 = e;
			while (e2 != null) {
				Log.ln("SQL-Exception: " + e2);
				e2 = e2.getNextException();
			}
			throw new MigrationException("SQL job failed.", e);
		} finally {
			conn.close();
		}
		conn.close();
	}

    private String getPostgresVersion(Connection conn) throws SQLException
    {
        DatabaseMetaData metadata = conn.getMetaData();
        String pgVersion = metadata.getDatabaseProductVersion();
        
        /* Keep major and minor version */
        if (pgVersion != null)
        {
            String[] parts = pgVersion.split("\\.");
            if (parts.length > 1)
            {
                pgVersion = parts[0] + "." + parts[1];
            }
            
        }
        return pgVersion;
    }

    private void executeStatements(Connection conn, Map<String, List<String>> selectors, List<String> templates, String hint)
            throws SQLException
    {
        for (String template : templates) {
        	
        	/* Only one selector permitted per statement */
        	String selectorName = getSelectorName(template);
        	Statement stmt = conn.createStatement();
        	int counter = 0;
        	List<String> objects = selectors.get(selectorName);
        	if (objects == null) {
        		stmt.addBatch(template);
        		Log.ln(template);
        	} else {
        	    if (hint != null)
        	    {
        	        Log.ln(hint);
        	    }
        		Log.ln("Preparing SQL batch job for template " + template + " and selector " + selectorName);
        		for (String object : objects) {
        			String sql = template.replaceAll("%\\{" + selectorName + "\\}", object).trim();
        			stmt.addBatch(sql);
        			counter++;
        			Log.ln("(" + counter + ") " + sql);
        		}
        	}
        	
        	int[] results = stmt.executeBatch();
        	stmt.close();
        	for (int i = 0; i < results.length; i++) {
        		if (results[i] < 0) {
        			throw new MigrationException("SQL statement " + (i + 1) + " failed.", null);
        		}
        	}	
        }
    }

	private String getSelectorName(String template) {
		String result = null;
		Matcher matcher = placeHolderPattern.matcher(template);
		while (matcher.find()) {

			String name = matcher.group(1);
			if (result != null && !result.equals(name)) {
				throw new MigrationException("Multiple selectors are not allowed: " + template, null);
			}
			result = name;
			
		}
		return result;
	}

	private Map<String, List<String>> queryObjectSelectors(Connection conn)
			throws XPathExpressionException, SQLException {
		Map<String, List<String>> selectors = new HashMap<String, List<String>>();
		Map<String, String> objectSelectorQueries = getXPath().getMap("object-selectors/object-selector");
		for (Map.Entry<String, String> entry : objectSelectorQueries.entrySet()) {
			String objectSelector = entry.getValue();
			String key = entry.getKey();
			List<String> objects = new ArrayList<String>();
			if (objectSelector != null) {
				objects = new ArrayList<String>();
				Statement stmt = conn.createStatement();
				stmt.execute(objectSelector.trim());
				ResultSet results = stmt.getResultSet();
				while (results.next()) {
					String s = results.getString(1);
					if (s != null) {
						objects.add(s);
					}
				}
				stmt.close();
				selectors.put(key, objects);
			}				
		}
		return selectors;
	}


}
