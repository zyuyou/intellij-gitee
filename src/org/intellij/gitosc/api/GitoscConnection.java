/*
 * Copyright 2016 码云
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.gitosc.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.PlatformUtils;
import com.intellij.util.net.IdeHttpClientHelpers;
import com.intellij.util.net.ssl.CertificateManager;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.intellij.gitosc.exceptions.*;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscSettings;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.net.ssl.SSLHandshakeException;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.intellij.gitosc.GitoscConstants.JOINER;
import static org.intellij.gitosc.GitoscConstants.LOG;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubConnection.java
 * @author JetBrains s.r.o.
 */
public class GitoscConnection {
	@NotNull
	private final String myHost;
	@Nullable
	private final String myAccessToken;
	@NotNull
	private final CloseableHttpClient myClient;

	private final boolean myReusable;

	private volatile HttpUriRequest myRequest;
	private volatile boolean myAborted;

	private enum HttpVerb {
		GET, POST, DELETE, HEAD, PATCH
	}

	@TestOnly
	public GitoscConnection(@NotNull GitoscAuthData auth){
		this(auth, false);
	}

	public GitoscConnection(@NotNull GitoscAuthData auth, boolean reusable) {
		myHost = auth.getHost();

		GitoscAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
		if(sessionAuth != null){
			myAccessToken = sessionAuth.getAccessToken();
		}else {
			myAccessToken = null;
		}

		myClient = createClient(auth);
		myReusable = reusable;
	}

	@NotNull
	public String getHost() {
		return myHost;
	}

	@Nullable
	private String getAccessToken() {
		if(myAccessToken != null){
			return "private_token=" + myAccessToken;
		}else{
			return null;
		}
	}

	public void abort() {
		if (myAborted) return;
		myAborted = true;

		HttpUriRequest request = myRequest;
		if (request != null) request.abort();
	}

	public void close() throws IOException {
		myClient.close();
	}

	//======================================================================
	// client creation
	//======================================================================
	@NotNull
	private static CloseableHttpClient createClient(@NotNull GitoscAuthData auth){
		HttpClientBuilder builder = HttpClients.custom();

		return builder
			.setDefaultRequestConfig(createRequestConfig(auth))
			.setDefaultConnectionConfig(createConnectionConfig(auth))
//			.setDefaultCredentialsProvider(createCredentialsProvider(auth))
			.setDefaultHeaders(createHeaders(auth))
//			.addInterceptorFirst(PREEMPTIVE_BASIC_AUTH)
			.setSslcontext(CertificateManager.getInstance().getSslContext())
//			HostNameVerifier
			.build();
	}

	@NotNull
	private static RequestConfig createRequestConfig(@NotNull GitoscAuthData auth){
		RequestConfig.Builder builder = RequestConfig.custom();

		int timeout = GitoscSettings.getInstance().getConnectionTimeout();

		builder
			.setConnectTimeout(timeout)
			.setSocketTimeout(timeout);

		if(auth.isUseProxy()){
			IdeHttpClientHelpers.ApacheHttpClient4.setProxyForUrlIfEnabled(builder, auth.getHost());
		}

		return builder.build();
	}

	@NotNull
	private static ConnectionConfig createConnectionConfig(@NotNull GitoscAuthData auth) {
		return ConnectionConfig.custom()
			.setCharset(Consts.UTF_8)
			.build();
	}

	@NotNull
	private static Collection<? extends Header> createHeaders(@NotNull GitoscAuthData auth) {
		List<Header> headers = new ArrayList<Header>();
		headers.add(new BasicHeader("User-Agent", "GitExt/1.0-JB." + PlatformUtils.getPlatformPrefix()));
		return headers;
	}

	//======================================================================
	// Request functions
	//======================================================================
	@Nullable
	public JsonElement getRequest(@NotNull String path,
	                              @NotNull Header... headers) throws IOException {
		return request(path, null, Arrays.asList(headers), HttpVerb.GET).getJsonElement();
	}

	@Nullable
	public JsonElement postRequest(@NotNull String path,
	                               @Nullable String requestBody,
	                               @NotNull Header... headers) throws IOException {
		return request(path, requestBody, Arrays.asList(headers), HttpVerb.POST).getJsonElement();
	}

	@Nullable
	public JsonElement patchRequest(@NotNull String path,
	                                @Nullable String requestBody,
	                                @NotNull Header... headers) throws IOException {
		return request(path, requestBody, Arrays.asList(headers), HttpVerb.PATCH).getJsonElement();
	}

