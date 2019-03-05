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

		// log.info("Log No." + String.valueOf(context.counter) + " pdf conversion api:
		// " + url);
		// log.info("Log No." + String.valueOf(context.counter) + " object store api: "
		// + object_store_api);

		String documentName = context.getStatus().getFileName();
		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"filename\": \"" + documentName
				+ "\", \"pages\": [";
		String imageContent = "";

		// Loop through Document[index] get pages and send that to PDF Converter service
		for (int i = 0; i < context.getLoad().getDocuments()[context.getIndex()].getPages().length; i++) {
			// Reconcile all page contents
			imageContent = context.getLoad().getDocuments()[context.getIndex()].getPages()[i].getContent();
			// log.info("Log No." + String.valueOf(context.counter) + " Image[" +
			// context.getIndex() +"][" + i + "] content: " + imageContent);

			requestJson += "{ \"content\": \"" + imageContent + "\"},";
		}
		requestJson = requestJson.substring(0, requestJson.length() - 1);
		requestJson += "]}";

		// log.info("Log No." + String.valueOf(context.counter) + " JSON Payload for
		// document[" + context.getIndex() +"] conversion: " + requestJson);
		// Call PDF Convert service via RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);
		ResponseEntity<String> response = null;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
		}

		if (response.getStatusCode() == HttpStatus.OK) {
			return context;
		}

		log.error("Log No." + String.valueOf(context.counter) + " Failed to convert images to PDF files.");
		return updateStatus(context);
	}

	@Override
	public Context fallBack() {
		log.error("Log No." + String.valueOf(context.counter) + " Failed to convert images to PDF files.");
		return updateStatus(context);
	}

	public Context updateStatus(Context context) {
		String statusCode = "3";
		String statusDescription = "PDF Conversion Failed";
		context.getStatus().setStatus(statusCode);
		context.getStatus().setStatusDescription(statusDescription);
		return context;
	}

}
