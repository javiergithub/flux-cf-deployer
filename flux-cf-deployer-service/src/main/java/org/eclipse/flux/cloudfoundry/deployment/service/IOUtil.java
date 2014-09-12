/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.cloudfoundry.deployment.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {

	/**
	 * Copy data from an inputstream into a file until end of the inputstream
	 * is reached.
	 * <p>
	 * The input stream is closed automatically.
	 */
	public static void pipe(InputStream data, File target) throws IOException {
		target.getParentFile().mkdirs(); //try to create dirs for parent if they don't exist.
		OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
		try {
			pipe(data, out);
		} finally {
			out.close();
		}
	}

	/**
	 * Copy input stream to output stream until end of the inputstream is reached.
	 * The intpustream is closed automatically, but the output stream is not.
	 */
	public static void pipe(InputStream input, OutputStream output) throws IOException {
		try {
		    byte[] buf = new byte[1024*4];
		    int n = input.read(buf);
		    while (n >= 0) {
		      output.write(buf, 0, n);
		      n = input.read(buf);
		    }
		    output.flush();
		} finally {
			input.close();
		}
	}

}
