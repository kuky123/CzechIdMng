package eu.bcvsolutions.idm.core.api;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Helper class which is able to autowire a specified class. It holds a static
 * reference to the {@link org .springframework.context.ApplicationContext}.
 */
@Component
public final class AutowireHelper implements ApplicationContextAware {

	private static final AutowireHelper INSTANCE = new AutowireHelper();
	private static ApplicationContext applicationContext;

	private AutowireHelper() {
	}

	/**
	 * Tries to autowire the specified instance of the class if one of the
	 * specified beans which need to be autowired are null.
	 *
	 * @param classToAutowire
	 *            the instance of the class which holds @Autowire annotations
	 * @param beansToAutowireInClass
	 *            the beans which have the @Autowire annotation in the specified
	 *            {#classToAutowire}
	 */
	public static void autowire(Object classToAutowire, Object... beansToAutowireInClass) {
		for (Object bean : beansToAutowireInClass) {
			if (bean == null && applicationContext != null) {
				applicationContext.getAutowireCapableBeanFactory().autowireBean(classToAutowire);
				return;
			}
		}
	}
	
	/**
	 * Return specified instace of bean class defined by class.
	 * 
	 * @param clazz
	 * @return
	 */
	public static <T> T getBean( Class<T> clazz ) {
	      if ( applicationContext != null ) {
	         return applicationContext.getBean( clazz );
	      }
	      return null;
	   }

	/**
	 * Return specified instance of bean class defined by name of class.
	 * 
	 * @param name
	 * @return
	 */
	public static Object getBean(String name) {
		if (applicationContext != null) {
			return applicationContext.getBean(name);
		}
		return null;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) {
		AutowireHelper.applicationContext = applicationContext;
	}

	/**
	 * @return the singleton instance.
	 */
	public static AutowireHelper getInstance() {
		return INSTANCE;
	}

}
