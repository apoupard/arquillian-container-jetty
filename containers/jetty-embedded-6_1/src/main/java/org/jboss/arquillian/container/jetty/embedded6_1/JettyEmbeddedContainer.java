/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.jetty.embedded6_1;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.jetty.api.ShrinkWrapWebAppContext;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * <p>Jetty Embedded 6.1.x container for the Arquillian project.</p>
 *
 * <p>This container only supports a WebArchive deployment. The context path of the
 * deployed application is always set to "/test", which is expected by the servlet protocol (SHRINKWRAP-196).</p>
 *
 * <p>A known issue (SHRINKWRAP-197) is that you cannot deploy two archives with the same name (i.e., test.war).
 * The ShrinkWrap Jetty extension always uses the same temporary file and doesn't delete
 * it until the JVM exists. Therefore, two successive Arquillian tests will collide trying
 * to write to the same file. The workaround is to give your archive a unique name in the
 * <code>@Deployment</code> method.</p>
 *
 * <p>Another known issue is that the container configuration process logs an exception when
 * running in-container. However, the container is still configured properly during setup.</p>
 *
 * <p>Minimum recommended Jetty version is 6.1.12, due to compatibility changes
 * with JNDI bindings (prior to 6.1.12, scoping was implemented differently).
 * Not compatible with Jetty 7 due to changes in package names.</p>
 *
 * @author Dan Allen
 * @version $Revision: $
 */
public class JettyEmbeddedContainer implements DeployableContainer
{
   public static final String HTTP_PROTOCOL = "http";

   public static final String WEB_APP_CONTEXT_PATH = "/test";

   public static final String[] JETTY_PLUS_CONFIGURATION_CLASSES =
   {
       "org.mortbay.jetty.webapp.WebInfConfiguration",
       "org.mortbay.jetty.plus.webapp.EnvConfiguration",
       //"org.mortbay.jetty.plus.webapp.Configuration",
       "org.jboss.arquillian.container.jetty.embedded6_1.CompositeConfiguration",
       "org.mortbay.jetty.webapp.JettyWebXmlConfiguration"
   };

   private static final Logger log = Logger.getLogger(JettyEmbeddedContainer.class.getName());

   private Server server;

   private JettyEmbeddedConfiguration containerConfig;
   
   public JettyEmbeddedContainer()
   {
   }
   
   public void setup(Context context, Configuration arquillianConfig)
   {
      containerConfig = arquillianConfig.getContainerConfig(JettyEmbeddedConfiguration.class);
   }
   
   public void start(Context context) throws LifecycleException
   {
      try 
      {
         server = new Server();
         Connector connector = new SelectChannelConnector();
         connector.setHost(containerConfig.getBindHost());
         connector.setPort(containerConfig.getBindHttpPort());
         server.setConnectors(new Connector[] { connector });
         server.start();
      } 
      catch (Exception e) 
      {
         throw new LifecycleException("Could not start container", e);
      }
   }

   public void stop(Context context) throws LifecycleException
   {
      try 
      {
         server.stop();
      } 
      catch (Exception e) 
      {
         throw new LifecycleException("Could not stop container", e);
      }
   }

   public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException
   {
      try 
      {
         WebAppContext wctx = archive.as(ShrinkWrapWebAppContext.class);
         // Jetty plus is required to support in-container invocation and enrichment
         if (containerConfig.isJettyPlus())
         {
            wctx.setConfigurationClasses(JETTY_PLUS_CONFIGURATION_CLASSES);
         }
         // FIXME shrinkwrap jetty adapter does not remove file extension from name when setting context path
         wctx.setContextPath(WEB_APP_CONTEXT_PATH);
         // HACK this needs to be rethought, perhaps another auxiliary archive appender to guarantee uniqueness and a static check for run mode?
//         if (archive.contains(ArchivePaths.create("/WEB-INF/lib/arquillian-protocol.jar")))
//         {
//            // wctx.setOverrideDescriptor("jar:file:" + wctx.getTempDirectory() + "/webapp/WEB-INF/lib/arquillian-protocol.jar!/META-INF/web-fragment.xml");
//            // NOTE go on faith there is only one META-INF/web-fragment.xml
//            wctx.setOverrideDescriptor("META-INF/web-fragment.xml");
//         }
         // possible configuration parameters
         wctx.setExtractWAR(true);
         wctx.setLogUrlOnStart(true);
         server.addHandler(wctx);
         wctx.start();
         context.add(WebAppContext.class, wctx);
      } 
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy " + archive.getName(), e);
      }

      try 
      {
         return new ServletMethodExecutor(
               new URL(
                     HTTP_PROTOCOL,
                     containerConfig.getBindHost(),
                     containerConfig.getBindHttpPort(),
                     "/")
               );
      } 
      catch (Exception e) 
      {
         throw new RuntimeException("Could not create ContainerMethodExecutor", e);
      }
   }

   public void undeploy(Context context, Archive<?> archive) throws DeploymentException
   {
      WebAppContext wctx = context.get(WebAppContext.class);
      if (wctx != null)
      {
         try
         {
            wctx.stop();
         }
         catch (Exception e)
         {
            e.printStackTrace();
            log.severe("Could not stop context " + wctx.getContextPath() + ": " + e.getMessage());
         }
         // NOTE work around a bug in Jetty where removeHandler is ignored if only one handler is set
         if (server.getHandlers() == null)
         {
            server.setHandler(null);
         }
         else
         {
            server.removeHandler(wctx);
         }
      }
   }
   
}
