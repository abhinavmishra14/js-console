package de.fme.jsconsole;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.connector.User;

/**
 * The Class IsAdminEvaluator.
 */
public class IsAdminEvaluator extends BaseEvaluator {

	/**
	 * Evaluate.
	 *
	 * @param jsonObject the json object
	 * @return true, if successful
	 */
	@Override
	public boolean evaluate(final JSONObject jsonObject) {
		final RequestContext requestCtx = ThreadLocalRequestContext.getRequestContext();
		final User user = requestCtx.getUser();
		return (user != null && user.isAdmin());
	}
}
