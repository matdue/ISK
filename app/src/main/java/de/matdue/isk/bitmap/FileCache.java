/**
 * Copyright 2012 Matthias Düsterhöft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.matdue.isk.bitmap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileCache {
	
	private File cacheDir;
	private static final long MAXIMUM_AGE = 30l * 24l * 60l * 60l * 1000l;  // 30 days in milliseconds
	
	public FileCache(File cacheDir) {
		this.cacheDir = cacheDir;
	}
	
	public File getFile(String url) {
		String md5HashTag = createMd5Hash(url);
		File cachedFile = new File(cacheDir, md5HashTag);
		
		// Check if file exists
		if (!cachedFile.exists()) {
			return null; 
		}
		
		// Check if expired
		long expires = System.currentTimeMillis() - MAXIMUM_AGE;
		if (cachedFile.lastModified() < expires) {
			cachedFile.delete();
			return null;
		}
		
		// Return cached file
		return cachedFile;
	}
	
	public InputStream getStream(String url) {
		File cachedFile = getFile(url);
		if (cachedFile != null) {
			try {
				return new BufferedInputStream(new FileInputStream(cachedFile));
			} catch (FileNotFoundException e) {
				// Ignore exceptions, simulate cached file not available to caller
			}
		}
		
		return null;
	}
	
	public InputStream storeStream(String url, InputStream stream, boolean returnCachedStream) {
		String md5HashTag = createMd5Hash(url);
		File cachedFile = new File(cacheDir, md5HashTag);
		try {
			FileOutputStream cacheFileStream = new FileOutputStream(cachedFile);
			
			// Copy content
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = stream.read(buffer)) > 0) {
				cacheFileStream.write(buffer, 0, bytesRead);
			}
			cacheFileStream.flush();
			cacheFileStream.close();
			
			// Return a stream to new file if requested
			if (returnCachedStream) {
				return new BufferedInputStream(new FileInputStream(cachedFile));
			}
		} catch (Exception e) {
			// Ignore exceptions
			// Delete cache file as it may be incomplete
			cachedFile.delete();
		}
		
		return null;
	}
	
	public void cleanup() {
		long expires = System.currentTimeMillis() - MAXIMUM_AGE;
		File[] cachedFiles = cacheDir.listFiles();
		for (File cachedFile : cachedFiles) {
			if (cachedFile.isFile() && cachedFile.lastModified() < expires) {
				cachedFile.delete();
			}
		}
	}
	
	private String createMd5Hash(String url) {
		String md5HashTag;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(url.getBytes());
			BigInteger md5 = new BigInteger(1, digest.digest());
			md5HashTag = String.format("%1$032X", md5);
		} catch (NoSuchAlgorithmException e) {
			// Fallback String.hashCode()
			md5HashTag = Integer.toHexString(url.hashCode());
		}
		
		return md5HashTag;
	}

}
