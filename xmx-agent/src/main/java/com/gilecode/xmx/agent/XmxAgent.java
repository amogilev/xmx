// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.agent;

import com.gilecode.xmx.boot.XmxProxy;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XmxAgent {
	
	private static final String XMX_HOME_PROP = "xmx.home.dir";
	private static final String XMX_TEMP_HOME_NAME = "xmx_temp_home";
	private static final String XMX_DISTR_RES = "xmx-distribution.zip";

	// use: -javaagent:path\xmx-agent.jar[=override_props]
	// override_props::=override_prop[,override_prop]*
	// override_prop::=name=val
	// no spaces in override_props, names are case-insensitive.
	// Examples:
	// -javaagent:path\xmx-agent.jar=enabled=false
	// -javaagent:path\xmx-agent.jar=enabled=true,EmbeddedWebServer.enabled=false
	// -javaagent:path\xmx-agent.jar=enabled=true,embeddedwebserver.enabled=true,EmbeddedWebServer.Port=8082
	
	public static void premain(String agentArgs, Instrumentation instr) {
		Map<String,String> agentProperties = parseArguments(agentArgs);
		if ("false".equalsIgnoreCase(agentProperties.get("enabled"))) {
			// fast path of disabling XMX
			return;
		}
		
		// TODO: support pre-set XMX_HOME dir
		
		long copyStart = System.currentTimeMillis();
		
		try {
				
			File agentHomeDir;
			
			// check if agent is ran from xmx-agent-all.jar (with integrated distr)
			InputStream distrImageStream = XmxAgent.class.getClassLoader().getResourceAsStream(XMX_DISTR_RES);
			if (distrImageStream != null) {
				try (ZipInputStream distrImageZip = new ZipInputStream(new BufferedInputStream(distrImageStream))) {
					agentHomeDir = new File(System.getProperty("java.io.tmpdir") + File.separator + XMX_TEMP_HOME_NAME);
					
					// in order to prevent versioning and corruption problems, try delete previous if exists
					if ((agentHomeDir.exists() && !deleteRecursively(agentHomeDir)) || !agentHomeDir.mkdir()) {
						// if deletion or creation failed - use new temp folder instead
						agentHomeDir = Files.createTempDirectory(XMX_TEMP_HOME_NAME).toFile();
					}
					
					copyContents(distrImageZip, agentHomeDir);
//					double delta = (System.currentTimeMillis() - copyStart) / 1000.0;
//					System.err.println("XMX distribution extraction completed in " + delta + " sec");
				}
			} else {
				// normal xmx-agent.jar run, check if run in XMX_HOME/bin
				URL jarLocation = XmxAgent.class.getProtectionDomain().getCodeSource().getLocation();
				File agentBaseDir = new File(jarLocation.toURI()).getParentFile();
				agentHomeDir = agentBaseDir.getParentFile();
			}
				

			File agentLibDir = new File(agentHomeDir, "lib");
			
			// find xmx-boot.jar, support optional version
			File[] xmxBootFiles = agentLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase(Locale.ENGLISH);
					return name.equals("xmx-boot.jar") || (name.startsWith("xmx-boot-") && name.endsWith(".jar"));
				}
			});
			
			if (xmxBootFiles == null || xmxBootFiles.length == 0 || !xmxBootFiles[0].isFile()) {
				throw new RuntimeException("Failed to determine proper XMX home directory. Please make sure that xmx-agent.jar resides in XMX_HOME/bin");
			}
			
			System.setProperty(XMX_HOME_PROP, agentHomeDir.getAbsolutePath());
			instr.appendToBootstrapClassLoaderSearch(new JarFile(xmxBootFiles[0]));
			
			// from this moment we can use xmx-boot classes
			boolean success = initializeLoader(agentProperties);
			if (success) {
				// initialize transformer
				instr.addTransformer(new XmxClassTransformer());
			}
		} catch (Exception e) {
			System.err.println("Failed to start XmxAgent");
			e.printStackTrace();
		}
	}

	private static boolean initializeLoader(Map<String, String> agentProperties) {
		// no additional errors required if failed or disabled - all printed by XmxProxy
		return XmxProxy.initialize(agentProperties);
	}

	private static Map<String, String> parseArguments(String agentArgs) {
		
		if (agentArgs == null || agentArgs.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, String> result = new HashMap<>();
		for (String agentArg : agentArgs.split(",")) {
			int i = agentArg.indexOf('=');
			String key = (i < 0 ? agentArg : agentArg.substring(0, i)).trim().toLowerCase(Locale.ENGLISH);
			String val = i < 0 ? "" : agentArg.substring(i + 1).trim();
			result.put(key, val);
		}
		
		return result;
	}

	private static boolean deleteRecursively(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;			
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;			
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;			
			}
		});
		
		return !dir.exists();
	}

	// TODO support starting agent after VM startup
	public static void agentmain(String agentArgs, Instrumentation instr) {
		System.err.println("NOT IMPLEMENTED YET: XmxAgent agentmain");
	}
	
	/**
	 * Copies all file entries from ZIP archive to the specified directory. 
	 * 
	 * @param zipStream the source of files as ZIP stream
	 * @param dir the target directory
	 * 
	 * @throws IOException if I/O exception occurred file reading or writing 
	 */
	private static void copyContents(ZipInputStream zipStream, File dir) throws IOException {
		ZipEntry nextEntry;
		// TODO: check whether Maven archiver on Linux use "\" or  "/" as separators in entry names
		byte[] buf = new byte[8192];
		while ((nextEntry = zipStream.getNextEntry()) != null) {
			String name = nextEntry.getName();
			if (!name.endsWith("/")) { // if not a folder
				File targetFile = new File(dir, name.replace('/', File.separatorChar));
				targetFile.getParentFile().mkdirs();
				try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile))) {
					copyStreams(zipStream, out, buf);
				}
			}
		}
	}

	private static void copyStreams(InputStream in, OutputStream out, byte[] buf) throws IOException {
		int read = 0;
		while ((read = in.read(buf)) > 0) {
			out.write(buf, 0, read);
		}
	}
}
