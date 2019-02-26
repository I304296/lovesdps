package com.sap.loves.docProcess.biz;

import java.util.List;
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
import com.sap.loves.docProcess.comm.PDFStitchingService;
import com.sap.loves.docProcess.comm.RemoteCall;
import com.sap.loves.docProcess.comm.TestServer;
import com.sap.loves.docProcess.pojo.BillOfLading;
import com.sap.loves.docProcess.pojo.Context;
import com.sap.loves.docProcess.pojo.Load;
import com.sap.loves.docProcess.pojo.Message;
import com.sap.loves.docProcess.pojo.RateConfirmation;
import com.sap.loves.docProcess.pojo.Status;

import com.sap.loves.docProcess.utility.DestinationProxy;

import org.json.JSONException;
import org.json.JSONObject;

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
	private Load load;

	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public Message doProcess(Load load) throws RuntimeException {
		Message msg = new Message(200, "ok");
		try {
			this.load = load;
			for (int i = 0; i < load.getDocuments().length; i++) {
				// Initialize Context data
				Context ctx = new Context(i);
				ctx.setLoad(load);
				ctx.counter = 0;

				// Do processing logic for the document
				// 1. Save Load status data
				CompletableFuture.supplyAsync(() -> prepareStatusData(ctx), ioBound)
						.thenApply(contextData -> saveStatusData(contextData))
						// 2. Save RC
						.thenApply(contextData -> prepareRCData(contextData))
						.thenApply(contextData -> saveRCData(contextData))
						// 3. Save BOL
						.thenApply(contextData -> prepareBOLData(contextData))
						.thenApply(contextData -> saveBOLData(contextData))
						// 4. Call Blur detection per page
						.thenApply(contextData -> checkBlurScore(contextData))
						// 5. Call Contrast enhancement per page
						.thenApply(contextData -> enhanceContrast(contextData))
						// 6. Convert Image to PDF and store to Object Store
						.thenApply(contextData -> convertPDFandSaveToObjectStore(contextData))
						// 7. Finally Update status
						.thenApply(contextData -> checkProcessStatus(contextData))
						// 8. Stitch PDF
						.thenAccept(contextData -> stitchPDF(contextData));

			}

		} catch (Exception e) {
			// Log message
			log.error(e.getMessage());
		}
		return msg;
	}

	// 1.Save Load status data
	public Context prepareStatusData(Context context) {
		// this.log.info(String.valueOf(++context.counter)+":Preparing Status Data");
		log.info("Log No." + String.valueOf(++context.counter) + ": Preparing Status Data");

		String statusCode = statusCodeInitiated;
		String statusDescription = "Initial Posting";

		/*
		 * //test try { Thread.sleep(5000); } catch (InterruptedException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		// Check if any entry exists in DB for same Load number, if so this is a
		// Re-submission
		if (loadEntryExists(context)) {
			statusCode = statusCodeResubmission;
			statusDescription = "Re-submission";
		}
		// log.info("Log No." + String.valueOf(context.counter) + ": Status:" +
		// statusCode);

		// Generate file name
		String fileName = context.getLoad().getDocuments()[context.getIndex()].getDocumentType() + "_"
				+ context.getLoad().getDebtorName() + "_" + context.getLoad().getLoadNo() + "_"
				+ context.getLoad().getDate();

		List<String> filenames = this.load.getFilenames();
		// log.info("Log No." + String.valueOf(context.counter) + ": current file list "
		// + filenames + " ");

		// add filename into filenames
		filenames.add(fileName);
		this.load.setFilenames(filenames);
		// log.info("Log No." + String.valueOf(context.counter) + ": file " + fileName +
		// " has been added to list "
		// + filenames);

		// combined name = debtorName + LoadNo + Date + "_" + debotName ....
		String stitchedPdfName = this.load.getStitchedPdfName();
		stitchedPdfName += context.getLoad().getDebtorName() + "_" + context.getLoad().getLoadNo() + "_"
				+ context.getLoad().getDate() + "_";
		this.load.setStitchedPdfName(stitchedPdfName);

		// Map status data
		Status statusdata = new Status(context.getLoad().getGUID(), context.getLoad().getLoadNo(),
				context.getLoad().getDebtorName(), context.getLoad().getDate(),
				context.getLoad().getDocuments()[context.getIndex()].getDocumentType(), fileName,
				context.getLoad().getDocuments().length, statusCode, statusDescription);
		// log.info("Log No." + String.valueOf(context.counter) + ": Setting StatusData:
		// doc type:"
		// + context.getLoad().getDocuments()[context.getIndex()].getDocumentType());
		// Map to Context data structure
		context.setStatus(statusdata);
		// this.log.info(String.valueOf(context.counter)+"StatusData:"+context.getStatus().toString());

		return context;
	}

	public HystrixCommand.Setter getHystrixConfig(String DestinationName) {
		
		//Initialize with Default Value
		int ExecutionTimeoutInMilliseconds = 180000;
		int CircuitBreakerSleepWindowInMilliseconds = 4000;
		boolean CircuitBreakerEnabled = true;
		int CircuitBreakerRequestVolumeThreshold = 1;
		boolean FallbackEnabled = true;
		
		log.info("Inside getHystrixConfig. DestinationName="+DestinationName);
		
		//If destination name is supplied then get from destination
		if(DestinationName.length()>0 || !DestinationName.equals("default")) {
			DestinationProxy dp = new DestinationProxy(DestinationName);
			try {
				ExecutionTimeoutInMilliseconds =  Integer.parseInt(dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.ExecutionTimeoutInMilliseconds"));
				CircuitBreakerSleepWindowInMilliseconds = Integer.parseInt(dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.CircuitBreakerSleepWindowInMilliseconds"));
				CircuitBreakerRequestVolumeThreshold = Integer.parseInt(dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.CircuitBreakerRequestVolumeThreshold"));
				CircuitBreakerEnabled = Boolean.parseBoolean(dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.CircuitBreakerEnabled"));
				FallbackEnabled = Boolean.parseBoolean(dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.FallbackEnabled"));
				
				log.info("ExecutionTimeoutInMilliseconds:"+String.valueOf(ExecutionTimeoutInMilliseconds)+
						"|CircuitBreakerSleepWindowInMilliseconds:"+String.valueOf(CircuitBreakerSleepWindowInMilliseconds)+
						"|CircuitBreakerRequestVolumeThreshold:"+String.valueOf(CircuitBreakerRequestVolumeThreshold)+
						"|CircuitBreakerEnabled:"+String.valueOf(CircuitBreakerEnabled)+
						"|FallbackEnabled:"+String.valueOf(FallbackEnabled));
			
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error("Destination not configured:" + e.getMessage());
				e.printStackTrace();
			}
			
		}
		HystrixCommand.Setter config = HystrixCommand.Setter
				.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupThreadPool"));
		HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
		commandProperties.withExecutionTimeoutInMilliseconds(ExecutionTimeoutInMilliseconds);
		commandProperties.withCircuitBreakerSleepWindowInMilliseconds(CircuitBreakerSleepWindowInMilliseconds);
		commandProperties.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		commandProperties.withCircuitBreakerEnabled(CircuitBreakerEnabled);
		commandProperties.withCircuitBreakerRequestVolumeThreshold(CircuitBreakerRequestVolumeThreshold);
		commandProperties.withFallbackEnabled(FallbackEnabled);

		config.andCommandPropertiesDefaults(commandProperties);
		config.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withMaxQueueSize(10).withCoreSize(4)
				.withQueueSizeRejectionThreshold(10));
		return config;
	}

	// Save Status via OData POST
	public Context saveStatusData(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Saving Status Data");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new HANAService(context, "https://hana-odata-server.com/", "StatusEntity", "POST"), context)
							.execute();

		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + ": " + e.getMessage());
		}
		return context;
	}

	// Check if entry exists via ODATA GET
	public boolean loadEntryExists(Context context) {
		boolean exists = false;
		log.info("Log No." + String.valueOf(++context.counter) + ": Check Status Data");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new HANAService(context, "https://hana-odata-server.com/", "StatusEntity", "GET"), context)
							.execute();

			// log.info("Log No." + String.valueOf(context.counter) + "DB Status record:"
			// + context.getStatusDBRecord().getLoadNo());
			// log.info("Log No." + String.valueOf(context.counter) + "Status record:" +
			// context.getLoad().getLoadNo());
			// log.info("Log No." + String.valueOf(context.counter) + "DB Status record:"
			// + context.getStatusDBRecord().getDebtorName());
			// log.info(
			// "Log No." + String.valueOf(context.counter) + "Status record:" +
			// context.getLoad().getDebtorName());
			// if(context.getStatusDBRecord() != null) {
			if (context.getStatusDBRecord().getLoadNo().equals(context.getLoad().getLoadNo())
					&& context.getStatusDBRecord().getDebtorName().equals(context.getLoad().getDebtorName())) {
				exists = true;
			}
			// }
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return exists;
	}

	public Context updateStatus(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Updating Status Data");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new HANAService(context, "https://hana-odata-server.com/", "StatusEntity", "PATCH"), context)
							.execute();

			// log.info(String.valueOf(context.counter)+"Update remote call success!");

		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	// 2. Save RC
	public Context prepareRCData(Context context) {
		context.setRc(new RateConfirmation(context.getLoad().getGUID(), // GUID
				context.getLoad().getLoadNo(), // loadNo;
				context.getLoad().getDebtorName(), // debtorName;
				context.getLoad().getDate(), // date;
				"", // documentConfidence;
				context.getLoad().getAmount(), // amount;
				"", // amountML;
				"", // amountConfidence;
				context.getLoad().getCarrierName(), // carrierName;
				"", // carrierNameML;
				"", // carrierNameConfidence;
				"", // loadNoML;
				"", // loadNoConfidence;
				"", // debtorNameML;
				"", // debtorNameConfidence;
				"", // receiverNameML;
				"", // receiverNameConfidence;
				"", // shipperNameML;
				"", // shipperNameConfidence;
				"", // shipToML;
				"", // shipToConfidence;
				"", // shipFromML;
				"" // shipFromConfidence;
		));
		return context;
	}

	public Context saveRCData(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Saving Rate Confirmation Data");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new HANAService(context, "https://hana-odata-server.com/", "RCEntity", "POST"), context).execute();

		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	public Context prepareBOLData(Context context) {

		/*
		 * BillOfLading[] bols = new
		 * BillOfLading[context.getLoad().getDocuments().length];
		 * 
		 * for(int i=0; i<context.getLoad().getDocuments().length;i++) { bols[i] = new
		 * BillOfLading( context.getLoad().getGUID(), //GUID;
		 * context.getLoad().getLoadNo(), //loadNo; context.getLoad().getDebtorName(),
		 * //debtorName; context.getLoad().getDate(), //date;
		 * UUID.randomUUID().toString(), //BOLID
		 * context.getLoad().getDocuments()[i].getReceiverName(), //receiverName; "",
		 * //receiverNameML; "", //receiverNameConfidence;
		 * context.getLoad().getDocuments()[i].getShipperName(), //shipperName; "",
		 * //shipperNameML; "", //shipperNameConfidence;
		 * context.getLoad().getDocuments()[i].getShipTo(), //shipTo; "", //shipToML;
		 * "", //shipToConfidence; context.getLoad().getDocuments()[i].getShipFrom(),
		 * //shipFrom; "", //shipFromML; "" //shipFromConfidence; ); }
		 */
		context.setBol(new BillOfLading(context.getLoad().getGUID(), // GUID;
				context.getLoad().getLoadNo(), // loadNo;
				context.getLoad().getDebtorName(), // debtorName;
				context.getLoad().getDate(), // date;
				UUID.randomUUID().toString(), // BOLID
				context.getLoad().getDocuments()[context.getIndex()].getReceiverName(), // receiverName;
				"", // receiverNameML;
				"", // receiverNameConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipperName(), // shipperName;
				"", // shipperNameML;
				"", // shipperNameConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipTo(), // shipTo;
				"", // shipToML;
				"", // shipToConfidence;
				context.getLoad().getDocuments()[context.getIndex()].getShipFrom(), // shipFrom //shipFrom;
				"", // shipFromML;
				"" // shipFromConfidence;
		));
		return context;
	}

	public Context saveBOLData(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Saving Bill of Lading Data");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new HANAService(context, "https://hana-odata-server.com/", "BOLEntity", "POST"), context).execute();

		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	public Context checkBlurScore(Context context) {
		// Skip this step for PDF
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		int pageIndex = 0;
		String message = "";
		log.info("Log No." + String.valueOf(++context.counter) + ":Get Blur score");
		// Set Hystrix properties
		HystrixCommand.Setter config = getHystrixConfig("BlurScoreDest");
		String blurScoreAPIURL = getblurScoreAPIURL();
		log.info("Log No." + String.valueOf(++context.counter) + ":Target URL:" + blurScoreAPIURL);
		try {
			message += "Blur Page:";
			double thrshold = 2000.0; // Todo: Call method to get threshold from destination
			for (pageIndex = 0; pageIndex < context.getLoad().getDocuments()[context.getIndex()]
					.getPages().length; pageIndex++) {
				context.setPageIndex(pageIndex);
				// log.info(String.valueOf(context.counter++)+":Call Blur Service");
				// Call Blur service
				context = new RemoteCall(config,
						new BlurScoreService(context,
								blurScoreAPIURL),
						context).execute();
				// If image score less than threshold -> consider image blurry
				if (context.getLoad().getDocuments()[context.getIndex()].getPages()[pageIndex]
						.getBlurScore() < thrshold) {
					message = message + String.valueOf(pageIndex) + ".";
					// Construct Status
					// Map status data
					Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
							context.getStatus().getDebtorName(), context.getStatus().getDate(),
							context.getStatus().getDocumentType(), context.getStatus().getFileName(),
							context.getStatus().getPageCount(), statusCodeBlurred, message);

					// Update Context
					context.setStatus(statusdata);
					// Update DB
					context = updateStatus(context);

				}
			}
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			message += "Error during Blur Detection.Page:" + String.valueOf(pageIndex) + ".";
			// Construct Status
			// Map status data
			Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(), context.getStatus().getDate(),
					context.getStatus().getDocumentType(), context.getStatus().getFileName(),
					context.getStatus().getPageCount(), statusCodeConrastFailed, message);

			// Update Context
			context.setStatus(statusdata);
			// Update DB
			context = updateStatus(context);
		}
		return context;
	}

	private String getblurScoreAPIURL() {
		// TODO Auto-generated method stub
		String targetURL = "";
		DestinationProxy dp = new DestinationProxy("BlurScoreDest");
		//return dp.getProperties().toString();
		//JSONObject destConfig = new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8));
		try {
			targetURL =  dp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			log.error("Destination not configured:" + e.getMessage());
			e.printStackTrace();
		}
		return targetURL;
	}

	public Context enhanceContrast(Context context) {
		// Skip this step for PDF
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		int pageIndex = 0;
		String message = "";
		log.info("Log No." + String.valueOf(++context.counter) + ": Contrast Enhancement");
		// Set Hystrix properties
		HystrixCommand.Setter config = getHystrixConfig("default");
		try {
			for (pageIndex = 0; pageIndex < context.getLoad().getDocuments()[context.getIndex()]
					.getPages().length; pageIndex++) {
				context.setPageIndex(pageIndex);
				context = new RemoteCall(config, new ContrastEnhanceService(context,
						"https://contrastenhancementpython-patient-klipspringer.cfapps.us10.hana.ondemand.com/"),
						context).execute();

			}
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			message += "Error during Contrast Enhancement.Page:" + String.valueOf(pageIndex) + ".";
			// Construct Status
			// Map status data
			Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(), context.getStatus().getDate(),
					context.getStatus().getDocumentType(), context.getStatus().getFileName(),
					context.getStatus().getPageCount(), statusCodeConrastFailed, message);

			// Update Context
			context.setStatus(statusdata);
			// Update DB
			context = updateStatus(context);

		}
		return context;
	}

	public Context convertPDFandSaveToObjectStore(Context context) {
		// Skip this step for PDF
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equals("PDF")) {
			return context;
		}
		String message = "";
		log.info("Log No." + String.valueOf(++context.counter) + ": Converting to PDF");
		try {
			// Set Hystrix properties
			HystrixCommand.Setter config = getHystrixConfig("default");
			context = new RemoteCall(config,
					new PDFConvertService(context,
							"https://imagetopdfpython-funny-chimpanzee.cfapps.us10.hana.ondemand.com/"),
					context).execute();

		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			message += "Error during PDF conversion";
			// Construct Status
			// Map status data
			Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(), context.getStatus().getDate(),
					context.getStatus().getDocumentType(), context.getStatus().getFileName(),
					context.getStatus().getPageCount(), statusCodePDFConvertFailed, message);
			// Update Context
			context.setStatus(statusdata);
			// Update DB
			context = updateStatus(context);
		}
		return context;
	}

	public Context checkProcessStatus(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Set Status to Ready for ML pickup");
		// If All the check passed successfully
		// log.info("Log No." + String.valueOf(context.counter) + " StatusData: " +
		// context.getStatus().getStatus());

		if (context.getStatus().getStatus().toString().equals(statusCodeInitiated)) {
			Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
					context.getStatus().getDebtorName(), context.getStatus().getDate(),
					context.getStatus().getDocumentType(), context.getStatus().getFileName(),
					context.getStatus().getPageCount(), statusCodeObjectStoreOpsReady, "Ready for ML Process");
			// log.info("Log No." + String.valueOf(context.counter) + " GUID:" +
			// context.getStatus().getGUID() + "LoadNo:"
			// + context.getStatus().getLoadNo() + "Debtor:" +
			// context.getStatus().getDebtorName() + "Date:"
			// + context.getStatus().getDate() + "DocumentType:" +
			// context.getStatus().getDocumentType()
			// + "File Name:" + context.getStatus().getFileName() + "Page Count:"
			// + context.getStatus().getPageCount());
			// Update Context
			context.setStatus(statusdata);
			// Update DB
			context = updateStatus(context);

		}
		return context;
	}

	public Context stitchPDF(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + " Check if it's ready to stitch PDF files.");
		log.info("Log No." + String.valueOf(context.counter) + " Current Doc Index:"
				+ String.valueOf(context.getIndex()));
		log.info("Log No." + String.valueOf(context.counter) + " Total Doc Index:"
				+ String.valueOf(context.getLoad().getDocuments().length));
		if (context.getIndex() == context.getLoad().getDocuments().length - 1) {
			log.info("Log No." + String.valueOf(context.counter) + " Current Page Index:"
					+ String.valueOf(context.getPageIndex()));
			log.info("Log No." + String.valueOf(context.counter) + " Total Page Index:"
					+ String.valueOf(context.getLoad().getDocuments()[context.getIndex()].getPages().length));
			// Stitch only after processing of last page
			if (context.getPageIndex() == context.getLoad().getDocuments()[context.getIndex()].getPages().length - 1) {
				String message = "";
				log.info("Log No." + String.valueOf(context.counter) + " Stitch PDF");
				try {
					// Set Hystrix properties
					HystrixCommand.Setter config = getHystrixConfig("default");
					context = new RemoteCall(config,
							new PDFStitchingService(context,
									"https://pdfstitchingpython-thankful-bilby.cfapps.us10.hana.ondemand.com/"),
							context).execute();
				} catch (Exception e) {
					log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
					message += "Error during PDF stitching";
					// Construct Status
					// Map status data
					Status statusdata = new Status(context.getStatus().getGUID(), context.getStatus().getLoadNo(),
							context.getStatus().getDebtorName(), context.getStatus().getDate(),
							context.getStatus().getDocumentType(), context.getStatus().getFileName(),
							context.getStatus().getPageCount(), statusCodePDFConvertFailed, message);

					// Update Context
					context.setStatus(statusdata);
					// Update DB
					context = updateStatus(context);

				}

			}
		}
		return context;
	}

}

