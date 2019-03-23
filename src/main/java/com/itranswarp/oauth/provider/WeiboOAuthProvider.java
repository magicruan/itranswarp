package com.itranswarp.oauth.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.itranswarp.enums.AuthProviderType;
import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.util.JsonUtil;

@Component
@ConditionalOnProperty(name = "spring.oauth.weibo.enabled", havingValue = "true")
public class WeiboOAuthProvider extends AbstractOAuthProvider {

	@Value("${spring.oauth.weibo.client-id:}")
	String clientId;

	@Value("${spring.oauth.weibo.client-secret:}")
	String clientSecret;

	@Override
	public String getAuthenticateUrl(String redirectUrl) {
		return String.format("https://api.weibo.com/oauth2/authorize?client_id=%s&response_type=%s&redirect_uri=%s",
				this.clientId, "code", URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
	}

	@Override
	public OAuthAuthentication getAuthentication(String code, String redirectUrl) throws Exception {
		String[] queries = new String[] { // request body
				"client_id=" + this.clientId, // client id
				"client_secret=" + this.clientSecret, // client secret
				"grant_type=authorization_code", // grant type
				"code=" + code, // code
				"redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) };
		String postData = String.join("&", queries);
		logger.info("post: {}", postData);
		HttpRequest requestAccessToken = HttpRequest.newBuilder()
				.uri(new URI("https://api.weibo.com/oauth2/access_token")).timeout(DEFAULT_TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(postData, StandardCharsets.UTF_8)).build();
		HttpResponse<String> responseAccessToken = this.httpClient.send(requestAccessToken,
				HttpResponse.BodyHandlers.ofString());
		if (responseAccessToken.statusCode() != 200) {
			throw new IOException("Bad response: " + responseAccessToken.statusCode());
		}
		WeiboAuth auth = JsonUtil.readJson(responseAccessToken.body(), WeiboAuth.class);
		HttpRequest requestUser = HttpRequest.newBuilder()
				.uri(new URI("https://api.weibo.com/2/users/show.json?uid=" + auth.uid)).timeout(DEFAULT_TIMEOUT)
				.header("Content-Type", "application/json").header("Authorization", "OAuth2 " + auth.access_token).GET()
				.build();
		HttpResponse<String> responseUser = this.httpClient.send(requestUser, HttpResponse.BodyHandlers.ofString());
		if (responseUser.statusCode() != 200) {
			throw new IOException("Bad response: " + responseUser.statusCode());
		}
		WeiboUser user = JsonUtil.readJson(responseUser.body(), WeiboUser.class);
		return new OAuthAuthentication() {
			@Override
			public String getAuthenticationId() {
				return auth.uid;
			}

			@Override
			public String getAccessToken() {
				return auth.access_token;
			}

			@Override
			public Duration getExpires() {
				return Duration.ofSeconds(auth.expires_in);
			}

			@Override
			public String getName() {
				return user.screen_name;
			}

			@Override
			public String getProfileUrl() {
				return "https://weibo.com/" + (user.domain == null ? user.idstr : user.domain);
			}

			@Override
			public String getImageUrl() {
				return user.profile_image_url;
			}
		};
	}

	public static class WeiboAuth {

		public long expires_in;

		public String access_token;

		public String uid;

	}

	public static class WeiboUser {

		public String screen_name;

		public String domain;

		public String idstr;

		public String profile_image_url;
	}

	@Override
	public AuthProviderType getProvider() {
		// TODO Auto-generated method stub
		return AuthProviderType.WEIBO;
	}

}
