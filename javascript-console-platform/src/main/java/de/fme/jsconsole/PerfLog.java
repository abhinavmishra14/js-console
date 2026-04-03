package de.fme.jsconsole;

import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for performance monitoring. create your instance via constructor, start
 * the monitoring via {@link #start()} and stop the monitoring with
 * {@link #stop(String, Object...)}.
 *
 * @author jgoldhammer
 */
public class PerfLog {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(PerfLog.class);

	/** The start time. */
	private long startTime;

	/**
	 * Starts the performance measurement.
	 *
	 * @return this instance for chained calls
	 */
	public PerfLog start() {
		this.startTime = System.currentTimeMillis();
		return this;
	}

	/**
	 * Stops the performance measurement.
	 *
	 * @param message the log message for logging execution performance in debug
	 *                level
	 * @param params  the parameters for the log message
	 * @return the time measured for between start and stop in milliseconds
	 */
	public long stop(final String message, final Object... params) {
		ParameterCheck.mandatoryString("message", message);

		final long endTime = System.currentTimeMillis();
		final long neededTime = endTime - this.startTime;

		final Object[] effParams = new Object[params.length + 1];
		System.arraycopy(params, 0, effParams, 1, params.length);
		effParams[0] = neededTime;

		LOGGER.debug(message, effParams);

		return neededTime;
	}
}
