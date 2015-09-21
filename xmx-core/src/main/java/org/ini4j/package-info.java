/**
 * Enhancements to Ini4J related to better handling of comments, including
 * multi-comments (separated by empty lines), ending section comments etc.
 * <p/>
 * Have to use org.ini4j package for access to package-private methods
 * <p/>
 * In order to use, one needs to create Ini in a special way, like this:
 * <pre>
 *  static {
 *		System.setProperty(IniBuilder.class.getName(), EnhancedIniBuilder.class.getName());
 *		System.setProperty(IniFormatter.class.getName(), EnhancedIniFormatter.class.getName());
 *	}
 *  ...	
 *	
 *		Ini ini = new Ini();
 *		ini.setConfig(new EnhancedIniConfig(ini));
 * 
 * </pre>
 * 
 * @author Andrey Mogilev
 */
package org.ini4j;
