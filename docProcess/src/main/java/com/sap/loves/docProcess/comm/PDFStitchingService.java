package com.sap.loves.docProcess.comm;

import java.util.List;

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

public class PDFStitchingService implements IServer {
	private String url;
	private String object_store_api;
	private Context context;
	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public PDFStitchingService(Context context, String url, String object_store_api) {
		this.context = context;
		this.object_store_api = object_store_api;
		this.url = url;
	}

	@Override
	public Context execute() {
		List<String> filenames = context.getLoad().getFilenames();

		String files = "[";
		String stitchedPdfName = context.getLoad().getStitchedPdfName();
		for (int i = 0; i < filenames.size(); i++) {
//			log.info("Log No." + String.valueOf(context.counter) + " " + filenames.get(i) + " stitched!");
			// stitchedPdfName += filenames.get(i) + "_";
			files += "{\"name\":\"" + filenames.get(i) + ".pdf\"},";
		}
		files = files.substring(0, files.length() - 1) + "]";
		stitchedPdfName += context.getLoad().getDebtorName() + "_" + context.getLoad().getLoadNo() + "_"
				+ context.getLoad().getDate() + ".pdf";

		// log.info("Log No." + String.valueOf(context.counter) + " files: " + files);
//		log.info("Log No." + String.valueOf(context.counter) + " pdf name: " + stitchedPdfName);

		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"outputname\": \"" + stitchedPdfName
				+ "\", \"files\": " + files + "}";

		log.info("Log No." + String.valueOf(context.counter) + " payload for stitching: " + requestJson);

		// Call PDF Stitching service via RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(requestJson, headers);
		ResponseEntity<String> response = null;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			return updateStatus(context);
		}

		if (response.getStatusCode() == HttpStatus.OK) {
			log.info("Log No." + String.valueOf(context.counter) + " All PDF files have been stitched. ");
			return context;
		}

		log.info("Log No." + String.valueOf(context.counter) + " Failed to stitched pdf files document: " + response.getBody());
		return updateStatus(context);
	}

	@Override
	public Context fallBack() {
		log.info("Log No." + String.valueOf(context.counter) + " Fallback Failed to stitched pdf files document. ");
		return updateStatus(context);
	}

	public Context updateStatus(Context context) {
		String statusCode = "11";
		String statusDescription = "PDF Stitching Failed";
		context.getStatus().setStatus(statusCode);
		context.getStatus().setStatusDescription(statusDescription);
		return context;
	}

}
