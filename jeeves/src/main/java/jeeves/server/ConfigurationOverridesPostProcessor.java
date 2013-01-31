package jeeves.server;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import jeeves.utils.Log;
import jeeves.utils.Xml;

import org.jdom.Element;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ConfigurationOverridesPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    Multimap<String, PropertyUpdater> updaters = HashMultimap.create();
    private ApplicationContext applicationContext;
    private Properties properties;

    public ConfigurationOverridesPostProcessor(List<Element> add, List<Element> set, Properties properties) {
        this.properties = properties;
        for (Element element : set) {
            PropertyUpdater updater = PropertyUpdater.create(element);
            this.updaters.put(updater.getBeanName(), updater);
        }
        for (Element element : add) {
            PropertyUpdater updater = PropertyUpdater.create(element);
            this.updaters.put(updater.getBeanName(), updater);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for (PropertyUpdater updater : updaters.get(beanName)) {
            updater.update(applicationContext, properties, bean);
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    private static abstract class PropertyUpdater {

        public static PropertyUpdater create(Element element) {
            PropertyUpdater updater;
            if("set".equalsIgnoreCase(element.getName())) {
                updater = new SetPropertyUpdater();
            } else if ("add".equalsIgnoreCase(element.getName())) {
                updater = new AddPropertyUpdater();
            } else {
                throw new IllegalArgumentException(element.getName()+" is not known type of updater");
            }
            updater.setBeanName(element.getAttributeValue("bean"));
            updater.setPropertyName(element.getAttributeValue("property"));
            ValueLoader valueLoader;
            if(element.getAttributeValue("ref") != null) {
                valueLoader = new RefValueLoader(element.getAttributeValue("ref"));
            } else {
                throw new IllegalArgumentException(Xml.getString(element)+" does not have a value associated with it that is recognized. Excepted ref or value attribute");
            }
            updater.setSetValueLoader(valueLoader);
            return updater;
        }

        public Object update(ApplicationContext applicationContext, Properties properties, Object bean) {
            Object value = valueLoader.load(applicationContext, properties);
            Field field = ReflectionUtils.findField(bean.getClass(), propertyName);
            return doUpdate(applicationContext, bean, field, value);
        }
        protected abstract Object doUpdate(ApplicationContext applicationContext, Object bean, Field field, Object value);
        
        protected ValueLoader valueLoader;
        protected String propertyName;
        protected String beanName;

        private void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }
        private void setSetValueLoader(ValueLoader valueLoader) {
            this.valueLoader = valueLoader;   
        }
        private void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getBeanName() {
            return beanName;
        }
    }
    private static class SetPropertyUpdater extends PropertyUpdater {

        @Override
        protected Object doUpdate(ApplicationContext applicationContext, Object bean, Field field, Object value) {
            Log.debug(Log.JEEVES, "Setting "+propertyName+" on "+beanName+" with new value: "+value);
            ReflectionUtils.setField(field, bean, value);
            return bean;
        }
        
    }
    private static class AddPropertyUpdater extends PropertyUpdater {

        @SuppressWarnings("unchecked")
        @Override
        protected Object doUpdate(ApplicationContext applicationContext, Object bean, Field field, Object value) {
            Log.debug(Log.JEEVES, "Adding new value "+value+" to property: "+propertyName+" on "+beanName);
            Object originalValue = ReflectionUtils.getField(field, bean);
            if (originalValue instanceof Collection) {
                Collection<Object> coll = (Collection<Object>) originalValue;
                coll.add(value);
            }
            return bean;
        }
        
    }
    
    private static interface ValueLoader {
        Object load(ApplicationContext context, Properties properties);
    }
    private static class RefValueLoader implements ValueLoader {
        private String beanName;

        public RefValueLoader(String beanName) {
            this.beanName = beanName;
        }

        @Override
        public Object load(ApplicationContext context, Properties properties) {
            Object bean = context.getBean(beanName);
            if(bean == null) {
                throw new IllegalArgumentException("Could not find a bean with id: "+beanName);
            }
            if (bean instanceof String) {
                String string = (String) bean;
                bean = ConfigurationOverrides.updatePropertiesInText(properties, string);
            }
            return bean;
        }
    }
}
