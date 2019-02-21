package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
	public Context execute() {
		String base64content = context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()]
				.getContent();
		// Call Blur service using RestTemplate

		String response = "";
		String requestJson = "{\"img\":\"";
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		requestJson += base64content + "\"}";

		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

		try {
			response += restTemplate.postForObject(url, entity, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}

		double score = Double.parseDouble(response);
//		log.info("Log No." + String.valueOf(context.counter) + " Blurness Score: "+ score +" Doc Index: "+String.valueOf(context.getIndex())+" Page Index: "+String.valueOf(context.getPageIndex()));
		
		context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].setBlurScore(score);

		// update context
		return this.context;
	}

	@Override
	public Context fallBack() {
		// Default implementation
		log.info("Log No." + String.valueOf(context.counter) + " Failed to retrieve blurness score. Setting the default score 2000");
		context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].setBlurScore(2000);
		return context;
	}

}
