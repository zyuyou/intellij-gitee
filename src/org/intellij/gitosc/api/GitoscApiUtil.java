package org.intellij.gitosc.api;

import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.intellij.gitosc.exceptions.GitoscConfusingException;
import org.intellij.gitosc.exceptions.GitoscJsonException;
import org.intellij.gitosc.exceptions.GitoscStatusCodeException;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitoscApiUtil {
	private static final Logger LOG = GitoscUtil.LOG;

	public static final String DEFAULT_GITOSC_HOST = "git.oschina.net";

	@NotNull private static final Gson gson = initGson();

	private static Gson initGson(){
		GsonBuilder builder = new GsonBuilder();
		builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
		return builder.create();
	}

	@NotNull
	public static <T> T fromJson(@Nullable JsonElement json, @NotNull Class<T> classT) throws IOException {
		if (json == null) {
			throw new GitoscJsonException("Unexpected empty response");
		}

		T res;
		try {
			//cast as workaround for early java 1.6 bug
			//noinspection RedundantCast
			res = (T)gson.fromJson(json, classT);
		}
		catch (ClassCastException e) {
			throw new GitoscJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
		}
		catch (JsonParseException e) {
			throw new GitoscJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
		}
		if (res == null) {
			throw new GitoscJsonException("Empty Json response");
		}
		return res;
	}

	@NotNull
	public static <Raw extends DataConstructor, Result> Result createDataFromRaw(@NotNull Raw rawObject, @NotNull Class<Result> resultClass)
		throws GitoscJsonException {
		try {
			return rawObject.create(resultClass);
		}
		catch (Exception e) {
			throw new GitoscJsonException ("Json parse error", e);
		}
	}

	//====================================================================================
	// git.oschina.net/api/v3 - method calls
	//====================================================================================

	@NotNull
	public static List<GitoscRepo> getAvailableRepos(@NotNull GitoscConnection connection) throws IOException {
		List<GitoscRepo> repos = new ArrayList<GitoscRepo>();
		repos.addAll(getUserRepos(connection));
		return repos;
	}

	@NotNull
	public static GitoscUserDetailed getCurrentUserDetailed(@NotNull GitoscConnection connection, @NotNull GitoscAuthData authData) throws IOException {
		try {
			String requestBody;
			GitoscAuthData.SessionAuth sessionAuth = authData.getSessionAuth();

			if(sessionAuth != null){
				requestBody = GitoscUtil.JOINER.join("email=" + sessionAuth.getLogin(), "password=" + sessionAuth.getPassword());
			}else{
				requestBody = null;
			}

			JsonElement result = connection.postRequest("/session", requestBody);
			return createDataFromRaw(fromJson(result, GitoscUserRaw.class), GitoscUserDetailed.class);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user info");
			throw e;
		}
	}

	@NotNull
	public static GitoscRepoDetailed getDetailedRepoInfo(@NotNull GitoscConnection connection, @NotNull String owner, @NotNull String name)
		throws IOException {
		try {
			String path = "/projects/" + owner + "%2F" + name;
			return createDataFromRaw(fromJson(connection.getRequest(path), GitoscRepoRaw.class), GitoscRepoDetailed.class);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get repository info: " + owner + "/" + name);
			throw e;
		}
	}

	@NotNull
	public static List<GitoscRepo> getUserRepos(@NotNull GitoscConnection connection) throws IOException {
		try {
			String path = "/projects";

			GitoscConnection.PagedRequest<GitoscRepo> request = new GitoscConnection.PagedRequest<GitoscRepo>(path, GitoscRepo.class, GitoscRepoRaw[].class);

			return request.getAll(connection);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user repositories");
			throw e;
		}
	}

	@NotNull
	public static GitoscRepo createRepo(@NotNull GitoscConnection connection,
	                                    @NotNull String name,
	                                    @NotNull String description,
	                                    boolean isPrivate)
		throws IOException {

		try {
			String path = "/projects";
			String requestBody = GitoscUtil.JOINER.join("name=" + name, "description=" + description, "private=" + (isPrivate? 1 : 0));
			return createDataFromRaw(fromJson(connection.postRequest(path, requestBody), GitoscRepoRaw.class), GitoscRepo.class);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't create repository: " + name);
			throw e;
		}
	}

}
