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

public class PDFStoreService implements IServer {
	private String url;
	private String object_store_api;
	private Context context;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public PDFStoreService(Context context, String url, String object_store_api) {
		super();
		this.url = url;
		this.object_store_api = object_store_api;
		this.context = context;
	}

	@Override
	public Context execute() {
		String documentName = context.getStatus().getFileName() + ".pdf";
		String pdfContent = context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getContent();
		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"filename\": \"" + documentName
				+ "\", \"content\": \"" + pdfContent + "\"}";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);
		ResponseEntity<String> response = null;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			return updateStatus(context, "4", "Filed to send the PDF file to object store");
		}

		if (response.getStatusCode() == HttpStatus.OK) {
			return updateStatus(context, "5", "The PDF file is saved in object store");
		}

		log.error("Log No." + String.valueOf(context.counter) + " Failed to store PDF files.");
		return updateStatus(context, "4", "Filed to send the PDF file to object store");
	}

	@Override
	public Context fallBack() {
		log.error("Log No." + String.valueOf(context.counter) + " Failed to store PDF files.");
		return updateStatus(context, "4", "Filed to send the PDF file to object store");
	}

	public Context updateStatus(Context context, String code, String description) {
		context.getStatus().setStatus(code);
		context.getStatus().setStatusDescription(description);
		return context;
	}

}
