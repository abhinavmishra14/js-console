package de.fme.jsconsole;

import java.io.Serializable;

/**
 * The Class JsConsoleDump.
 */
public class JsConsoleDump implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 51455718851086609L;

	/** The json. */
	private final String json;

	/** The node ref. */
	private final String nodeRef;

	/**
	 * Instantiates a new js console dump.
	 *
	 * @param nodeRef the node ref
	 * @param json the json
	 */
	public JsConsoleDump(final String nodeRef, final String json) {
		this.nodeRef = nodeRef;
		this.json = json;
	}

	/**
	 * Gets the node ref.
	 *
	 * @return the node ref
	 */
	public String getNodeRef() {
		return this.nodeRef;
	}

	/**
	 * Gets the json.
	 *
	 * @return the json
	 */
	public String getJson() {
		return this.json;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.json;
	}

}
