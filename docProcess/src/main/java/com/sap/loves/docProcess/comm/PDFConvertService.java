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

public class PDFConvertService implements IServer {
	private String url;
	private String object_store_api;
	private Context context;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public PDFConvertService(Context context, String url, String object_store_api) {
		super();
		this.url = url;
		this.object_store_api = object_store_api;
		this.context = context;
	}

	@Override
	public Context execute() {
		String filename = context.getStatus().getFileName();
//		log.info("Log No." + String.valueOf(context.counter) + " Converting images to a PDF file " +  documentName);
		String[] parts = filename.split("/");
		String folder = parts[0];
		String realFilename = parts[1];
		
		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"folder\": \"" + folder + "\", \"filename\": \"" + realFilename
				+ "\", \"pages\": [";
		String imageContent = "";

		// Loop through Document[index] get pages and send that to PDF Converter service
		for (int i = 0; i < context.getLoad().getDocuments()[context.getIndex()].getPages().length; i++) {
			// Reconcile all page contents
			imageContent = context.getLoad().getDocuments()[context.getIndex()].getPages()[i].getContent();
			requestJson += "{ \"content\": \"" + imageContent + "\"},";
		}
		requestJson = requestJson.substring(0, requestJson.length() - 1);
		requestJson += "]}";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);
		ResponseEntity<String> response = null;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			context = updateContextStatusDescription(context, "| Exception during pdf conversion API call ");
		}

		if (response.getStatusCode() != HttpStatus.OK) {
			context = updateContextStatusDescription(context, "| Error during pdf conversion API call at ");
		}

		return context;
	}

	@Override
	public Context fallBack() {
		log.error("Log No." + String.valueOf(context.counter) + " Failed to convert images to a PDF file.");
		context = updateContextStatusDescription(context, "| Fallback during pdf conversion API call ");
		return context;
	}

	public Context updateContextStatusDescription(Context context, String description) {
		String message = context.getStatus().getStatusDescription();
		message += description;
		context.getStatus().setStatusDescription(message);
		context.getStatus().setStatus("3");
		return context;
	}

}
