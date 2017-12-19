/*
 * Copyright 2016-2017 码云
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
 *
 */
package org.intellij.gitosc.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.intellij.gitosc.api.data.GitoscErrorMessage;
import org.intellij.gitosc.exceptions.*;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscUrlUtil;
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
import static org.intellij.gitosc.api.GitoscApiUtil.fromJson;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubConnection.java
 * @author JetBrains s.r.o.
 */
public class GitoscConnection {
	@NotNull
	private final String myApiURL;
	@NotNull
	private final GitoscAuthData myAuth;
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
		myApiURL = GitoscUrlUtil.getApiUrl(auth);
		myAuth = auth;
		myClient = new GithubConnectionBuilder(auth, myApiURL).createClient();
		myReusable = reusable;
	}

	@NotNull
	public String getApiUrl() {
		return myApiURL;
	}

	@NotNull
	public GitoscAuthData getAuth(){
		return myAuth;
	}

	@Nullable
	private String getAccessToken() {
		switch (myAuth.getAuthType()){
			case SESSION:
				GitoscAuthData.SessionAuth sessionAuth = myAuth.getSessionAuth();
				assert sessionAuth != null;
				return "private_token=" + sessionAuth.getAccessToken();
			case TOKEN:
				GitoscAuthData.TokenAuth tokenAuth = myAuth.getTokenAuth();
				assert tokenAuth != null;
				return "access_token=" + tokenAuth.getToken();
			default:
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
	private static StringEntity createEntity(String requestBody){
		return new StringEntity(requestBody, ContentType.create("application/x-www-form-urlencoded", "UTF-8"));
	}

	private static StringEntity createEntity(String requestBody, @NotNull GitoscAuthData.AuthType authType){
		switch (authType){
			case TOKEN:
				return new StringEntity(requestBody, ContentType.APPLICATION_JSON);
			default:
				return new StringEntity(requestBody, ContentType.create("application/x-www-form-urlencoded", "UTF-8"));
		}
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
		return doRequest(getRequestUrl(myApiURL, path + "?" + getAccessToken()), requestBody, headers, verb);
	}

	@NotNull
	private static String getRequestUrl(@NotNull String ApiUrl, @NotNull String path) {
		return ApiUrl + path;
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
					((HttpPost)request).setEntity(createEntity(requestBody, this.getAuth().getAuthType()));
				}
				break;
			case PATCH:
				request = new HttpPatch(uri);
				if (requestBody != null) {
					((HttpPatch)request).setEntity(createEntity(requestBody, this.getAuth().getAuthType()));
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
				GitoscErrorMessage error = fromJson(parseResponse(entity.getContent()), GitoscErrorMessage.class);
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
		try (Reader reader = new InputStreamReader(gitoscResponse, CharsetToolkit.UTF8_CHARSET)) {
			return new JsonParser().parse(reader);
		} catch (JsonParseException jse) {
			throw new GitoscJsonException("Couldn't parse Gitee response", jse);
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

	public static abstract class PagedRequestBase<T> implements PagedRequest<T> {
		@NotNull private String myPath;
		@NotNull private final Collection<Header> myHeaders;

		private int myNextPage = 1;

		public PagedRequestBase(@NotNull String path, @NotNull Header... headers){
			myPath = path;
			myHeaders = Arrays.asList(headers);
		}

		@NotNull
		public List<T> next(@NotNull GitoscConnection connection) throws IOException {
			String url = getRequestUrl(connection.getApiUrl(), myPath + JOINER.join("&page=" + myNextPage, connection.getAccessToken()));

			ResponsePage response = connection.doRequest(url, null, myHeaders, HttpVerb.GET);

			if(response.getJsonElement() == null){
				throw new GitoscConfusingException("Empty response");
			}

			if (!response.getJsonElement().isJsonArray()) {
				throw new GitoscJsonException("Wrong json type: expected JsonArray", new Exception(response.getJsonElement().toString()));
			}

			List<T> result = parse(response.getJsonElement());
			if(result.size() > 0){
				myNextPage += 1;
			}else{
				myNextPage = 0;
			}

			return result;
		}

		public boolean hasNext() {
			return myNextPage > 0;
		}

		protected abstract List<T> parse(@NotNull JsonElement response) throws IOException;

	}

	public static class ArrayPagedRequest<T> extends PagedRequestBase<T> {
		@NotNull private final Class<? extends T[]> myTypeArray;

		public ArrayPagedRequest(@NotNull String path,
		                         @NotNull Class<? extends T[]> typeArray,
		                         @NotNull Header... headers) {
			super(path, headers);
			myTypeArray = typeArray;
		}

		@Override
		protected List<T> parse(@NotNull JsonElement response) throws IOException {
			if (!response.isJsonArray()) {
				throw new GitoscJsonException("Wrong json type: expected JsonArray", new Exception(response.toString()));
			}

			T[] result = fromJson(response.getAsJsonArray(), myTypeArray);
			return Arrays.asList(result);
		}
	}

	public interface PagedRequest<T> {
		@NotNull
		List<T> next(@NotNull GitoscConnection connection) throws IOException;

		boolean hasNext();

		@NotNull
		default List<T> getAll(@NotNull GitoscConnection connection) throws IOException {
			List<T> result = new ArrayList<>();
			while(hasNext()){
				result.addAll(next(connection));
			}
			return result;
		}

	}
}
