package com.sap.loves.docProcess.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.loves.docProcess.biz.ProcessDocuments;
import com.sap.loves.docProcess.pojo.Document;
import com.sap.loves.docProcess.pojo.Load;
import com.sap.loves.docProcess.pojo.Message;
import com.sap.loves.docProcess.pojo.Page;

/**************************************************************
 * 
 * @author I304296
 * Spring Boot Controller
 * 
 * 
 *************************************************************/

@RestController
@RequestMapping("/rest/api/load")
public class ApiController {
	//Logging
	final static Logger log = LoggerFactory.getLogger(ApiController.class);
	
	@GetMapping
	public ResponseEntity<Load> doGet() {
		Load load = defaultResponseObject();
		return new ResponseEntity<Load>(load, HttpStatus.OK);

	}
	
	@PostMapping(	consumes = {MediaType.APPLICATION_JSON_VALUE}	)
	public ResponseEntity<Message> doPost(@RequestBody Load requestedLoad) {
		Message returnMessage = new Message(200,"Payload Recieved");
		
		try {
			//test
			//Thread.sleep(5000);
			returnMessage = new ProcessDocuments().doProcess(requestedLoad);
		}catch(RuntimeException e) {
			//Log message
			log.error(e.getMessage());
		} /*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
//		LoadReturn responseLoad = new LoadReturn(returnMessage);
		return new ResponseEntity<Message>(returnMessage,HttpStatus.CREATED);
	}
	
	//Dummy implementation
	public Load defaultResponseObject() {
		int pageNo = 0;
		Page[] pages = { new Page(++pageNo,"JPEG", "Base64EncodedContent1"),
				      new Page(++pageNo,"PNG", "Base64EncodedContent1")
		};
		Document d1 = new Document(
				"RC", 
		       // "JPEG",
		        "Dallas",
		        "New Orleans",
		        "Ship Co",
		        "RECV Company inc",
		        pages
		);
		Document d2 = new Document(
				"BOL", 
		       // "JPEG",
		        "Dallas",
		        "Houston	",
		        "Ship Co",
		        "RECV Company inc",
		        pages
		);
		Document[] documents = {d1,d2};
		
		return new Load(
				"ABCD1234",
				"L0001",
				"ABC Corp",
				"1200 USD",
				"BCD Transports LLC",
				"YYYYMMDD",
				documents
				);		
	}

}
