/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.ui.html.json;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.shared.SharedConfigProperties.ServiceTunnelTargetUrlProperty;
import org.eclipse.scout.rt.shared.servicetunnel.http.DefaultAuthToken;
import org.eclipse.scout.rt.shared.servicetunnel.http.DefaultAuthTokenSigner;
import org.eclipse.scout.rt.shared.servicetunnel.http.HttpServiceTunnel;
import org.eclipse.scout.rt.ui.html.AbstractUiServletRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO CGU CN only proxy? Pure Scout JS apps use ClientNotificationResource?
public class PollRequestHandler extends AbstractUiServletRequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(PollRequestHandler.class);

  protected boolean acceptRequest(HttpServletRequest req) {
    return StringUtility.startsWith(req.getPathInfo(), getLocalContextPathPrefix());
  }

  protected String getLocalContextPathPrefix() {
    return "/poll";
  }

  @Override
  protected boolean handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!acceptRequest(req)) {
      return false;
    }
//    AsyncContext asyncContext = req.startAsync(req, resp);
    String baseUrl = BEANS.get(ServiceTunnelTargetUrlProperty.class).getValue().replace("/process", "");
//    HttpRequest httpReq = BEANS.get(DefaultHttpTransportManager.class).getHttpRequestFactory().buildRequest(req.getMethod(), new GenericUrl(url), null);
//    httpReq.executeAsync().get();

    HttpClient client = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    String url = baseUrl + "/api/notifications/poll";
    DefaultAuthToken token = BEANS.get(DefaultAuthTokenSigner.class).createDefaultSignedToken(DefaultAuthToken.class);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .POST(BodyPublishers.ofString(""))
        .header("Content-Type", "application/json")
        .headers("X-Requested-With", req.getHeader("X-Requested-With"))
        .header(HttpServiceTunnel.TOKEN_AUTH_HTTP_HEADER, token.toString())
        .build();
    try {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      resp.setContentType("application/json");
      resp.setStatus(response.statusCode());
      resp.getWriter().write(response.body());
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    // TODO CGU CN error handling?
    // TODO CGU CN abort remote connection browser abort
//    client.sendAsync(request, BodyHandlers.ofString())
//        .thenApply(HttpResponse::body)
//        .thenAccept(body -> {
//          try {
//            asyncContext.getResponse().getWriter().write(body);
//            asyncContext.complete();
//          }
//          catch (Throwable e) {
//            LOG.error("Proxying client notification poll request failed", e);
//          }
//        });

    return true;
  }
}
