package com.prapps.tutorial.ejb.rest.interceptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.util.Base64;

@Provider
public class RestSecurityInterceptor implements ContainerRequestFilter {

	private static final Logger LOG = Logger.getLogger(RestSecurityInterceptor.class);
	
	private static final String AUTHORIZATION_PROPERTY = "Authorization";
	private static final String AUTHENTICATION_SCHEME = "Basic";
	private static final ServerResponse ACCESS_DENIED = new ServerResponse(
			"Access denied for this resource", 401, new Headers<Object>());;
	private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse(
			"Nobody can access this resource", 403, new Headers<Object>());;
	private static final ServerResponse SERVER_ERROR = new ServerResponse(
			"INTERNAL SERVER ERROR", 500, new Headers<Object>());;

	public void filter(ContainerRequestContext ctx) throws IOException {
		System.out.println("invoked SecurityInterceptor");

		ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) ctx
				.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
		Method method = methodInvoker.getMethod();
		// Access allowed for all
		if (!method.isAnnotationPresent(PermitAll.class)) {
			// Access denied for all
			if (method.isAnnotationPresent(DenyAll.class)) {
				ctx.abortWith(ACCESS_FORBIDDEN);
				return;
			}

			// Get request headers
			final MultivaluedMap<String, String> headers = ctx.getHeaders();
			LOG.debug("headers: "+headers);

			// Fetch authorization header
			final List<String> authorization = headers
					.get(AUTHORIZATION_PROPERTY);

			// If no authorization information present; block access
			if (authorization == null || authorization.isEmpty()) {
				ctx.abortWith(ACCESS_DENIED);
				return;
			}

			// Get encoded username and password
			final String encodedUserPassword = authorization.get(0)
					.replaceFirst(AUTHENTICATION_SCHEME + " ", "");

			// Decode username and password
			String usernameAndPassword = null;
			try {
				usernameAndPassword = new String(
						Base64.decode(encodedUserPassword));
			} catch (IOException e) {
				ctx.abortWith(SERVER_ERROR);
				return;
			}

			// Split username and password tokens
			final StringTokenizer tokenizer = new StringTokenizer(
					usernameAndPassword, ":");
			final String username = tokenizer.nextToken();
			final String password = tokenizer.nextToken();

			// Verifying Username and password
			LOG.debug(username+"\t"+password);

			// Verify user access
			if (method.isAnnotationPresent(RolesAllowed.class)) {
				RolesAllowed rolesAnnotation = method
						.getAnnotation(RolesAllowed.class);
				Set<String> rolesSet = new HashSet<String>(
						Arrays.asList(rolesAnnotation.value()));

				// Is user valid?
				if (!isUserAllowed(username, password, rolesSet)) {
					ctx.abortWith(ACCESS_DENIED);
					return;
				}
				/*ctx.setSecurityContext(new SecurityContext() {
					@Override
					public boolean isUserInRole(String role) {
						return true;
					}
					
					@Override
					public boolean isSecure() {
						return true;
					}
					
					@Override
					public Principal getUserPrincipal() {
						return new Principal() {
							
							@Override
							public String getName() {
								return username;
							}
						};
					}
					
					@Override
					public String getAuthenticationScheme() {
						return "sts";
					}
				});
				LOG.debug("Security Context is set");*/
			}
		}
	}

	private boolean isUserAllowed(final String username, final String password,
			final Set<String> rolesSet) {
		boolean isAllowed = false;

		// Step 1. Fetch password from database and match with password in
		// argument
		// If both match then get the defined role for user from database and
		// continue; else return isAllowed [false]
		// Access the database and do this part yourself
		// String userRole = userMgr.getUserRole(username);
		String userRole = "ADMIN";

		// Step 2. Verify user role
		if (rolesSet.contains(userRole)) {
			isAllowed = true;
		}
		return isAllowed;
	}
}
