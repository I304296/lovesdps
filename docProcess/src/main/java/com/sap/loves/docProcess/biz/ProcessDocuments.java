package com.sap.loves.docProcess.biz;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.comm.BlurScoreService;
import com.sap.loves.docProcess.comm.ContrastEnhanceService;
import com.sap.loves.docProcess.comm.HANAService;
import com.sap.loves.docProcess.comm.PDFConvertService;
import com.sap.loves.docProcess.comm.RemoteCall;
import com.sap.loves.docProcess.comm.TestServer;
import com.sap.loves.docProcess.pojo.BillOfLading;
import com.sap.loves.docProcess.pojo.Context;
import com.sap.loves.docProcess.pojo.Load;
import com.sap.loves.docProcess.pojo.Message;
import com.sap.loves.docProcess.pojo.RateConfirmation;
import com.sap.loves.docProcess.pojo.Status;

public class ProcessDocuments {

	final String statusCodeInitiated = "0";
	final String statusCodeBlurred = "1";
	final String statusCodeConrastFailed = "2";
	final String statusCodePDFConvertFailed = "3";
	final String statusCodeObjectStoreOpsFailed = "4";
	final String statusCodeObjectStoreOpsReady = "5";
	final String statusCodeResubmission = "9";
	ExecutorService cpuBound = Executors.newFixedThreadPool(4);
	ExecutorService ioBound = Executors.newCachedThreadPool();

	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public Message doProcess(Load load) throws RuntimeException{
		Message msg = new Message(200,"ok");
		try {
			for(int i=0; i<load.getDocuments().length; i++) {

				//Initialize Context data
				Context ctx = new Context(i);
				ctx.setLoad(load);
				ctx.counter = 0;

				//Do processing logic for the document
				//1. Save Load status data				
				CompletableFuture.supplyAsync(() -> prepareStatusData(ctx), ioBound)
				.thenApply(contextData -> saveStatusData(contextData))
				//2. Save RC 
				.thenApply(contextData -> prepareRCData(contextData))
				.thenApply(contextData -> saveRCData(contextData))
				//3. Save BOL
				.thenApply(contextData -> prepareBOLData(contextData))
				.thenApply(contextData -> saveBOLData(contextData))
				//4. Call Blur detection per page
				.thenApply(contextData -> checkBlurScore(contextData))
				//5. Call Contrast enhancement per page 
				.thenApply(contextData -> enhanceContrast(contextData))
				//6. Convert Image to PDF and store to Object Store
				.thenApply(contextData -> convertPDFandSaveToObjectStore(contextData))
				//7. Finally Update status
				.thenApply(contextData -> checkProcessStatus(contextData))
				
				//8. Stitch PDF
				.thenAccept(contextData -> stitchPDF(contextData));
				
			}

			
		}catch(Exception e) {
			//Log message
			log.error(e.getMessage());			
		}
		return msg;
	}

