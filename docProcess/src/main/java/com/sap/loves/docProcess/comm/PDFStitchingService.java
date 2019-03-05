package com.sap.loves.docProcess.comm;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

		String response = "";
		String files = "[";
		String stitchedPdfName = "";
		for (int i = 0; i < filenames.size(); i++) {
			log.info("Log No." + String.valueOf(context.counter) + " " + filenames.get(i) + " stitched!");
			stitchedPdfName += filenames.get(i) + "_";
			files += "{\"name\":\"" + filenames.get(i) + ".pdf\"},";
		}
		files = files.substring(0, files.length() - 1) + "]";
		stitchedPdfName = stitchedPdfName.substring(0, stitchedPdfName.length() - 1) + ".pdf";

		log.info("Log No." + String.valueOf(context.counter) + " files: " + files);
		log.info("Log No." + String.valueOf(context.counter) + " pdf name: " + stitchedPdfName);

		String requestJson = "{\"object_store_api\": \"" + object_store_api + "\", \"outputname\": \"" + stitchedPdfName + "\", \"files\": " + files + "}";

		log.info("Log No." + String.valueOf(context.counter) + " payload: " + requestJson);

		// Call PDF Stitching service via RestTemplate
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

		try {
			response += restTemplate.postForObject(url, entity, String.class);
		} catch (RuntimeException e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}

		if (response.equals("200")) {
			log.info("Log No." + String.valueOf(context.counter) + " All pdf files have been stitched in "
					+ stitchedPdfName + ".");
		} else {
			log.info("Log No." + String.valueOf(context.counter) + " Failed to stitched pdf files document. ");
		}
		return context;
	}

	@Override
	public Context fallBack() {
		log.info("Log No." + String.valueOf(context.counter) + " Failed to stitched pdf files document. ");
		return context;
	}

}

