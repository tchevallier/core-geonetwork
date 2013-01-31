package jeeves.config.springutil;

import java.io.IOException;

import jeeves.server.ConfigurationOverrides;

import org.jdom.JDOMException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class JeevesApplicationContext extends XmlWebApplicationContext {
	
    private String appPath;
    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }
    
	@Override
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader)
			throws IOException {
		reader.setValidating(false);
		try {
            ConfigurationOverrides.importSpringConfigurations(reader, (ConfigurableBeanFactory) reader.getBeanFactory(), getServletContext(), appPath);
        } catch (JDOMException e) {
            throw new IOException(e);
        }
		super.loadBeanDefinitions(reader);
	}
}
