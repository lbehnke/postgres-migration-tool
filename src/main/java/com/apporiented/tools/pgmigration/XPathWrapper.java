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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathWrapper {

	private static XPathFactory xpathFactory = XPathFactory.newInstance();
	
	private XPath xpath;
	
	private Node config;
	
	private Map<String, String> options = new HashMap<String, String>();
	
	public XPathWrapper(Node config, Map<String, String> options) {
		this.config = config;
		if (options != null) {
			this.options.putAll(options);
		}
	}
	
	private  XPath getXPath() {
		if (xpath == null) {
			xpath = xpathFactory.newXPath();
		}
		return xpath;
	}
	
	public void addOptions(Map<String, String> newOptions) {
		options.putAll(newOptions);
	}
	
	public String getText(String xpath) throws XPathExpressionException {
		return getText(xpath, null);
	}
	
	public  String getText(String xpath, String defaultValue) throws XPathExpressionException  {
		String result = (String) getXPath().evaluate(xpath, config, XPathConstants.STRING);
		result = replacePlaceholders(result);
		return result == null || result.length() == 0 ? defaultValue : result;
	}
	
	public  NodeList getNodeList(String xpath) throws XPathExpressionException  {
		return (NodeList) getXPath().evaluate(xpath, config, XPathConstants.NODESET);
	}
	
	public  Map<String, String> getMap(String xpath) throws XPathExpressionException  {
		Map<String, String> result = new HashMap<String, String>();
		NodeList nodeList = (NodeList) getXPath().evaluate(xpath, config, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String key = getXPath().evaluate("@key", node);
			String value = replacePlaceholders(node.getTextContent());
			result.put(key, value);
		}
		return result;
	}
	
	public  List<String> getList(String xpath) throws XPathExpressionException  {
		List<String> result = new ArrayList<String>();
		NodeList nodeList = (NodeList) getXPath().evaluate(xpath, config, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String s = replacePlaceholders(node.getTextContent());
			result.add(s);
		}
		return result;
	}
	public  boolean getBoolean(String xpath, String def) throws XPathExpressionException {
		String s = getText(xpath, def);
		s = replacePlaceholders(s);
		return Boolean.parseBoolean(s);
	}
	
	
	public String replacePlaceholders(String txt) {
		if (txt != null && txt.length() > 0) {
			for (Map.Entry<String, String> option : options.entrySet()) {
				String key = option.getKey();
				String value = option.getValue();
				key.replaceAll("\\.", "\\\\.");
				txt = txt.replaceAll("\\$\\{" + key + "\\}", value);
			}
		}
		return txt;
	}
	
}