	@Nullable
	public JsonElement deleteRequest(@NotNull String path,
	                                 @NotNull Header... headers) throws IOException {
		return request(path, null, Arrays.asList(headers), HttpVerb.DELETE).getJsonElement();
	}

	@NotNull
	public Header[] headRequest(@NotNull String path,
	                            @NotNull Header... headers) throws IOException {
		return request(path, null, Arrays.asList(headers), HttpVerb.HEAD).getHeaders();
	}

	@NotNull
	private ResponsePage request(@NotNull String path,
	                             @Nullable String requestBody,
	                             @NotNull Collection<Header> headers,
	                             @NotNull HttpVerb verb) throws IOException {
		return doRequest(getRequestUrl(myHost, path + "?" + getAccessToken()), requestBody, headers, verb);
	}

	@NotNull
	private static String getRequestUrl(@NotNull String host, @NotNull String path) {
		return GitoscUrlUtil.getApiUrl(host) + path;
	}

	@NotNull
	private ResponsePage doRequest(@NotNull String uri,
	                               @Nullable String requestBody,
	                               @NotNull Collection<Header> headers,
	                               @NotNull HttpVerb verb) throws IOException {

		if (myAborted) throw new GitoscOperationCanceledException();

		if (EventQueue.isDispatchThread() && !ApplicationManager.getApplication().isUnitTestMode()) {
			LOG.warn("Network operation in EDT"); // TODO: fix
		}

		CloseableHttpResponse response = null;
		try {
			response = doREST(uri, requestBody, headers, verb);

			if (myAborted) throw new GitoscOperationCanceledException();

			checkStatusCode(response, requestBody);

			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return createResponse(response);
			}

			JsonElement ret = parseResponse(entity.getContent());
			if (ret.isJsonNull()) {
				return createResponse(response);
			}

			return createResponse(ret, null, response);
		}
		catch (SSLHandshakeException e) { // User canceled operation from CertificateManager
			if (e.getCause() instanceof CertificateException) {
				LOG.info("Host SSL certificate is not trusted", e);
				throw new GitoscOperationCanceledException("Host SSL certificate is not trusted", e);
			}
			throw e;
		}
		catch (IOException e) {
			if (myAborted) throw new GitoscOperationCanceledException("Operation canceled", e);
			throw e;
		}
		finally {
			myRequest = null;
			if (response != null) {
				response.close();
			}
			if (!myReusable) {
				myClient.close();
			}
		}
	}

	@NotNull
	private CloseableHttpResponse doREST(@NotNull final String uri,
	                                     @Nullable final String requestBody,
	                                     @NotNull final Collection<Header> headers,
	                                     @NotNull final HttpVerb verb) throws IOException {
		HttpRequestBase request;
		switch (verb) {
			case POST:
				request = new HttpPost(uri);
				if (requestBody != null) {
					((HttpPost)request).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED));
				}
				break;
			case PATCH:
				request = new HttpPatch(uri);
				if (requestBody != null) {
					((HttpPatch)request).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED));
				}
				break;
			case GET:
				request = new HttpGet(uri);
				break;
			case DELETE:
				request = new HttpDelete(uri);
				break;
			case HEAD:
				request = new HttpHead(uri);
				break;
			default:
				throw new IllegalStateException("Unknown HttpVerb: " + verb.toString());
		}

		for (Header header : headers) {
			request.addHeader(header);
		}

		myRequest = request;
		return myClient.execute(request);
	}

	//======================================================================
	// Respone functions
	//======================================================================
	private static void checkStatusCode(@NotNull CloseableHttpResponse response, @Nullable String body) throws IOException {
		int code = response.getStatusLine().getStatusCode();
		switch (code) {
			case HttpStatus.SC_OK:
			case HttpStatus.SC_CREATED:
			case HttpStatus.SC_ACCEPTED:
			case HttpStatus.SC_NO_CONTENT:
				return;
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_PAYMENT_REQUIRED:
			case HttpStatus.SC_FORBIDDEN:
				//noinspection ThrowableResultOfMethodCallIgnored
				GitoscStatusCodeException error = getStatusCodeException(response);

				if (error.getError() != null && error.getError().containsReasonMessage("API rate limit exceeded")) {
					throw new GitoscRateLimitExceededException(error.getMessage());
				}

				throw new GitoscAuthenticationException("Request response: " + error.getMessage());
			case HttpStatus.SC_BAD_REQUEST:
			case HttpStatus.SC_UNPROCESSABLE_ENTITY:
				LOG.info("body message:" + body);
				throw getStatusCodeException(response);
			default:
				throw getStatusCodeException(response);
		}
	}

	@NotNull
	private static GitoscStatusCodeException getStatusCodeException(@NotNull CloseableHttpResponse response) {
		StatusLine statusLine = response.getStatusLine();
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				GitoscErrorMessage error = GitoscApiUtil.fromJson(parseResponse(entity.getContent()), GitoscErrorMessage.class);
				String message = statusLine.getReasonPhrase() + " - " + error.getMessage();
				return new GitoscStatusCodeException(message, error, statusLine.getStatusCode());
			}
		}
		catch (IOException e) {
			LOG.info(e);
		}
		return new GitoscStatusCodeException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
	}

	@NotNull
	private static JsonElement parseResponse(@NotNull InputStream gitoscResponse) throws IOException {
		Reader reader = new InputStreamReader(gitoscResponse, CharsetToolkit.UTF8_CHARSET);
		try {
			return new JsonParser().parse(reader);
		}
		catch (JsonParseException jse) {
			throw new GitoscJsonException("Couldn't parse GitOSC response", jse);
		}
		finally {
			reader.close();
		}
	}

	private ResponsePage createResponse(@NotNull CloseableHttpResponse response) throws GitoscOperationCanceledException {
		if (myAborted) throw new GitoscOperationCanceledException();

		return new ResponsePage(null, null, response.getAllHeaders());
	}

	private ResponsePage createResponse(@NotNull JsonElement ret, @Nullable String path, @NotNull CloseableHttpResponse response)
		throws GitoscOperationCanceledException {
		if (myAborted) throw new GitoscOperationCanceledException();

		return new ResponsePage(ret, path, response.getAllHeaders());
	}


	public static class PagedRequest<T> {
		@NotNull private String myPath;
		@NotNull private final Collection<Header> myHeaders;
		@NotNull private final Class<T> myResult;
		@NotNull private final Class<? extends DataConstructor[]> myRawArray;

		private int myNextPage = 1;

		public PagedRequest(@NotNull String path,
		                    @NotNull Class<T> result,
		                    @NotNull Class<? extends DataConstructor[]> rawArray,
		                    @NotNull Header... headers) {
			myPath = path;
			myResult = result;
			myRawArray = rawArray;
			myHeaders = Arrays.asList(headers);
		}

		@NotNull
		public List<T> next(@NotNull GitoscConnection connection) throws IOException {
			String url = getRequestUrl(connection.getHost(), myPath + "?" + JOINER.join("page=" + myNextPage, connection.getAccessToken()));

			ResponsePage response = connection.doRequest(url, null, myHeaders, HttpVerb.GET);

			if (response.getJsonElement() == null) {
				throw new GitoscConfusingException("Empty response");
			}

			if (!response.getJsonElement().isJsonArray()) {
				throw new GitoscJsonException("Wrong json type: expected JsonArray", new Exception(response.getJsonElement().toString()));
			}

			List<T> result = new ArrayList<T>();

			DataConstructor[] rawList = GitoscApiUtil.fromJson(response.getJsonElement().getAsJsonArray(), myRawArray);
			if(rawList.length > 0){
				for (DataConstructor raw : rawList) {
					result.add(GitoscApiUtil.createDataFromRaw(raw, myResult));
				}
				myNextPage += 1;
			}else{
				myNextPage = 0;
			}
			return result;
		}

		public boolean hasNext() {
			return myNextPage > 0;
		}

		@NotNull
		public List<T> getAll(@NotNull GitoscConnection connection) throws IOException {
			List<T> result = new ArrayList<T>();
			while (hasNext()) {
				result.addAll(next(connection));
			}
			return result;
		}
	}

	private static class ResponsePage {
		@Nullable private final JsonElement myResponse;
		@Nullable private final String myNextPage;
		@NotNull private final Header[] myHeaders;

		public ResponsePage(@Nullable JsonElement response, @Nullable String next, @NotNull Header[] headers) {
			myResponse = response;
			myNextPage = next;
			myHeaders = headers;
		}

		@Nullable
		public JsonElement getJsonElement() {
			return myResponse;
		}

		@Nullable
		public String getNextPage() {
			return myNextPage;
		}

		@NotNull
		public Header[] getHeaders() {
			return myHeaders;
		}
	}
}
