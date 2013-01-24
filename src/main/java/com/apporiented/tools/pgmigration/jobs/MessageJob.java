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

import java.util.Scanner;

import com.apporiented.tools.pgmigration.Log;
import com.apporiented.tools.pgmigration.exceptions.MigrationException;


public class MessageJob extends AbstractJob {
	
	@Override
	protected void doExecute() throws Exception {
		String text = getXPath().getText("text", "");
		boolean confirmationRequired = Boolean.parseBoolean(getXPath().getText("confirmation-required"));

		try {
			Log.ln(text);
			if (confirmationRequired)
			{
			    Log.ln("Do you want to continue? [y/n]");
			    Scanner sc = new Scanner(System.in);
			    String l = sc.nextLine();
			    if (l != null && !l.equalsIgnoreCase("y"))
	            {
			        Log.ln("Migration aborted by user.");
			        System.exit(1);
	            }
			}
	
		} catch (MigrationException e) {
			throw e;
		}
	}
}
