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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public final class Log {

	public static Writer out = new PrintWriter(System.out); 
	
	public static void setOut(Writer writer) {
		out = writer;
	}
	
	public static Writer getOut() {
		return out;
	}
	
	public static void ln() {
		ln("");
	}

	public static void ln(String txt) {
		try {
			out.write(txt + "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void headLn(String txt) {
		ln();
		ln("================================================");
		ln(txt.toUpperCase());
		ln("================================================");
		
	}

}
