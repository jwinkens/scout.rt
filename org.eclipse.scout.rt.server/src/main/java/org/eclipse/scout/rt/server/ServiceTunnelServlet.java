/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.security.AccessController;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.commons.ICallable;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.annotations.Internal;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.context.IRunMonitor;
import org.eclipse.scout.rt.server.ServerConfigProperties.HttpServerDebugProperty;
import org.eclipse.scout.rt.server.admin.html.AdminSession;
import org.eclipse.scout.rt.server.commons.cache.IClientIdentificationService;
import org.eclipse.scout.rt.server.commons.cache.IHttpSessionCacheService;
import org.eclipse.scout.rt.server.commons.config.WebXmlConfigManager;
import org.eclipse.scout.rt.server.commons.context.ServletRunContexts;
import org.eclipse.scout.rt.server.commons.servletfilter.IHttpServletRoundtrip;
import org.eclipse.scout.rt.server.context.RunMonitorCancelRegistry;
import org.eclipse.scout.rt.server.context.ServerRunContext;
import org.eclipse.scout.rt.server.context.ServerRunContexts;
import org.eclipse.scout.rt.server.session.ServerSessionProvider;
import org.eclipse.scout.rt.shared.servicetunnel.DefaultServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelRequest;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelResponse;
import org.eclipse.scout.rt.shared.ui.UserAgent;

/**
 * Use this Servlet to dispatch scout UI service requests using {@link IServiceTunnelRequest},
 * {@link IServiceTunnelResponse} and any {@link IServiceTunnelContentHandler} implementation.
 */
public class ServiceTunnelServlet extends HttpServlet {

