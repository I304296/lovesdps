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

public class FileStoreService implements IServer {
	
	private String url;
	private String object_store_api;
	private Context context;
	private String filename;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public FileStoreService(Context context, String url, String object_store_api, String filename) {
		this.url = url;
		this.object_store_api = object_store_api;
		this.context = context;
		this.filename = filename;
	}

	@Override
	public Context execute() {
		String fileContent = context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getContent();
		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"filename\": \"" + filename
				+ "\", \"content\": \"" + fileContent + "\"}";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);
		ResponseEntity<String> response = null;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " Failed to store MISC file: " + e.getMessage());
			context = updateContextStatusDescription(context, "| Error during saving file to object store");
		}
		
		if (response.getStatusCode() == HttpStatus.OK) {
//			log.info("Log No." + String.valueOf(context.counter) + " Successfully store MISC file.");
		}
		
		return context;
	}

	@Override
	public Context fallBack() {
		log.error("Log No." + String.valueOf(context.counter) + " Fallback - Failed to store MISC file.");
		context = updateContextStatusDescription(context, "| Fallback during saving file to object store");
		return context;
	}
	
	public Context updateContextStatusDescription(Context context, String description) {
		String message = context.getStatus().getStatusDescription();
		message += description;
		context.getStatus().setStatusDescription(message);
		context.getStatus().setStatus("4");
		return context;
	}

}