	//1.Save Load status data
	public Context prepareStatusData(Context context) {
		this.log.info(String.valueOf(context.counter++)+":Preparing Status Data");
		String statusCode = statusCodeInitiated;
		String statusDescription = "Initial Posting";

		/*		//test
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		//Check if any entry exists in DB for same Load number, if so this is a Re-submission
		if(loadEntryExists(context)) {
			statusCode = statusCodeResubmission;
			statusDescription = "Re-submission";	
		}
		this.log.info(String.valueOf(context.counter)+"Status:"+statusCode);

		//Generate file name
		String fileName = context.getLoad().getDocuments()[context.getIndex()].getDocumentType()
				+"_"
				+context.getLoad().getDebtorName()
				+"_"
				+context.getLoad().getLoadNo()
				+"_"
				+context.getLoad().getDate();
		
		this.log.info(String.valueOf(context.counter)+"Filename:"+fileName);
		
		//Map status data
		Status statusdata = new Status(
				context.getLoad().getGUID(),
				context.getLoad().getLoadNo(),
				context.getLoad().getDebtorName(),
				context.getLoad().getDate(), 
				context.getLoad().getDocuments()[context.getIndex()].getDocumentType(),
				fileName,
				context.getLoad().getDocuments().length,
				statusCode,
				statusDescription					
				);
		this.log.info(String.valueOf(context.counter)+"Setting StatusData: doc type:"+context.getLoad().getDocuments()[context.getIndex()].getDocumentType());
		//Map to Context data structure
		context.setStatus(statusdata);
		//this.log.info(String.valueOf(context.counter)+"StatusData:"+context.getStatus().toString());

		return context;
	}

	public HystrixCommand.Setter getHystrixConfig() {
		HystrixCommand.Setter config = HystrixCommand
				.Setter
				.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupThreadPool"));
		HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
		commandProperties.withExecutionTimeoutInMilliseconds(2000);
		commandProperties.withCircuitBreakerSleepWindowInMilliseconds(4000);
		commandProperties.withExecutionIsolationStrategy
		(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		commandProperties.withCircuitBreakerEnabled(true);
		commandProperties.withCircuitBreakerRequestVolumeThreshold(1);
		commandProperties.withFallbackEnabled(true);

		config.andCommandPropertiesDefaults(commandProperties);
		config.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
				.withMaxQueueSize(10)
				.withCoreSize(4)
				.withQueueSizeRejectionThreshold(10));
		return config;
	}
	//Save Status via OData POST
	public Context saveStatusData(Context context) {
		this.log.info(String.valueOf(context.counter++)+":Saving Status Data");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info("Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new HANAService(context, 
							"https://hana-odata-server.com/","StatusEntity", "POST"), context).execute();

		}catch(Exception e) {
			log.error(e.getMessage());
		}
		return context;
	}
	//Check if entry exists via ODATA GET
	public boolean loadEntryExists(Context context) {
		boolean exists = false;
		this.log.info(String.valueOf(context.counter++)+":Check Status Data");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info(String.valueOf(context.counter)+"Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new HANAService(context, 
							"https://hana-odata-server.com/","StatusEntity", "GET"), context).execute();

			this.log.info(String.valueOf(context.counter)+"DB Status record:"+context.getStatusDBRecord().getLoadNo());
			this.log.info(String.valueOf(context.counter)+"Status record:"+context.getLoad().getLoadNo());
			this.log.info(String.valueOf(context.counter)+"DB Status record:"+context.getStatusDBRecord().getDebtorName());
			this.log.info(String.valueOf(context.counter)+"Status record:"+context.getLoad().getDebtorName());
			//if(context.getStatusDBRecord() != null) {
				if(context.getStatusDBRecord().getLoadNo().equals(context.getLoad().getLoadNo()) &&
						context.getStatusDBRecord().getDebtorName().equals(context.getLoad().getDebtorName())) 
				{
					exists = true;
				}
			//}
		}catch(Exception e) {
			log.error(e.getMessage());
		}

		return exists;
	}

	public Context updateStatus(Context context) {
		this.log.info(String.valueOf(context.counter++)+":Updating Status Data");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info(String.valueOf(context.counter)+"Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new HANAService(context, 
							"https://hana-odata-server.com/","StatusEntity", "PATCH"), context).execute();

			this.log.info(String.valueOf(context.counter)+"Update remote call success!");
			
		}catch(Exception e) {
			log.error(e.getMessage());
		}
		return context;
	}

	//2. Save RC
	public Context prepareRCData(Context context) {
		context.setRc(new RateConfirmation(
				context.getLoad().getGUID(),       //GUID
				context.getLoad().getLoadNo(),     //loadNo;
				context.getLoad().getDebtorName(), //debtorName;
				context.getLoad().getDate(),       //date;
				"",                                //documentConfidence;
				context.getLoad().getAmount(),     //amount;
				"",                                //amountML;
				"",                                //amountConfidence;
				context.getLoad().getCarrierName(),// carrierName;
				"",                                //carrierNameML;
				"",                                //carrierNameConfidence;
				"",                                //loadNoML;
				"",                                //loadNoConfidence;
				"",                                //debtorNameML;
				"",                                //debtorNameConfidence;
				"",                                //receiverNameML;
				"",                                //receiverNameConfidence;
				"",                                //shipperNameML;
				"",                                //shipperNameConfidence;
				"",                                //shipToML;
				"",                                //shipToConfidence;
				"",                                //shipFromML;
				""                                //shipFromConfidence;
				));
		return context;
	}

	public Context saveRCData(Context context) {
		this.log.info(String.valueOf(context.counter++)+":Saving Rate Confirmation Data");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info("Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new HANAService(context, 
							"https://hana-odata-server.com/","RCEntity", "POST"), context).execute();

		}catch(Exception e) {
			log.error(e.getMessage());
		}
		return context;		
	}

	public Context prepareBOLData(Context context) {

		/*	BillOfLading[] bols = new BillOfLading[context.getLoad().getDocuments().length];

		for(int i=0; i<context.getLoad().getDocuments().length;i++) {
		  bols[i] = new BillOfLading(
					context.getLoad().getGUID(),       //GUID;
					context.getLoad().getLoadNo(),     //loadNo;
					context.getLoad().getDebtorName(), //debtorName;
					context.getLoad().getDate(),       //date;
					UUID.randomUUID().toString(),  //BOLID
					context.getLoad().getDocuments()[i].getReceiverName(), //receiverName;
					"",                                //receiverNameML;
					"",                                //receiverNameConfidence;
					context.getLoad().getDocuments()[i].getShipperName(), //shipperName;
					"",                                //shipperNameML;
					"",                                //shipperNameConfidence;
					context.getLoad().getDocuments()[i].getShipTo(),      //shipTo;
					"",                                //shipToML;
					"",                                //shipToConfidence;
					context.getLoad().getDocuments()[i].getShipFrom(),                                //shipFrom;
					"",                                //shipFromML;
					""                                //shipFromConfidence;
					);		
		}*/
		context.setBol(new BillOfLading(
				context.getLoad().getGUID(),       //GUID;
				context.getLoad().getLoadNo(),     //loadNo;
				context.getLoad().getDebtorName(), //debtorName;
				context.getLoad().getDate(),       //date;
				UUID.randomUUID().toString(),  //BOLID
				context.getLoad().getDocuments()[context.getIndex()].getReceiverName(), //receiverName;
				"",                                //receiverNameML;
				"",                                //receiverNameConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipperName(), //shipperName;
				"",                                //shipperNameML;
				"",                                //shipperNameConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipTo(),      //shipTo;
				"",                                //shipToML;
				"",                                //shipToConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipFrom(),    //shipFrom                             //shipFrom;
				"",                                //shipFromML;
				""                                //shipFromConfidence;
				));
		return context;
	}

