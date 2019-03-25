package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class BillOfLadingHANAService implements IServer {
	private Context context;
	private String url;
	private String operation;

	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public BillOfLadingHANAService(Context context, String url, String operation) {
		this.context = context;
		this.url = url;
		this.operation = operation;
	}

	@Override
	public Context execute() {
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper mapper = new ObjectMapper();

		String entityJson = "";

		try {
			entityJson = mapper.writeValueAsString(context.getBol());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<String>(entityJson, headers);

		ResponseEntity<String> response = null;
		
		log.info("Log No." + String.valueOf(context.counter) + " About to send Bill of Lading data with url: " + url);
		log.info("Log No." + String.valueOf(context.counter) + " About to send Bill of Lading data with payload: " + entityJson);

		response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		if(response.getStatusCode() == HttpStatus.CREATED) {
			log.info("Log No." + String.valueOf(context.counter) + " Successfully " +  operation + " Bill of Lading");
		}else {
			log.error("Log No." + String.valueOf(context.counter) + " Failed to " +  operation +  " Bill of Lading: " + response.getBody());
		}

		return context;
	}

	@Override
	public Context fallBack() {
		log.error("Log No." + String.valueOf(context.counter) + " Fallback - Failed to " +  operation +  " Bill of Lading.");
		return context;
	}

}
