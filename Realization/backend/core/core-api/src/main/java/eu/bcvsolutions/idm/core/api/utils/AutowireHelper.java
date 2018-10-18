package eu.bcvsolutions.idm.core.api.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Helper class which is able to autowire a specified class. It holds a static
 * reference to the {@link org .springframework.context.ApplicationContext}.
 * 
 * @author Ondřej Kopr
 * @author Radek Tomiška
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
     * @param objectToAutowire        the instance of the class which holds @Autowire annotations
     * @param beansToAutowireInClass the beans which have the @Autowire annotation in the specified
	 *            {#classToAutowire}
	 */
	public static void autowire(Object objectToAutowire, Object... beansToAutowireInClass) {
		autowireBean(objectToAutowire, beansToAutowireInClass);
	}
	
	/**
	 * Tries to autowire the specified instance of the class if one of the
	 * specified beans which need to be autowired are null.
	 *
     * @param objectToAutowire the instance of the class which holds @Autowire annotations
     * @param beansToAutowireInClass the beans which have the @Autowire annotation in the specified {#classToAutowire}
	 * @return autowired object
	 */
	public static <T> T autowireBean(T objectToAutowire, Object... beansToAutowireInClass) {
		if(beansToAutowireInClass == null || beansToAutowireInClass.length == 0) {
			applicationContext.getAutowireCapableBeanFactory().autowireBean(objectToAutowire);
			return objectToAutowire;
		}
		for (Object bean : beansToAutowireInClass) {
			if (bean == null && applicationContext != null) {
				applicationContext.getAutowireCapableBeanFactory().autowireBean(objectToAutowire);
				return objectToAutowire;
			}
		}
		return objectToAutowire;
	}
	
	/**
	 * Return specified instance of bean class defined by class.
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
	
	/**
	 * Returns bean description
	 * 
	 * @param beanName
	 * @return
	 */
	public static String getBeanDescription(String beanName) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext)applicationContext).getBeanFactory().getBeanDefinition(beanName).getDescription();
		}
		return null;
	}
	
	/**
	 * Returns bean description 
	 * 
	 * @param beanClass
	 * @return
	 */
	public static String getBeanDescription(Class<?> beanClass) {
		if (applicationContext == null) {
			// application is not initialized yet
			return null;
		}
		String[] beanNames =  applicationContext.getBeanNamesForType(beanClass);
		if (beanNames.length != 1) {
			return null;
		}
		return getBeanDescription(beanNames[0]);
	}
	
	/**
	 * Fully create a new bean instance of the given class.
	 * <p>Performs full initialization of the bean, including all applicable
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>Note: This is intended for creating a fresh instance, populating annotated
	 * fields and methods as well as applying all standard bean initialization callbacks.
	 * It does <i>not</> imply traditional by-name or by-type autowiring of properties;
	 * use {@link #createBean(Class, int, boolean)} for those purposes.
	 * 
	 * @param beanClass the class of the bean to create
	 * @return the new bean instance
	 * @throws BeansException if instantiation or wiring failed
	 */
	public static <T> T createBean(Class<T> beanClass) {
		return applicationContext.getAutowireCapableBeanFactory().createBean(beanClass);
	}
}