	public Context saveBOLData(Context context) {
		this.log.info(String.valueOf(context.counter++)+":Saving Bill of Lading Data");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info("Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new HANAService(context, 
							"https://hana-odata-server.com/","BOLEntity", "POST"), context).execute();

		}catch(Exception e) {
			log.error(e.getMessage());
		}
		return context;	
	}	

	public Context checkBlurScore(Context context) {
		//Skip this step for PDF
		if(context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		int pageIndex = 0;
		String message = "";
		this.log.info(String.valueOf(context.counter++)+":Get Blur score");
		//Set Hystrix properties
		HystrixCommand.Setter config = getHystrixConfig();		
		try {
			message += "Blur Page:";
			double thrshold = 2000.0; //Todo: Call method to get threshold from destination
			for(pageIndex=0; pageIndex<context.getLoad().getDocuments()[context.getIndex()].getPages().length;pageIndex++) {
				context.setPageIndex(pageIndex);
				this.log.info(String.valueOf(context.counter++)+":Call Blur Service");
				//Call Blur service
				context = new RemoteCall(config, 
						new BlurScoreService(context, 
								"https://blur-service.com/"), context).execute();
				//If image score less than threshold -> consider image blurry
				if(context.getLoad().getDocuments()[context.getIndex()].getPages()[pageIndex].getBlurScore() < thrshold) {
					message = message+ String.valueOf(pageIndex)+".";
					//Construct Status
					//Map status data
					Status statusdata = new Status(
							context.getStatus().getGUID(),
							context.getStatus().getLoadNo(),
							context.getStatus().getDebtorName(),
							context.getStatus().getDate(), 
							context.getStatus().getDocumentType(),
							context.getStatus().getFileName(),
							context.getStatus().getPageCount(),
							statusCodeBlurred,
							message					
							);
					
					//Update Context
					context.setStatus(statusdata);
					//Update DB
					context = updateStatus(context);
								
				}
			}
		}catch(Exception e) {
			log.error(e.getMessage());
			message += "Error during Blur Detection.Page:"+ String.valueOf(pageIndex)+".";
			//Construct Status
			//Map status data
			Status statusdata = new Status(
					context.getStatus().getGUID(),
					context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(),
					context.getStatus().getDate(), 
					context.getStatus().getDocumentType(),
					context.getStatus().getFileName(),
					context.getStatus().getPageCount(),
					statusCodeConrastFailed,
					message				
					);
			
			//Update Context
			context.setStatus(statusdata);
			//Update DB
			context = updateStatus(context);			
		}
		return context;	
	}

	public Context enhanceContrast(Context context) {
		//Skip this step for PDF
		if(context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		int pageIndex=0;
		String message="";
		this.log.info(String.valueOf(context.counter++)+":Contrast Enhancement");
		//Set Hystrix properties
		HystrixCommand.Setter config = getHystrixConfig();		
		try {
			for(pageIndex=0; pageIndex<context.getLoad().getDocuments()[context.getIndex()].getPages().length;pageIndex++) {
				context.setPageIndex(pageIndex);
				this.log.info("Contrast Enhancement call");
				context = new RemoteCall(config, 
						new ContrastEnhanceService(context, 
								"https://Contrast-service.com/"), context).execute();

			}
		}catch(Exception e) {
			log.error(e.getMessage());
			message += "Error during Contrast Enhancement.Page:"+ String.valueOf(pageIndex)+".";
			//Construct Status
			//Map status data
			Status statusdata = new Status(
					context.getStatus().getGUID(),
					context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(),
					context.getStatus().getDate(), 
					context.getStatus().getDocumentType(),
					context.getStatus().getFileName(),
					context.getStatus().getPageCount(),
					statusCodeConrastFailed,
					message				
					);
			
			//Update Context
			context.setStatus(statusdata);
			//Update DB
			context = updateStatus(context);
			
			
		}
		return context;	
	}

	public Context convertPDFandSaveToObjectStore(Context context) {
		//Skip this step for PDF
		if(context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		String message="";
		this.log.info(String.valueOf(context.counter++)+":Converting to PDF");
		try {
			//Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig();

			this.log.info("Call remote service via Hystrix");
			context = new RemoteCall(config, 
					new PDFConvertService(context, 
							"https://pdf-service.com/", false), context).execute();

		}catch(Exception e) {
			log.error(e.getMessage());
			message += "Error during PDF conversion";
			//Construct Status
			//Map status data
			Status statusdata = new Status(
					context.getStatus().getGUID(),
					context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(),
					context.getStatus().getDate(), 
					context.getStatus().getDocumentType(),
					context.getStatus().getFileName(),
					context.getStatus().getPageCount(),
					statusCodePDFConvertFailed,
					message				
					);
			
			//Update Context
			context.setStatus(statusdata);
			//Update DB
			context = updateStatus(context);

					
		}
		return context;	
	}
    public Context checkProcessStatus(Context context) {
    	this.log.info(String.valueOf(context.counter++)+":Set Status to Ready for ML pickup");
    	//If All the check passed successfully
    	this.log.info(String.valueOf(context.counter)+"StatusData:"+context.getStatus().getStatus());
    	
    	if(context.getStatus().getStatus().toString().equals(statusCodeInitiated)) {
    		
    		Status statusdata = new Status(
					context.getStatus().getGUID(),
					context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(),
					context.getStatus().getDate(), 
					context.getStatus().getDocumentType(),
					context.getStatus().getFileName(),
					context.getStatus().getPageCount(),
					statusCodeObjectStoreOpsReady,
					"Ready for ML Process"				
					);
    		this.log.info("GUID:"+context.getStatus().getGUID()
    				     +"LoadNo:"+context.getStatus().getLoadNo()
    				     +"Debtor:"+context.getStatus().getDebtorName()
    				     +"Date:"+context.getStatus().getDate()
    				     +"DocumentType:"+context.getStatus().getDocumentType()
    				     +"File Name:"+context.getStatus().getFileName()
    				     +"Page Count:"+context.getStatus().getPageCount()
    				     );
			//Update Context
			context.setStatus(statusdata);
			//Update DB
			context = updateStatus(context);
	
    	}
    	return context;
    }
    
    public Context stitchPDF(Context context) {
    	this.log.info(String.valueOf(context.counter++)+":Start stitching PDF");
    	this.log.info(String.valueOf(context.counter)+"Current Doc Index:"+String.valueOf(context.getIndex()));
    	this.log.info(String.valueOf(context.counter)+"Total Doc Index:"+String.valueOf(context.getLoad().getDocuments().length));
    	//Stitch only if all documents are processed
    	if(context.getIndex() == context.getLoad().getDocuments().length-1) {
        	this.log.info(String.valueOf(context.counter)+"Current Page Index:"+String.valueOf(context.getPageIndex()));
        	this.log.info(String.valueOf(context.counter)+"Total Page Index:"+String.valueOf(context.getLoad().getDocuments()[context.getIndex()].getPages().length));    		
    		//Stitch only after processing of last page
    		if(context.getPageIndex() == context.getLoad().getDocuments()[context.getIndex()].getPages().length-1){
    			String message="";
    			this.log.info(String.valueOf(context.counter)+":Stitch PDF");
    			try {
    				//Set Hystrix properties
    				HystrixCommand.Setter config = getHystrixConfig();

    				this.log.info("Call remote service via Hystrix");
    				context = new RemoteCall(config, 
    						new PDFConvertService(context, 
    								"https://pdf-service.com/", true), context).execute();

    			}catch(Exception e) {
    				log.error(e.getMessage());
    				message += "Error during PDF conversion";
    				//Construct Status
    				//Map status data
    				Status statusdata = new Status(
    						context.getStatus().getGUID(),
    						context.getStatus().getLoadNo(),
    						context.getStatus().getDebtorName(),
    						context.getStatus().getDate(), 
    						context.getStatus().getDocumentType(),
    						context.getStatus().getFileName(),
    						context.getStatus().getPageCount(),
    						statusCodePDFConvertFailed,
    						message				
    						);
    				
    				//Update Context
    				context.setStatus(statusdata);
    				//Update DB
    				context = updateStatus(context);
    						
    			}
    			
    		}
    	}
    	return context;
    }

}