  private static final String ADMIN_SESSION_KEY = AdminSession.class.getName();

  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ServiceTunnelServlet.class);

  private transient IServiceTunnelContentHandler m_contentHandler;
  private boolean m_debug;
  private WebXmlConfigManager m_configManager;

  // === HTTP-GET ===

  @Override
  protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
    if (Subject.getSubject(AccessController.getContext()) == null) {
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    lazyInit(servletRequest, servletResponse);

    try {
      ServletRunContexts.copyCurrent().locale(Locale.getDefault()).servletRequest(servletRequest).servletResponse(servletResponse).run(new IRunnable() {

        @Override
        public void run() throws Exception {
          ServerRunContext serverRunContext = ServerRunContexts.copyCurrent();
          serverRunContext.userAgent(UserAgent.createDefault());
          serverRunContext.session(lookupServerSessionOnHttpSession(serverRunContext.copy()));

          invokeAdminService(serverRunContext);
        }
      });
    }
    catch (ProcessingException e) {
      throw new ServletException("Failed to invoke AdminServlet", e);
    }
  }

  // === HTTP-POST ===

  @Override
  protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
    if (Subject.getSubject(AccessController.getContext()) == null) {
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    lazyInit(servletRequest, servletResponse);

    try {
      ServletRunContexts.copyCurrent().servletRequest(servletRequest).servletResponse(servletResponse).run(new IRunnable() {

        @Override
        public void run() throws Exception {
          IServiceTunnelRequest serviceRequest = deserializeServiceRequest();

          //enable cancel
          IRunMonitor runMonitor = BEANS.get(IRunMonitor.class);
          BEANS.get(RunMonitorCancelRegistry.class).register(serviceRequest.getRequestSequence(), runMonitor);

          ServerRunContext serverRunContext = ServerRunContexts.copyCurrent();
          serverRunContext.locale(serviceRequest.getLocale());
          serverRunContext.userAgent(UserAgent.createByIdentifier(serviceRequest.getUserAgent()));
          serverRunContext.runMonitor(runMonitor);
          serverRunContext.session(lookupServerSessionOnHttpSession(serverRunContext.copy()));

          IServiceTunnelResponse serviceResponse = invokeService(serverRunContext, serviceRequest);

          BEANS.get(RunMonitorCancelRegistry.class).unregister(serviceRequest.getRequestSequence());

          serializeServiceResponse(serviceResponse);
        }
      });
    }
    catch (Exception e) {
      if (isConnectionError(e)) {
        // NOOP: Ignore disconnect errors: we do not want to throw an exception, if the client closed the connection.
      }
      else {
        LOG.error(String.format("Client=%s@%s/%s", servletRequest.getRemoteUser(), servletRequest.getRemoteAddr(), servletRequest.getRemoteHost()), e);
        servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }

  // === SERVICE INVOCATION ===

  /**
   * Method invoked to delegate the HTTP request to the 'admin service'.
   */
  protected void invokeAdminService(final ServerRunContext serverRunContext) throws ProcessingException {
    serverRunContext.run(new IRunnable() {

      @Override
      public void run() throws Exception {
        final HttpServletRequest servletRequest = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.get();
        final HttpServletResponse servletResponse = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.get();

        AdminSession adminSession = (AdminSession) BEANS.get(IHttpSessionCacheService.class).getAndTouch(ADMIN_SESSION_KEY, servletRequest, servletResponse);
        if (adminSession == null) {
          adminSession = new AdminSession();
          BEANS.get(IHttpSessionCacheService.class).put(ADMIN_SESSION_KEY, adminSession, servletRequest, servletResponse);
        }
        adminSession.serviceRequest(servletRequest, servletResponse);
      }
    });
  }

  /**
   * Method invoked to delegate the HTTP request to the 'process service'.
   */
  protected IServiceTunnelResponse invokeService(final ServerRunContext serverRunContext, final IServiceTunnelRequest serviceTunnelRequest) throws ProcessingException {
    return serverRunContext.call(new ICallable<IServiceTunnelResponse>() {

      @Override
      public IServiceTunnelResponse call() throws Exception {
        return new DefaultTransactionDelegate(isDebug()).invoke(serviceTunnelRequest);
      }
    });
  }

  // === MESSAGE UNMARSHALLING / MARSHALLING ===

  /**
   * Method invoked to deserialize a service request to be given to the service handler.
   */
  protected IServiceTunnelRequest deserializeServiceRequest() throws Exception {
    return m_contentHandler.readRequest(IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.get().getInputStream());
  }

  /**
   * Method invoked to serialize a service response to be sent back to the client.
   */
  protected void serializeServiceResponse(IServiceTunnelResponse serviceResponse) throws Exception {
    // security: do not send back error stack trace
    if (serviceResponse.getException() != null) {
      serviceResponse.getException().setStackTrace(new StackTraceElement[0]);
    }

    HttpServletResponse servletResponse = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.get();
    servletResponse.setDateHeader("Expires", -1);
    servletResponse.setHeader("Cache-Control", "no-cache");
    servletResponse.setHeader("pragma", "no-cache");
    servletResponse.setContentType("text/xml");
    m_contentHandler.writeResponse(servletResponse.getOutputStream(), serviceResponse);
  }

  // === INITIALIZATION ===

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    m_configManager = new WebXmlConfigManager(config);

    // read config
    m_debug = m_configManager.getPropertyValue(HttpServerDebugProperty.class);
  }

  protected WebXmlConfigManager getConfigManager() {
    return m_configManager;
  }

  /**
   * Method invoked by 'HTTP-GET' and 'HTTP-POST' to identify the session-class and to initialize the content handler
   * for serialization/deserialization.
   */
  @Internal
  protected void lazyInit(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException {
    m_contentHandler = createContentHandler();
  }

  /**
   * Create the (reusable) content handler (soap, xml, binary) for marshalling scout remote service calls
   * <p>
   * This method is part of the protected api and can be overridden.
   */
  protected IServiceTunnelContentHandler createContentHandler() {
    DefaultServiceTunnelContentHandler e = new DefaultServiceTunnelContentHandler();
    e.initialize();
    return e;
  }

  // === SESSION LOOKUP ===

  @Internal
  protected IServerSession lookupServerSessionOnHttpSession(final ServerRunContext serverRunContext) throws ProcessingException, ServletException {
    final HttpServletRequest servletRequest = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.get();
    final HttpServletResponse servletResponse = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.get();

    //external request: apply locking, this is the session initialization phase
    IHttpSessionCacheService cacheService = BEANS.get(IHttpSessionCacheService.class);
    IServerSession serverSession = (IServerSession) cacheService.getAndTouch(IServerSession.class.getName(), servletRequest, servletResponse);
    if (serverSession == null) {
      synchronized (servletRequest.getSession()) {
        serverSession = (IServerSession) cacheService.get(IServerSession.class.getName(), servletRequest, servletResponse); // double checking
        if (serverSession == null) {
          serverSession = provideServerSession(serverRunContext);
          cacheService.put(IServerSession.class.getName(), serverSession, servletRequest, servletResponse);
        }
      }
    }
    return serverSession;
  }

  /**
   * Method invoked to provide a new {@link IServerSession} for the current HTTP-request.
   *
   * @param serverRunContext
   *          <code>ServerRunContext</code> with information about the ongoing service request.
   * @return {@link IServerSession}; must not be <code>null</code>.
   */
  protected IServerSession provideServerSession(final ServerRunContext serverRunContext) throws ProcessingException {
    final HttpServletRequest servletRequest = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.get();
    final HttpServletResponse servletResponse = IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.get();

    final IServerSession serverSession = BEANS.get(ServerSessionProvider.class).provide(serverRunContext);
    serverSession.setIdInternal(BEANS.get(IClientIdentificationService.class).getClientId(servletRequest, servletResponse));
    return serverSession;
  }

  // === Helper methods ===

  /**
   * @return <code>true</code> if the {@link ServiceTunnelServlet} runs in debug mode; see property
   *         {@link ServiceTunnelServlet#HTTP_DEBUG_PARAM}.
   */
  protected boolean isDebug() {
    return m_debug;
  }

  @Internal
  protected boolean isConnectionError(Exception e) {
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof SocketException) {
        return true;
      }
      else if (cause.getClass().getSimpleName().equalsIgnoreCase("EofException")) {
        return true;
      }
      else if (cause instanceof InterruptedIOException) {
        return true;
      }
      // next
      cause = cause.getCause();
    }
    return false;
  }
}
