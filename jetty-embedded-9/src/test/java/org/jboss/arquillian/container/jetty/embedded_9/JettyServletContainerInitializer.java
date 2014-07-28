package org.jboss.arquillian.container.jetty.embedded_9;


import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.http.HttpServlet;

@HandlesTypes(value= HttpServlet.class)
public class JettyServletContainerInitializer implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
		for(Class<?> clazz : arg0) {
			if(clazz.isAssignableFrom(MyHandlesTypesServlet.class)) {
				arg1.addServlet(clazz.getName(), clazz.asSubclass(MyHandlesTypesServlet.class)).addMapping("/"+MyHandlesTypesServlet.URL_PATTERN);
			}
		}
	}
	
}
