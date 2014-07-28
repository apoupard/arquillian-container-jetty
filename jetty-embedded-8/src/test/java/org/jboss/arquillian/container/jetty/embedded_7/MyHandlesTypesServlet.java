package org.jboss.arquillian.container.jetty.embedded_7;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyHandlesTypesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String URL_PATTERN = "HandlesTypesServlet";

	public static final String MESSAGE = "helloHandlesTypes";

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append(MESSAGE);
	}

}
