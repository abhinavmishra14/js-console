package de.fme.jsconsole;

import org.alfresco.repo.jscript.ScriptLogger;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.annotation.ScriptClass;
import org.springframework.extensions.webscripts.annotation.ScriptClassType;
import org.springframework.extensions.webscripts.annotation.ScriptMethod;
import org.springframework.extensions.webscripts.annotation.ScriptParameter;

/**
 * This class is based upon {@link ScriptLogger the default Repository-tier
 * script logger} and its
 * {@link org.springframework.extensions.webscripts.ScriptLogger web-scripts
 * clone} and has been modified to allow printing of messages to the JavaScript
 * Console result.
 * 
 * @author Kevin Roast
 * @author davidc
 * @author Florian Maul (fme AG)
 */
@ScriptClass(help = "Provides functions to aid debugging of scripts.", code = "logger.log(\"Command Processor: isEmailed=\" + isEmailed);", types = {
		ScriptClassType.JavaScriptRootObject })
public final class JavascriptConsoleScriptLogger {

	/** The Constant logger. */
	// NOTE: keep compatibility with repository script logger
	private static final Logger logger = LoggerFactory.getLogger(ScriptLogger.class);

	/** The system out. */
	private final SystemOut systemOut = new SystemOut();

	/** The js console. */
	private final JavascriptConsoleScriptObject jsConsole;

	/**
	 * Instantiates a new javascript console script logger.
	 *
	 * @param jsConsole the js console
	 */
	public JavascriptConsoleScriptLogger(JavascriptConsoleScriptObject jsConsole) {
		this.jsConsole = jsConsole;
	}

	/**
	 * Checks if is logging enabled.
	 *
	 * @return true, if is logging enabled
	 */
	@ScriptMethod(help = "Returns true if logging is enabled.", code = "var loggerStatus = logger.isLoggingEnabled();", output = "true if logging is enabled")
	public boolean isLoggingEnabled() {
		return logger.isDebugEnabled();
	}

	/**
	 * Log.
	 *
	 * @param str the str
	 */
	@ScriptMethod(help = "Logs a message")
	public void log(@ScriptParameter(help = "Message to log") String str) {
		logger.debug(str);
		jsConsole.print("DEBUG - " + str);
	}

	/**
	 * Checks if is debug logging enabled.
	 *
	 * @return true, if is debug logging enabled
	 */
	@ScriptMethod(help = "Returns true if debug logging is enabled.", code = "var loggerStatus = logger.isDebugEnabled();", output = "true if debug logging is enabled")
	public boolean isDebugLoggingEnabled() {
		return logger.isDebugEnabled();
	}

	/**
	 * Debug.
	 *
	 * @param str the str
	 */
	@ScriptMethod(help = "Logs a debug message")
	public void debug(@ScriptParameter(help = "Message to log") String str) {
		logger.debug(str);
		jsConsole.print("DEBUG - " + str);
	}

	/**
	 * Checks if is info logging enabled.
	 *
	 * @return true, if is info logging enabled
	 */
	@ScriptMethod(help = "Returns true if info logging is enabled.", code = "var loggerStatus = logger.isInfoEnabled();", output = "true if info logging is enabled")
	public boolean isInfoLoggingEnabled() {
		return logger.isInfoEnabled();
	}

	/**
	 * Info.
	 *
	 * @param str the str
	 */
	@ScriptMethod(help = "Logs an info message")
	public void info(@ScriptParameter(help = "Message to log") String str) {
		logger.info(str);
		jsConsole.print(str);
	}

	/**
	 * Checks if is warn logging enabled.
	 *
	 * @return true, if is warn logging enabled
	 */
	@ScriptMethod(help = "Returns true if warn logging is enabled.", code = "var loggerStatus = logger.isWarnLoggingEnabled();", output = "true if warn logging is enabled")
	public boolean isWarnLoggingEnabled() {
		return logger.isWarnEnabled();
	}

	/**
	 * Warn.
	 *
	 * @param str the str
	 */
	@ScriptMethod(help = "Logs a warning message")
	public void warn(@ScriptParameter(help = "Message to log") String str) {
		logger.warn(str);
		jsConsole.print("WARN - " + str);
	}

	/**
	 * Checks if is error logging enabled.
	 *
	 * @return true, if is error logging enabled
	 */
	@ScriptMethod(help = "Returns true if error logging is enabled.", code = "var loggerStatus = logger.isErrorLoggingEnabled();", output = "true if error logging is enabled")
	public boolean isErrorLoggingEnabled() {
		return logger.isErrorEnabled();
	}

	/**
	 * Error.
	 *
	 * @param str the str
	 */
	@ScriptMethod(help = "Logs an error message")
	public void error(@ScriptParameter(help = "Message to log") String str) {
		logger.error(str);
		jsConsole.print("ERROR - " + str);
	}

	/**
	 * Gets the system.
	 *
	 * @return the system
	 */
	public SystemOut getSystem() {
		return systemOut;
	}

	/**
	 * The Class SystemOut.
	 */
	public class SystemOut {

		/**
		 * Out.
		 *
		 * @param str the str
		 */
		public void out(String str) {
			System.out.println(str);
			jsConsole.print(str);
		}
	}

	/**
	 * Sets the level.
	 *
	 * @param loggerName the logger name
	 * @param level      the level
	 */
	public void setLevel(String loggerName, String level) {
		org.apache.log4j.Logger underlyingLogger = org.apache.log4j.Logger.getLogger(loggerName);
		Level logLevel = Level.toLevel(level);
		underlyingLogger.setLevel(logLevel);
	}

	/**
	 * Gets the level.
	 *
	 * @param loggerName the logger name
	 * @return the level
	 */
	public String getLevel(String loggerName) {
		return org.apache.log4j.Logger.getLogger(loggerName).getLevel().toString();
	}
}
