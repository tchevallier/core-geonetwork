package jeeves.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jeeves.utils.Xml;

import org.jdom.Element;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ConfigurationOverridesPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    Map<String, PropertyUpdater> updaters = new HashMap<String, PropertyUpdater>();
    private ApplicationContext applicationContext;
    public ConfigurationOverridesPostProcessor(List<Element> add, List<Element> set) {
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
        // TODO Auto-generated method stub
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

        private String propertyName;
        private String beanName;

        private void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        private void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getBeanName() {
            return beanName;
        }
    }
    private static class SetPropertyUpdater extends PropertyUpdater {
        
    }
    private static class AddPropertyUpdater extends PropertyUpdater {
        
    }
    
    private static interface ValueLoader {
        Object load(ApplicationContext context);
    }
    private static class RefValueLoader implements ValueLoader {
        private String beanName;

        public RefValueLoader(String beanName) {
            this.beanName = beanName;
        }

        @Override
        public Object load(ApplicationContext context) {
            Object bean = context.getBean(beanName);
            if(bean == null) {
                throw new IllegalArgumentException("Could not find a bean with id: "+beanName);
            }
            return bean;
        }
    }
}
