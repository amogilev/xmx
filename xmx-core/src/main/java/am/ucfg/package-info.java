/**
 * Updateable configuration based on standard .ini files, which stores the 
 * current default values of all known options in a special form of comments
 * (named auto-comments), and allows to override them by regular options.
 * <p/>
 * Supports configuration updates like 
 * <ul>
 * <li>change of a comment for an option;</li>
 * <li>change of a default value for an option;</li>
 * <li>adding new options;</li>
 * <li>removing obsolete options;</li>
 * </ul>
 * In case of a change, auto-comments are updated as necessary, while regular
 * options and comments are kept untouched (except of removing of obsolete
 * options).     
 * 
 * @author Andrey Mogilev
 */
package am.ucfg;