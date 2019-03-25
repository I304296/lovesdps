package com.example.utility;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.identity.client.UaaContext;
import org.cloudfoundry.identity.client.UaaContextFactory;
import org.cloudfoundry.identity.client.token.GrantType;
import org.cloudfoundry.identity.client.token.TokenRequest;
import org.cloudfoundry.identity.uaa.oauth.token.CompositeAccessToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import com.example.demo.Controller;

public class DestinationProxy {
	
	final static Logger log = LoggerFactory.getLogger(DestinationProxy.class);
	final String DestinationAPIPath = "/destination-configuration/v1/destinations/";
	private String DestinationName;

	public DestinationProxy(String DestinationName) {
		super();
		this.DestinationName = DestinationName;
	}

	public String getDestinationName() {
		return DestinationName;
	}

	public void setDestinationName(String DestinationName) {
		this.DestinationName = DestinationName;
	}
	
	public JSONObject getProperties() {
		JSONObject properties = new JSONObject();
		try {
		String DestApiUri = getDestinationUri()+DestinationAPIPath+this.getDestinationName();
		URL url;
		
			url = new URL(DestApiUri);
		
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		CompositeAccessToken accessToken = getAccessToken();
		urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
		urlConnection.setConnectTimeout(10000);
		urlConnection.setReadTimeout(60000);
		 
		urlConnection.connect();
		InputStream in = urlConnection.getInputStream();
		properties = new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8));
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return properties;
	}
	
	private String getClientOAuthToken() throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			throw new Exception("User not authenticated");
		}
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) auth.getDetails();
		return details.getTokenValue();
	}
	
	public String getDestinationUri() {	
		String uri = "";
		try {
			JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
			JSONArray jsonArr = jsonObj.getJSONArray("destination");
			JSONObject destinationCredentials = jsonArr.getJSONObject(0).getJSONObject("credentials");
			uri = destinationCredentials.getString("uri");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return uri;
	}
	
	private JSONObject getServiceCredentials(String serviceName) throws JSONException {
		JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
		JSONArray jsonArr = jsonObj.getJSONArray(serviceName);
		return jsonArr.getJSONObject(0).getJSONObject("credentials");
	}
	
	// Get JWT token for the connectivity service from UAA
	private CompositeAccessToken getAccessToken() {
		CompositeAccessToken token = null;
		try {
		JSONObject connectivityCredentials = getServiceCredentials("destination");
		String clientId = connectivityCredentials.getString("clientid");
		String clientSecret = connectivityCredentials.getString("clientsecret");
		log.info("clientid:"+clientId+"clientsecret:"+clientSecret);
		// Make request to UAA to retrieve JWT token
		JSONObject xsuaaCredentials = getServiceCredentials("xsuaa");
		URI xsUaaUri = new URI(xsuaaCredentials.getString("url"));

		log.info("xsUaaUri:"+xsUaaUri);
		
		UaaContextFactory factory = UaaContextFactory.factory(xsUaaUri).authorizePath("/oauth/authorize")
				.tokenPath("/oauth/token");

		TokenRequest tokenRequest = factory.tokenRequest();
		tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
		tokenRequest.setClientId(clientId);
		tokenRequest.setClientSecret(clientSecret);
		log.info("Calling XSUAA:Authenticating...");
		UaaContext xsUaaContext = factory.authenticate(tokenRequest);
		log.info("xsUaa:"+xsUaaContext.getToken().getValue());
		log.info("xsUaa:"+xsUaaContext.getToken().getIdTokenValue());
		
		token = xsUaaContext.getToken();
		}catch(JSONException | URISyntaxException e) {
			log.error(e.getMessage());
		}
		return token;
	}
	
	
}
