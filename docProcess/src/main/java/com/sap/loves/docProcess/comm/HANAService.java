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
import com.sap.loves.docProcess.pojo.Status;

import java.io.StringReader;
import javax.xml.bind.*;

public class HANAService implements IServer {
	private Context context;
	private String baseUrl;
	private String entity;
	private String operation;
	private String filter;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public HANAService(Context context, String baseUrl, String entity, String operation) {
		this.context = context;
		this.baseUrl = baseUrl;
		this.entity = entity;
		this.operation = operation;
	}

	@Override
	public Context execute() {
		// Implement OData Call
		String url = "";
		if (operation == "POST") {
			url = baseUrl + entity;
		} else if (operation == "PUT" || operation == "GET") {
			Status status = context.getStatus();
			url = baseUrl + entity + "(GUID=\'" + status.getGUID() + "\',loadNo=\'" + status.getLoadNo()
					+ "\',debtorName=\'" + status.getDebtorName() + "\',date=\'" + status.getDate()
					+ "\',documentType=\'" + status.getDocumentType() + "\')";
		}
		
//		log.info("Log No." + String.valueOf(context.counter) + " HANA URL: " + url);

		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper mapper = new ObjectMapper();

		String entityJson = "";
		if (entity == "Status") {
			try {
				entityJson = mapper.writeValueAsString(context.getStatus());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		} else if (entity == "RateConfirmation") {
			try {
				entityJson = mapper.writeValueAsString(context.getRc());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		} else if (entity == "BillOfLading") {
			try {
				entityJson = mapper.writeValueAsString(context.getBol());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<String>(entityJson, headers);

		ResponseEntity<String> response = null;

		try {
			if(operation == "POST") {
				response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			}else if(operation == "PUT") {
				response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
			}else if(operation == "GET") {
				response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
				log.info("Log No." + String.valueOf(context.counter) + "Response Code: " + response.getStatusCode());
				if(response.getStatusCode() == HttpStatus.OK) {
					log.info("Log No." + String.valueOf(context.counter) + "Duplicated Status Data Exist");
					context.setExist(true);
				}
			}
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	@Override
	public Context fallBack() {
		// Default implementation
		log.info("Log No." + String.valueOf(context.counter) + " Failed to " + operation + " " + entity);

		return context;
	}

}
