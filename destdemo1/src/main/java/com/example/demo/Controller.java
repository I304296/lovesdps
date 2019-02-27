package com.example.demo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.utility.DestinationProxy;

@RestController
public class Controller {
	
	final static Logger log = LoggerFactory.getLogger(Controller.class);
	
	@GetMapping
	@RequestMapping("/destination")
	public String server(@RequestParam String name) {
		String ret = "";		
		DestinationProxy dp = new DestinationProxy(name);
		ret = dp.getProperties().toString();
		
		return ret;
	}
	

}
