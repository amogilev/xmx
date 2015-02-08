package am.xmx.agent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XmxAgent {
	
	private static final String XMX_HOME_PROP = "xmx.home.dir";
	private static final String XMX_TEMP_HOME_NAME = "xmx_temp_home";
	private static final String XMX_DISTR_RES = "xmx-distribution.zip";

	public static void premain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent premain");
		
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
					double delta = (System.currentTimeMillis() - copyStart) / 1000.0;
					System.err.println("XMX distribution extraction completed in " + delta + " sec");
				}
			} else {
				// normal xmx-agent.jar run, check if run in XMX_HOME/bin
				URL jarLocation = XmxAgent.class.getProtectionDomain().getCodeSource().getLocation();
				File agentBaseDir = new File(jarLocation.toURI()).getParentFile();
				agentHomeDir = agentBaseDir.getParentFile();
			}
				

			File agentLibDir = new File(agentHomeDir, "lib");
			
			// find xmx-api.jar, support optional version 
			File[] xmxApiFiles = agentLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase(Locale.ENGLISH);
					return name.equals("xmx-api.jar") || (name.startsWith("xmx-api-") && name.endsWith(".jar"));
				}
			});
			
			if (xmxApiFiles == null || xmxApiFiles.length == 0 || !xmxApiFiles[0].isFile()) {
				throw new RuntimeException("Failed to determine proper XMX home directory. Please make sure that xmx-agent.jar resides in XMX_HOME/bin");
			}
			
			System.setProperty(XMX_HOME_PROP, agentHomeDir.getAbsolutePath());
			instr.appendToBootstrapClassLoaderSearch(new JarFile(xmxApiFiles[0]));
			instr.addTransformer(new XmxClassTransformer());
			
		} catch (Exception e) {
			System.err.println("Failed to start XmxAgent");
			e.printStackTrace();
		}
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
		System.err.println("XmxAgent agentmain");
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
