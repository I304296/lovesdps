package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class ContrastEnhanceService implements IServer {
    private String url;
    private Context context;
    
    final static Logger log = LoggerFactory.getLogger(ApiController.class);
    
    public ContrastEnhanceService(Context context, String url) {
    	this.context = context;
    	this.url = url;
    }
    
	@Override
	public Context execute() {
//		log.info("Log No." + String.valueOf(context.counter) + " Enhancing Doc Index:"+String.valueOf(context.getIndex())+" Page Index:"+String.valueOf(context.getPageIndex()));
		String base64content = context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].getContent();
		//String base64content = context.getLoad().getDocuments()[0].getPages()[0].getContent();
		// Call Contrast service using RestTemplate, get enhanced image and update context
		String response = "";
		String requestJson = "{\"alpha\": 1.5, \"img\":\"";
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		requestJson += base64content + "\"}";

		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

		try {
			response += restTemplate.postForObject(url, entity, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " +  e.getMessage());
		}

		context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].setContent(response);
//		log.info("Log No." + String.valueOf(context.counter) + " Image Doc Index: "+String.valueOf(context.getIndex())+" Page Index: "+String.valueOf(context.getPageIndex()) + " has been enahnced" );
		
		return context;
	}

	@Override
	public Context fallBack() {
		// Default implementation
		log.info("Log No." + String.valueOf(context.counter) + " Failed to enchance the image. Doc Index: "+String.valueOf(context.getIndex())+" Page Index: "+String.valueOf(context.getPageIndex()));

		return context;
	}

}

