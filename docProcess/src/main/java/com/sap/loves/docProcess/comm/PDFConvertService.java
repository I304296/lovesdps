package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class PDFConvertService implements IServer {
	private String url;
	private Context context;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public PDFConvertService(Context context, String url) {
		super();
		this.url = url;
		this.context = context;
	}

	@Override
	public Context execute() {

		String response = "";
		String documentName = context.getStatus().getFileName();
		String requestJson = "{\"filename\": \"" + documentName + "\", \"pages\": [";
		String imageContent = "";

		// Loop through Document[index] get pages and send that to PDF Converter service
		for (int i = 0; i < context.getLoad().getDocuments()[context.getIndex()].getPages().length; i++) {
			// Reconcile all page contents
			imageContent = context.getLoad().getDocuments()[context.getIndex()].getPages()[i].getContent();
			requestJson += "{ \"content\": \"" + imageContent + "\"},";
		}
		requestJson = requestJson.substring(0, requestJson.length() - 1);
		requestJson += "]}";

		// log.info("JSON Payload for document " + i +" conversion: " + requestJson);
		// Call PDF Convert service via RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

		try {
			response += restTemplate.postForObject(url, entity, String.class);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
		}

		if (response.equals("200")) {
			log.info("Log No." + String.valueOf(context.counter) + " All images in document[" + context.getIndex()
					+ "] has been saved as " + documentName + ".pdf to object store");
			// context.getStatus().setStatus(status);
		} else {
			log.info("Log No." + String.valueOf(context.counter) + " Failed to convert document " + context.getIndex()
					+ " to PDF");
		}

		return context;
	}

	@Override
	public Context fallBack() {
		// Update Status message in context stating that PDF Converter service is down
		log.info("Log No." + String.valueOf(context.counter) + " Failed to convert the image into PDF. Doc Index: "
				+ String.valueOf(context.getIndex()) + " Page Index: " + String.valueOf(context.getPageIndex()));
		return context;
	}

}

