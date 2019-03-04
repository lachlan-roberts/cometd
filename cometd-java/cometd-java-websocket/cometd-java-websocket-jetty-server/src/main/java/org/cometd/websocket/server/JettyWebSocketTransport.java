/*
 * Copyright (c) 2008-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cometd.websocket.server;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.websocket.server.common.AbstractBayeuxContext;
import org.cometd.websocket.server.common.AbstractWebSocketTransport;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

public class JettyWebSocketTransport extends AbstractWebSocketTransport {
    public JettyWebSocketTransport(BayeuxServerImpl bayeux) {
        super(bayeux);
    }

    @Override
    public void init() {
        super.init();

        final ServletContext context = (ServletContext)getOption(ServletContext.class.getName());
        if (context == null) {
            throw new IllegalArgumentException("Missing ServletContext");
        }

        String cometdURLMapping = (String)getOption(COMETD_URL_MAPPING_OPTION);
        if (cometdURLMapping == null) {
            throw new IllegalArgumentException("Missing '" + COMETD_URL_MAPPING_OPTION + "' parameter");
        }

        // TODO: uncomment and fix this code
        /*
        NativeWebSocketConfiguration wsConfig = (NativeWebSocketConfiguration)context.getAttribute(NativeWebSocketConfiguration.class.getName());
        if (wsConfig == null) {
            throw new IllegalArgumentException("Missing WebSocketConfiguration");
        }

        WebSocketPolicy policy = wsConfig.getFactory().getPolicy();
        int bufferSize = getOption(BUFFER_SIZE_OPTION, policy.getInputBufferSize());
        policy.setInputBufferSize(bufferSize);
        int maxMessageSize = getMaxMessageSize();
        if (maxMessageSize < 0) {
            maxMessageSize = policy.getMaxTextMessageSize();
        }
        policy.setMaxTextMessageSize(maxMessageSize);

        long idleTimeout = getOption(IDLE_TIMEOUT_OPTION, policy.getIdleTimeout());
        policy.setIdleTimeout((int)idleTimeout);

        for (String mapping : normalizeURLMapping(cometdURLMapping)) {
            wsConfig.addMapping(mapping, (request, response) -> {
                String origin = request.getHeader("Origin");
                if (origin == null) {
                    origin = request.getHeader("Sec-WebSocket-Origin");
                }
                if (checkOrigin(request, origin)) {
                    List<ExtensionConfig> negotiated = new ArrayList<>();
                    for (ExtensionConfig extensionConfig : request.getExtensions()) {
                        String name = extensionConfig.getName();
                        boolean option = getOption(ENABLE_EXTENSION_PREFIX_OPTION + name, true);
                        if (option) {
                            negotiated.add(extensionConfig);
                        }
                    }
                    response.setExtensions(negotiated);

                    modifyUpgrade(request, response);

                    List<String> allowedTransports = getBayeux().getAllowedTransports();
                    if (allowedTransports.contains(getName())) {
                        WebSocketContext handshake = new WebSocketContext(context, request);
                        return newWebSocketEndPoint(handshake);
                    } else {
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("Transport not those allowed: {}", allowedTransports);
                        }
                    }
                } else {
                    if (_logger.isDebugEnabled()) {
                        _logger.debug("Origin check failed for origin {}", origin);
                    }
                }
                return null;
            });
        }
        */
    }

    protected Object newWebSocketEndPoint(BayeuxContext bayeuxContext) {
        return new EndPoint(bayeuxContext);
    }

    protected void modifyUpgrade(ServletUpgradeRequest request, ServletUpgradeResponse response) {
    }

    protected boolean checkOrigin(ServletUpgradeRequest request, String origin) {
        return true;
    }

    private class WebSocketContext extends AbstractBayeuxContext {
        private final Map<String, Object> attributes;

        private WebSocketContext(ServletContext context, ServletUpgradeRequest request) {
            // TODO: review casts to InetSocketAddress
            super(context, request.getRequestURI().toString(), request.getQueryString(), request.getHeadersMap(),
                    request.getParameterMap(), request.getUserPrincipal(), request.getSession(),
                    (InetSocketAddress)request.getLocalSocketAddress(), (InetSocketAddress)request.getRemoteSocketAddress(),
                    Collections.list(request.getLocales()), "HTTP/1.1", request.isSecure());
            this.attributes = request.getServletAttributes();
        }

        @Override
        public Object getRequestAttribute(String name) {
            return attributes.get(name);
        }
    }

    private class EndPoint extends JettyWebSocketEndPoint {
        public EndPoint(BayeuxContext bayeuxContext) {
            super(JettyWebSocketTransport.this, bayeuxContext);
        }

        @Override
        protected void writeComplete(Context context, List<ServerMessage> messages) {
            JettyWebSocketTransport.this.writeComplete(context, messages);
        }
    }
}
