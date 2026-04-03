package de.fme.jsconsole;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.alfresco.util.MD5;
import org.springframework.extensions.webscripts.ScriptContent;

/**
 * The Class StringScriptContent.
 */
public class StringScriptContent implements ScriptContent {

	/** The content. */
	private final String content;

	/**
	 * Instantiates a new string script content.
	 *
	 * @param content the content
	 */
	public StringScriptContent(final String content) {
		this.content = content;
	}

	/**
	 * Gets the input stream.
	 *
	 * @return the input stream
	 */
	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(this.content.getBytes(Charset.forName("UTF-8")));
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	@Override
	public String getPath() {
		return MD5.Digest(this.content.getBytes()) + ".js";
	}

	/**
	 * Gets the path description.
	 *
	 * @return the path description
	 */
	@Override
	public String getPathDescription() {
		return "Javascript Console Script";
	}

	/**
	 * Gets the reader.
	 *
	 * @return the reader
	 */
	@Override
	public Reader getReader() {
		return new StringReader(this.content);
	}

	/**
	 * Checks if is cachable.
	 *
	 * @return true, if is cachable
	 */
	@Override
	public boolean isCachable() {
		return false;
	}

	/**
	 * Checks if is secure.
	 *
	 * @return true, if is secure
	 */
	@Override
	public boolean isSecure() {
		return true;
	}
}