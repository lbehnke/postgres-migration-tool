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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import com.apporiented.tools.pgmigration.Log;
import com.apporiented.tools.pgmigration.exceptions.MigrationException;

public class PgMigrationJob extends AbstractJob {


	/**
	 * Example:
	 * <pre>
	 * 		pg_dump -U user -h localhost -f db.dump -Ft -n public db
	 *		pg_restore -U user -h localhost -d db -Ft -c db.dump
	 * </pre>
	 */
	public void doExecute() throws Exception {
		try {
			String fileName = getXPath().getText("dump-file");
			File dumpFile;
			if (fileName != null) {
				dumpFile = new File(fileName);
			} else {
				dumpFile = File.createTempFile("pg-migration-", ".dump");
			}
			runDump(dumpFile);
			runRestore(dumpFile);
			cleanup(dumpFile);
		} catch (XPathExpressionException e) {
			throw new MigrationException("Migration job failed.", e);
		}

	}

	private void cleanup(File dumpFile) throws XPathExpressionException {
		String kdf = getXPath().getText("keep-dump-file");
		boolean keepDumpFile = kdf == null || Boolean.parseBoolean(kdf);
		if (!keepDumpFile) {
			dumpFile.delete();
		}
	}

	private void runDump(File dumpFile) throws Exception {
		Log.ln("Exporting to " + dumpFile + "...");
		String toolsDir = getXPath().getText("tools-dir", "");
		String pgDump = isWindows() ? "pg_dump.exe" : "pg_dump";
		String dumpDb = getXPath().getText("dump/db", null);
		if (dumpDb == null) {
			throw new MigrationException("No dump database specified.", null);
		}
		
		List<String> params = new ArrayList<String>();
		addParam(params, "-U", getXPath().getText("dump/user", null));
		addParam(params, "-h", getXPath().getText("dump/host", "localhost"));
		addParam(params, "-n", getXPath().getText("dump/schema", null));
		addParam(params, "-F", getXPath().getText("dump/format", "t"));
		if (getXPath().getBoolean("dump/verbose", "true")) {
			addParam(params, "-v");
		}
        if (!isWindows()) {
            addParam(params, "-w");
        }
		addParam(params, "-f", dumpFile.getPath());
		addParam(params, "", dumpDb);
		
		String cmd = toolsDir == null || toolsDir.length() == 0 ? pgDump : toolsDir + File.separatorChar + pgDump;
		runProcess(cmd, params);
		
		Log.ln("Dump file created. Size: " + (dumpFile.length() / 1000) + " kb");

	}



	private void runRestore(File dumpFile) throws Exception {
        String dumpDb = getXPath().getText("restore/db", null);
        if (dumpDb == null) {
            Log.ln("No restore database specified.");
            return;
        }
	    Log.ln("Restoring from " + dumpFile + "...");
		String toolsDir = getXPath().getText("tools-dir", "");
		String pgRestore = isWindows() ? "pg_restore.exe" : "pg_restore";
		
		List<String> params = new ArrayList<String>();
		addParam(params, "-U", getXPath().getText("restore/user", null));
		addParam(params, "-h", getXPath().getText("restore/host", "localhost"));
		addParam(params, "-n", getXPath().getText("restore/schema", null));
		addParam(params, "-d", getXPath().getText("restore/db", null));
		addParam(params, "-F", getXPath().getText("dump/format", "t"));
		if (getXPath().getBoolean("dump/verbose", "true")) {
			addParam(params, "-v");
		}
        if (!isWindows()) {
            addParam(params, "-w");
        }
	

		addParam(params, dumpFile.getPath());
		
		String cmd = toolsDir == null || toolsDir.length() == 0 ? pgRestore : toolsDir + File.separatorChar + pgRestore;
		runProcess(cmd, params);
		
	}
	
	private boolean isWindows() {
		String os = System.getProperty("os.name");
		return (os != null && os.toLowerCase().contains("windows"));
	}
	
    private void addParam(List<String> params, String key,
			String value) {
		if (params != null && key != null && value != null && value.length() > 0) {
			params.add((key + "" + value).trim());
		}
	}

    private void addParam(List<String> params, String value) {
		if (params != null && value != null && value.length() > 0) {
			params.add(value);
		}
	}

	public void runProcess (String tool, List<String> params) throws Exception {
        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(tool);
        if (params != null) {
        	cmdLine.addAll(params);
        }
        logCmdLine(tool, params);
        ProcessBuilder procBuilder = new ProcessBuilder(cmdLine).redirectErrorStream(true);
        Process proc = procBuilder.start();
        Reader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		copy(reader, Log.getOut());
    }

    private void logCmdLine(String tool, List<String> params)
    {
        Log.ln("Executing " + tool + " with parameters ");
        for (String p : params)
        {
            Log.ln("  " + p);
        }
        Log.ln();
    }
	

	public int copy(Reader in, Writer out) throws IOException {
		try {
			int byteCount = 0;
			char[] buffer = new char[4096];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		finally {
			in.close();
		}
	}
	
	
}
