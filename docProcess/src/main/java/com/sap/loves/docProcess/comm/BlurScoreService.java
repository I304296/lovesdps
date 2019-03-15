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

import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class BlurScoreService implements IServer {
	private String url;
	private Context context;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public BlurScoreService(Context context, String url) {
		this.context = context;
		this.url = url;
	}

	@Override
	public Context execute(){
		String base64content = context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()]
				.getContent();

		ResponseEntity<String> response = null;
		String requestJson = "{\"img\":\"";
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		requestJson += base64content + "\"}";

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			context = updateContextStatusDescription(context, "| Exception during blur detection api call");
		}
		
		if (response.getStatusCode() == HttpStatus.OK) {
			double score = Double.parseDouble(response.getBody());
			context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].setBlurScore(score);
		} else {
			context = updateContextStatusDescription(context, "| Error during blur detection api call");
		}
		
		return context;
	}

	@Override
	public Context fallBack() {
		// Default implementation
		log.info("Log No." + String.valueOf(context.counter) + " Failed to retrieve blurness score for page "
				+ context.getPageIndex() + " document: " + context.getStatus().getFileName()
				+ "Setting the default score 2000");
		context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].setBlurScore(2000);
		context = updateContextStatusDescription(context, "| Fallback during blur detection api call");
		
		return context;
	}
	
	public Context updateContextStatusDescription(Context context, String description) {
		String message = context.getStatus().getStatusDescription();
		message += description;
		context.getStatus().setStatusDescription(message);
		return context;
	}

}
