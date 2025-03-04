/**
 *
 */
package de.fme.jsconsole;

import java.lang.reflect.Method;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * The Class VersionAwareCacheFactoryBean.
 *
 * @author <a href="mailto:axel.faust@prodyna.com">Axel Faust</a>,
 *         <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class VersionAwareCacheFactoryBean extends AbstractFactoryBean<Object> {

	/** The pre alf 42 class name. */
	protected String preAlf42ClassName;

	/** The cache name. */
	protected String cacheName;

	/** The descriptor service. */
	protected DescriptorService descriptorService;

	/**
	 * After properties set.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		PropertyCheck.mandatory(this, "descriptorService", this.descriptorService);

		PropertyCheck.mandatory(this, "preAlf42ClassName", this.preAlf42ClassName);
		PropertyCheck.mandatory(this, "cacheName", this.cacheName);

		super.afterPropertiesSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Object createInstance() throws Exception {
		final Descriptor serverDescriptor = this.descriptorService.getServerDescriptor();
		final VersionNumber versionNumber = serverDescriptor.getVersionNumber();

		final VersionNumber alf42Version = new VersionNumber("4.2");

		Object resultObject = null;

		// need to do most of this reflectively as classes may not be available between
		// 4.0/4.1 and 4.2/5.0
		if (alf42Version.compareTo(versionNumber) <= 0) {
			final Object cacheFactory = getBeanFactory().getBean("cacheFactory");
			Method createCacheMethod = cacheFactory.getClass().getMethod("createCache", String.class);
			resultObject = createCacheMethod.invoke(cacheFactory, this.cacheName);
		} else {
			final Class<?> forName = Class.forName(this.preAlf42ClassName);
			if (SimpleCache.class.isAssignableFrom(forName)) {
				resultObject = forName.newInstance();
			}
		}

		return resultObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getObjectType() {
		return SimpleCache.class;
	}

	/**
	 * Sets the pre alf 42 class name.
	 *
	 * @param preAlf42ClassName the preAlf42ClassName to set
	 */
	public final void setPreAlf42ClassName(final String preAlf42ClassName) {
		this.preAlf42ClassName = preAlf42ClassName;
	}

	/**
	 * Sets the cache name.
	 *
	 * @param cacheName the cacheName to set
	 */
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	/**
	 * Sets the descriptor service.
	 *
	 * @param descriptorService the descriptorService to set
	 */
	public final void setDescriptorService(final DescriptorService descriptorService) {
		this.descriptorService = descriptorService;
	}
}
