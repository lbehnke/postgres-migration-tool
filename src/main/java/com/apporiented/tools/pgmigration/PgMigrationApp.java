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

package com.apporiented.tools.pgmigration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.apporiented.tools.pgmigration.exceptions.MigrationException;
import com.apporiented.tools.pgmigration.jobs.Job;

public class PgMigrationApp {

	private Properties jobMapping;

	private XPathWrapper config;

	public PgMigrationApp(String fileName) {
		try {
			File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(is);
			config = new XPathWrapper(doc, null);
			jobMapping = createJobMapping();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Properties createJobMapping() {
		Properties mapping = new Properties();
		try {
			mapping.load(PgMigrationApp.class.getResourceAsStream("/job.mapping"));
		} catch (IOException e) {
			Log.ln("Job mapping not found.");
		}
		return mapping;
	}

	public void execute() throws Exception {
		NodeList nodeList = (NodeList) config.getNodeList("/migration/jobs/*");
		FileWriter logWriter = createLogWriter();
		Log.headLn("Migration process started\nDate: " + new Date() + "\nNumber of jobs: " + nodeList.getLength());

		Map<String, String> globalOptions = new HashMap<String, String>();
		Map<String, String> sysOptions = readSystemProperties();
		Map<String, String> fileOptions = config.getMap("/migration/options/option");
		globalOptions.putAll(sysOptions);
		globalOptions.putAll(fileOptions);
        
		config.addOptions(globalOptions);
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String jobClass = jobMapping.getProperty(node.getNodeName());
			if (jobClass == null) {
				throw new MigrationException("Unknown job: " + node.getNodeName(), null);
			}
			Job job = (Job)Class.forName(jobClass).newInstance();
			job.execute(node, globalOptions);
		}
		Log.headLn("All Done.");
		
		if (logWriter != null) {
			logWriter.close();
		}
	}

    private Map<String, String> readSystemProperties()
    {
        Map<String, String> sysOptions = new HashMap<String, String>();
		for (Object prop : System.getProperties().keySet())
        {
            String val = System.getProperty(prop.toString());
            sysOptions.put(prop.toString(), val);
        }
        return sysOptions;
    }

	private FileWriter createLogWriter()
			throws XPathExpressionException, IOException {
		FileWriter logWriter = null;
		String logFileName = config.getText("/migration/log-file");
		if (logFileName != null && logFileName.trim().length() > 0) {
			File logFile = new File(logFileName);
			if (logFile.exists()) {
				logFile.delete();
			}
			System.out.println("Writing output to " + logFile);
			logWriter = new FileWriter(logFile);
			Log.setOut(logWriter);
		}
		return logWriter;
	}

	public static void main(String[] args) throws Exception {

		if (args.length > 0 && args[0].toLowerCase().equals("--system")) {
			logSystemData();
			logManifestData();
			Log.ln();
		} else {
			String confName = args.length == 0 ? "migration.xml" : args[0];
			PgMigrationApp app = new PgMigrationApp(confName);
			app.execute();
		}
	}

	private static void logSystemData() {
		Set<Object> keys = System.getProperties().keySet();
		Log.headLn("System properties");
		for (Object key : keys) {
			String val = System.getProperty(key.toString());
			Log.ln(key + " : " + val);
		}
	}

	private static void logManifestData() throws IOException {
		InputStream is = PgMigrationApp.class.getResourceAsStream("/META-INF/MANIFEST.MF");
		if (is != null) {
			Log.headLn("Manifest");
			Manifest mf = new Manifest();
			mf.read(is);
			Attributes main = mf.getMainAttributes();
			Log.ln("Implemation title: " +  main.getValue(Attributes.Name.IMPLEMENTATION_TITLE));
			Log.ln("Implemation version: " +  main.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
			Log.ln("Class path: " +  main.getValue(Attributes.Name.CLASS_PATH));
		}
	}
}
