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

import java.util.Map;

import org.w3c.dom.Node;

import com.apporiented.tools.pgmigration.Log;
import com.apporiented.tools.pgmigration.XPathWrapper;
import com.apporiented.tools.pgmigration.exceptions.MigrationException;

public abstract class AbstractJob implements Job {

	private XPathWrapper xpathWrapper;
	
	public void execute(Node config, Map<String, String> options) throws MigrationException {
		try {
			xpathWrapper = new XPathWrapper(config, options);
			String name = xpathWrapper.getText("@name");
			if (name == null) {
				name = getClass().getSimpleName();
			}
			Log.headLn(name);
			doExecute();
		} catch (MigrationException e) {
			throw e;
		} catch (Exception e) {
			throw new MigrationException("Unexpected exception", e);
		}
	
	}
	
	protected abstract void doExecute() throws Exception;
	

	protected XPathWrapper getXPath() {
		return xpathWrapper;
	}

}
