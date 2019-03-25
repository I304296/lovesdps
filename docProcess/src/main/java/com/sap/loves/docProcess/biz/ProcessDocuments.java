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
import com.sap.loves.docProcess.comm.FileStoreService;
import com.sap.loves.docProcess.comm.PDFConvertService;
import com.sap.loves.docProcess.comm.PDFStitchingService;
import com.sap.loves.docProcess.comm.RateConfirmationHANAService;
import com.sap.loves.docProcess.comm.RemoteCall;
import com.sap.loves.docProcess.pojo.BillOfLading;
import com.sap.loves.docProcess.pojo.Context;
import com.sap.loves.docProcess.pojo.Load;
import com.sap.loves.docProcess.pojo.Message;
import com.sap.loves.docProcess.pojo.RateConfirmation;
import com.sap.loves.docProcess.pojo.Status;

import com.sap.loves.docProcess.utility.DestinationProxy;

import org.json.JSONException;

public class ProcessDocuments {

	final String statusCodeInitiated = "0";
	final String statusCodeBlurred = "1";
	final String statusCodeConrastFailed = "2";
	final String statusCodePDFConvertFailed = "3";
	final String statusCodeObjectStoreOpsFailed = "4";
	final String statusCodeObjectStoreOpsReady = "5";
	final String statusCodeResubmission = "9";
	final String statusCodePDFStitchingFailed = "11";
	ExecutorService cpuBound = Executors.newFixedThreadPool(4);
	ExecutorService ioBound = Executors.newCachedThreadPool();
	private Load load;

	private DestinationProxy hanaDp;
	private DestinationProxy blurDetectionDp;
	private DestinationProxy contrastEnhancementDp;
	private DestinationProxy objectStoreDp;
	private DestinationProxy imageToPdfDp;
	private DestinationProxy pdfStitchingDp;
	private DestinationProxy fileStoreDp;

	private String hanaApi = "";
	private String blurDetectionApi = "";
	private String contrastEnhancementApi = "";
	private String objectStoreApi = "";
	private String imageToPdfApi = "";
	private String pdfStitchingApi = "";
	private String fileStoreApi = "";
	
	private String miscFolder = "";
	private String rcFolder = "";
	private String bolFolder = "";
	private String stitchedPdfFolder = "";

	private HystrixCommand.Setter hanaHystrixConfig;
	private HystrixCommand.Setter blurDetectionHystrixConfig;
	private HystrixCommand.Setter contrastEnhancementHystrixConfig;
	private HystrixCommand.Setter imageToPdfHystrixConfig;
	private HystrixCommand.Setter pdfStitchingHystrixConfig;
	private HystrixCommand.Setter fileStoreHystrixConfig;

	final static Logger log = LoggerFactory.getLogger(ApiController.class);

	public Message doProcess(Load load) throws RuntimeException {
		Message msg = new Message(200, "Payload Recieved: Being Processed");

		setUpDestinations();

		try {
			this.load = load;
			int bolCounter = 0;
			int miscCounter = 0;
			for (int i = 0; i < load.getDocuments().length; i++) {
				// Initialize Context data
				Context ctx = new Context(i);
				ctx.setLoad(load);
				ctx.counter = 0;
				ctx.setExist(false);

				if (load.getDocuments()[i].getDocumentType().equals("MISC")) {
					ctx.setMiscCounter(++miscCounter);
					CompletableFuture.supplyAsync(() -> prepareStatusData(ctx), ioBound)
							.thenApply(contextData -> saveStatusData(contextData))
							.thenAccept(contextData -> saveMISC(contextData));
					continue;
				}

				if (load.getDocuments()[i].getDocumentType().equals("BOL")) {
					ctx.setBolCounter(++bolCounter);
				}
				CompletableFuture.supplyAsync(() -> prepareRCData(ctx), ioBound)
						.thenApply(contextData -> prepareBOLData(contextData))
						.thenApply(contextData -> prepareStatusData(contextData))
						.thenApply(contextData -> saveStatusData(contextData))
						// 2. Save RC
						// .thenApply(contextData -> prepareRCData(contextData))
						.thenApply(contextData -> saveRCData(contextData))
						// 3. Save BOL
						// .thenApply(contextData -> prepareBOLData(contextData))
						.thenApply(contextData -> saveBOLData(contextData))
						// 4. Call Contrast enhancement per page
						.thenApply(contextData -> enhanceContrast(contextData))
						// 5. Call Blur detection per page
						.thenApply(contextData -> checkBlurScore(contextData))
						// 6. Convert Image to PDF and store to Object Store
						.thenApply(contextData -> convertPDFandSaveToObjectStore(contextData))
						// 7. Stitch PDF
						.thenApply(contextData -> stitchPDF(contextData))
						// 8. Finally Update status
						.thenAccept(contextData -> checkProcessStatus(contextData));
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return msg;
	}

	public Context saveMISC(Context context) {
		context = new RemoteCall(fileStoreHystrixConfig,
				new FileStoreService(context, fileStoreApi, objectStoreApi, context.getStatus().getFileName()), context)
						.execute();
		return context;
	}

	// Check if entry exists via ODATA GET
	public boolean loadEntryExists(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Checking Duplicated Status Data");
		try {
			context = new RemoteCall(hanaHystrixConfig, new HANAService(context, hanaApi, "Status", "GET"), context)
					.execute();
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context.isExist();
	}

	// 1.Prepare Load status data
	public Context prepareStatusData(Context context) {
		String statusCode = statusCodeInitiated;
		String statusDescription = "Initial Posting ";

		String documentType = "";
		String fileName = "";
		if (context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("BOL")) {
			documentType = context.getLoad().getDocuments()[context.getIndex()].getDocumentType()
					+ String.valueOf(context.getBolCounter());
		} else {
			documentType = context.getLoad().getDocuments()[context.getIndex()].getDocumentType();
		}

		fileName = (context.getLoad().getDebtorName()).replaceAll(" ", "-") + "-" + context.getLoad().getLoadNo() + "-"
				+ context.getLoad().getDate();

		if (context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("MISC")) {
			// Add file extension for MISC files
			fileName += "-MISC" + String.valueOf(context.getMiscCounter()) + "." + context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().toLowerCase();
			documentType += String.valueOf(context.getMiscCounter());
			fileName = miscFolder + "/" + fileName;
		} else {
			if(context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("BOL")) {
				fileName = bolFolder + "/" + documentType + "-" + fileName + ".pdf";
			}else if(context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("RC")) {
				fileName = rcFolder + "/" + documentType + "-" + fileName + ".pdf";
			}
			List<String> filenames = this.load.getFilenames();
			// log.info("Log No." + String.valueOf(context.counter) + ": current file list "
			// + filenames + " ");

			// add filename into filenames
			filenames.add(fileName);
			this.load.setFilenames(filenames);

			// combined name = debtorName + LoadNo + Date + "-" + debotName ....
			String stitchedPdfName = this.load.getStitchedPdfName();
			stitchedPdfName += documentType + "-";
			this.load.setStitchedPdfName(stitchedPdfName);
		}

		// Map status data
		Status statusdata = new Status(context.getLoad().getGUID(), context.getLoad().getLoadNo(),
				context.getLoad().getDebtorName(), context.getLoad().getDate(), documentType, fileName,
				context.getLoad().getDocuments()[context.getIndex()].getPages().length, statusCode, statusDescription);

		context.setStatus(statusdata);

		log.info("Log No." + String.valueOf(++context.counter) + ": Preparing Status Data for " + fileName);

		// Check for duplicated data
		if (loadEntryExists(context)) {
			context.setExist(true);
		}

		// Check if any entry exists in DB for same Load number, if so this is a
		// Re-submission
		if (context.isExist()) {
			statusCode = statusCodeResubmission;
			statusDescription = "Re-submission ";
			context.getStatus().setStatus(statusCode);
			context.getStatus().setStatusDescription(statusDescription);
		}
		return context;
	}

	// Save Status via OData POST
	public Context saveStatusData(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Saving Status Data");
		if (!context.isExist()) {
			try {
				context = new RemoteCall(hanaHystrixConfig, new HANAService(context, hanaApi, "Status", "POST"),
						context).execute();
			} catch (Exception e) {
				log.error("Log No." + String.valueOf(context.counter) + ": " + e.getMessage());
			}
		} else {
			log.info("Log No." + String.valueOf(context.counter) + ": Staus data already exists, updating Status Data");
			updateStatus(context);
		}
		return context;
	}

	public Context updateContextStatusDescription(Context context, String description) {
		String message = context.getStatus().getStatusDescription();
		message += description;
		context.getStatus().setStatusDescription(message);
		return context;
	}

	public Context updateStatus(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Updating Status Data");
		try {
			context = new RemoteCall(hanaHystrixConfig, new HANAService(context, hanaApi, "Status", "PUT"), context)
					.execute();
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	// 2. Save RC
	public Context prepareRCData(Context context) {
		if (context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("RC")) {
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
			// saveRCData(context);
		}

		return context;
	}

	public Context saveRCData(Context context) {
		if (context.isExist()) {
			log.info("Log No." + String.valueOf(++context.counter)
					+ ": Rate Confirmation Data already exists, skipping saving");
			return context;
		}
		log.info("Log No." + String.valueOf(++context.counter) + ": Saving Rate Confirmation Data");
		try {
			Thread.sleep(1000);
			context = new RemoteCall(hanaHystrixConfig,
					new RateConfirmationHANAService(context, hanaApi + "RateConfirmation", "POST"), context).execute();
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}

		return context;
	}

	public Context prepareBOLData(Context context) {
		if (context.getLoad().getDocuments()[context.getIndex()].getDocumentType().equals("BOL")) {
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
		}
		return context;
	}

	public Context saveBOLData(Context context) {
		if (context.isExist()) {
			log.info("Log No." + String.valueOf(context.counter)
					+ ": Bill of Lading Data already exists, skipping saving");
			return context;
		}
		log.info("Log No." + String.valueOf(context.counter) + ": Saving Bill of Lading Data");
		try {
			Thread.sleep(1000);
			context = new RemoteCall(hanaHystrixConfig, new HANAService(context, hanaApi, "BillOfLading", "POST"),
					context).execute();
			// context = new RemoteCall(hanaHystrixConfig,
			// new BillOfLadingHANAService(context, hanaApi + "BillOfLading", "POST"),
			// context).execute();
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
		}
		return context;
	}

	public Context checkBlurScore(Context context) {
		// Skip this step and send PDF to object store
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equalsIgnoreCase("PDF")) {
			return context;
		}
		int pageIndex = 0;
		log.info("Log No." + String.valueOf(++context.counter) + ": Getting Blur score ");

		try {
			double threshold = 2000.0;
			for (pageIndex = 0; pageIndex < context.getLoad().getDocuments()[context.getIndex()]
					.getPages().length; pageIndex++) {
				context.setPageIndex(pageIndex);
				context = new RemoteCall(blurDetectionHystrixConfig, new BlurScoreService(context, blurDetectionApi),
						context).execute();
				// If image score less than and equal to threshold -> consider image blurry
				if (context.getLoad().getDocuments()[context.getIndex()].getPages()[pageIndex]
						.getBlurScore() < threshold) {
					context = updateContextStatusDescription(context,
							"| Page " + String.valueOf(pageIndex + 1) + " is blurry ");
					context = updateStatus(context);
				}
			}
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			context = updateContextStatusDescription(context, "| Error during blur detection API call ");
			context.getStatus().setStatus("T" + statusCodeBlurred);
			context = updateStatus(context);
		}
		return context;
	}

	public Context enhanceContrast(Context context) {
		// Skip this step for PDF
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equalsIgnoreCase("PDF")) {
			context = new RemoteCall(fileStoreHystrixConfig,
					new FileStoreService(context, fileStoreApi, objectStoreApi, context.getStatus().getFileName()),
					context).execute();
			return context;
		}
		int pageIndex = 0;
		log.info("Log No." + String.valueOf(++context.counter) + ": Enhancing contrast of the images ");

		try {
			for (pageIndex = 0; pageIndex < context.getLoad().getDocuments()[context.getIndex()]
					.getPages().length; pageIndex++) {
				context.setPageIndex(pageIndex);
				context = new RemoteCall(contrastEnhancementHystrixConfig,
						new ContrastEnhanceService(context, contrastEnhancementApi), context).execute();
				context = updateStatus(context);
			}
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			context = updateContextStatusDescription(context, "| Error during contrast enhancement API call at page "
					+ String.valueOf(context.getPageIndex()) + " ");
			context.getStatus().setStatus("T" + statusCodeConrastFailed);
			context = updateStatus(context);
		}
		return context;
	}

	public Context convertPDFandSaveToObjectStore(Context context) {
		// Skip this step for PDF
		if (context.getLoad().getDocuments()[context.getIndex()].getPages()[0].getDocumentFormat().equalsIgnoreCase("PDF")) {
			return context;
		}
		log.info("Log No." + String.valueOf(++context.counter) + ": Converting images ");
		try {
			context = new RemoteCall(imageToPdfHystrixConfig,
					new PDFConvertService(context, imageToPdfApi, objectStoreApi), context).execute();
			context = updateStatus(context);
		} catch (Exception e) {
			log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
			context = updateContextStatusDescription(context, "| Error during pdf conversion API call ");
			context.getStatus().setStatus(statusCodePDFConvertFailed);
			context = updateStatus(context);
		}
		return context;
	}

	public Context stitchPDF(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + " Checking if it's ready to stitch PDF files. ");
		if (context.getIndex() == context.getLoad().getDocuments().length - 1) {
			if (context.getPageIndex() == context.getLoad().getDocuments()[context.getIndex()].getPages().length - 1) {
				log.info("Log No." + String.valueOf(context.counter) + " Stitching all the PDF files");
				try {
					Thread.sleep(30000);
					context = new RemoteCall(pdfStitchingHystrixConfig,
							new PDFStitchingService(context, pdfStitchingApi, objectStoreApi, stitchedPdfFolder), context).execute();
					context = updateStatus(context);
				} catch (Exception e) {
					log.error("Log No." + String.valueOf(context.counter) + " " + e.getMessage());
					context = updateContextStatusDescription(context, "| Error during pdf stitching API call ");
					context.getStatus().setStatus("T" + statusCodePDFStitchingFailed);
					context = updateStatus(context);
				}
				context = updateStatus(context);
			}
		}
		return context;
	}

	public Context checkProcessStatus(Context context) {
		log.info("Log No." + String.valueOf(++context.counter) + ": Checking Status for ML pickup ");
		// If All the check passed successfully
		if (context.getStatus().getStatus().toString().equals(statusCodeInitiated)
				|| context.getStatus().getStatus().toString().equals(statusCodeResubmission)) {
			context = updateContextStatusDescription(context, "| Ready for ML Process ");
			context.getStatus().setStatus(statusCodeObjectStoreOpsReady);
			context = updateStatus(context);
		}
		return context;
	}

	// Method Overload
	public HystrixCommand.Setter getHystrixConfig(DestinationProxy dp) {

		// Initialize with Default Value
		int ExecutionTimeoutInMilliseconds = 180000;
		int CircuitBreakerSleepWindowInMilliseconds = 4000;
		boolean CircuitBreakerEnabled = true;
		int CircuitBreakerRequestVolumeThreshold = 1;
		boolean FallbackEnabled = true;

		// If destination name is supplied then get from destination
		try {
			ExecutionTimeoutInMilliseconds = Integer.parseInt(dp.getProperties()
					.getJSONObject("destinationConfiguration").getString("hystrix.ExecutionTimeoutInMilliseconds"));
			CircuitBreakerSleepWindowInMilliseconds = Integer
					.parseInt(dp.getProperties().getJSONObject("destinationConfiguration")
							.getString("hystrix.CircuitBreakerSleepWindowInMilliseconds"));
			CircuitBreakerRequestVolumeThreshold = Integer
					.parseInt(dp.getProperties().getJSONObject("destinationConfiguration")
							.getString("hystrix.CircuitBreakerRequestVolumeThreshold"));
			CircuitBreakerEnabled = Boolean.parseBoolean(dp.getProperties().getJSONObject("destinationConfiguration")
					.getString("hystrix.CircuitBreakerEnabled"));
			FallbackEnabled = Boolean.parseBoolean(
					dp.getProperties().getJSONObject("destinationConfiguration").getString("hystrix.FallbackEnabled"));

			log.info("ExecutionTimeoutInMilliseconds:" + String.valueOf(ExecutionTimeoutInMilliseconds)
					+ "|CircuitBreakerSleepWindowInMilliseconds:"
					+ String.valueOf(CircuitBreakerSleepWindowInMilliseconds) + "|CircuitBreakerRequestVolumeThreshold:"
					+ String.valueOf(CircuitBreakerRequestVolumeThreshold) + "|CircuitBreakerEnabled:"
					+ String.valueOf(CircuitBreakerEnabled) + "|FallbackEnabled:" + String.valueOf(FallbackEnabled));

		} catch (JSONException e) {
			log.error("Destination not configured:" + e.getMessage());
			e.printStackTrace();
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

	// Method Overload
	// public HystrixCommand.Setter getHystrixConfig(String DestinationName) {
	//
	// // Initialize with Default Value
	// int ExecutionTimeoutInMilliseconds = 180000;
	// int CircuitBreakerSleepWindowInMilliseconds = 4000;
	// boolean CircuitBreakerEnabled = true;
	// int CircuitBreakerRequestVolumeThreshold = 1;
	// boolean FallbackEnabled = true;
	//
	// log.info("Inside getHystrixConfig. DestinationName=" + DestinationName);
	//
	// // If destination name is supplied then get from destination
	// if (DestinationName.length() > 0 || !DestinationName.equals("default")) {
	// DestinationProxy dp = new DestinationProxy(DestinationName);
	// try {
	// ExecutionTimeoutInMilliseconds = Integer.parseInt(dp.getProperties()
	// .getJSONObject("destinationConfiguration").getString("hystrix.ExecutionTimeoutInMilliseconds"));
	// CircuitBreakerSleepWindowInMilliseconds = Integer
	// .parseInt(dp.getProperties().getJSONObject("destinationConfiguration")
	// .getString("hystrix.CircuitBreakerSleepWindowInMilliseconds"));
	// CircuitBreakerRequestVolumeThreshold = Integer
	// .parseInt(dp.getProperties().getJSONObject("destinationConfiguration")
	// .getString("hystrix.CircuitBreakerRequestVolumeThreshold"));
	// CircuitBreakerEnabled = Boolean.parseBoolean(dp.getProperties()
	// .getJSONObject("destinationConfiguration").getString("hystrix.CircuitBreakerEnabled"));
	// FallbackEnabled =
	// Boolean.parseBoolean(dp.getProperties().getJSONObject("destinationConfiguration")
	// .getString("hystrix.FallbackEnabled"));
	//
	// log.info("ExecutionTimeoutInMilliseconds:" +
	// String.valueOf(ExecutionTimeoutInMilliseconds)
	// + "|CircuitBreakerSleepWindowInMilliseconds:"
	// + String.valueOf(CircuitBreakerSleepWindowInMilliseconds)
	// + "|CircuitBreakerRequestVolumeThreshold:"
	// + String.valueOf(CircuitBreakerRequestVolumeThreshold) +
	// "|CircuitBreakerEnabled:"
	// + String.valueOf(CircuitBreakerEnabled) + "|FallbackEnabled:"
	// + String.valueOf(FallbackEnabled));
	//
	// } catch (JSONException e) {
	// log.error("Destination not configured:" + e.getMessage());
	// e.printStackTrace();
	// }
	//
	// }
	// HystrixCommand.Setter config = HystrixCommand.Setter
	// .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceGroupThreadPool"));
	// HystrixCommandProperties.Setter commandProperties =
	// HystrixCommandProperties.Setter();
	// commandProperties.withExecutionTimeoutInMilliseconds(ExecutionTimeoutInMilliseconds);
	// commandProperties.withCircuitBreakerSleepWindowInMilliseconds(CircuitBreakerSleepWindowInMilliseconds);
	// commandProperties.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
	// commandProperties.withCircuitBreakerEnabled(CircuitBreakerEnabled);
	// commandProperties.withCircuitBreakerRequestVolumeThreshold(CircuitBreakerRequestVolumeThreshold);
	// commandProperties.withFallbackEnabled(FallbackEnabled);
	//
	// config.andCommandPropertiesDefaults(commandProperties);
	// config.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withMaxQueueSize(10).withCoreSize(4)
	// .withQueueSizeRejectionThreshold(10));
	// return config;
	// }

	// Set up destinations
	public void setUpDestinations() {
		try {
			hanaDp = new DestinationProxy("HANAServiceDest");
			hanaApi = hanaDp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
			hanaHystrixConfig = getHystrixConfig(hanaDp);
		} catch (JSONException e) {
			log.error("HANA oData Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			blurDetectionDp = new DestinationProxy("BlurScoreDest");
			blurDetectionApi = blurDetectionDp.getProperties().getJSONObject("destinationConfiguration")
					.getString("URL");
			blurDetectionHystrixConfig = getHystrixConfig(blurDetectionDp);
		} catch (JSONException e) {
			log.error("Blur Detection Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			contrastEnhancementDp = new DestinationProxy("ContrastEnhancementDest");
			contrastEnhancementApi = contrastEnhancementDp.getProperties().getJSONObject("destinationConfiguration")
					.getString("URL");
			contrastEnhancementHystrixConfig = getHystrixConfig(contrastEnhancementDp);
		} catch (JSONException e) {
			log.error("Contrast Enhancement Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			objectStoreDp = new DestinationProxy("ObjectStoreDest");
			objectStoreApi = objectStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
		} catch (JSONException e) {
			log.error("Object Store Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			imageToPdfDp = new DestinationProxy("ImageToPdfDest");
			imageToPdfApi = imageToPdfDp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
			imageToPdfHystrixConfig = getHystrixConfig(imageToPdfDp);
		} catch (JSONException e) {
			log.error("Image to PDF Conversion Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			pdfStitchingDp = new DestinationProxy("PdfStitchingDest");
			pdfStitchingApi = pdfStitchingDp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
			pdfStitchingHystrixConfig = getHystrixConfig(pdfStitchingDp);
		} catch (JSONException e) {
			log.error("PDF Stitching Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
		try {
			fileStoreDp = new DestinationProxy("FileStoreDest");
			fileStoreApi = fileStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("URL");
			fileStoreHystrixConfig = getHystrixConfig(fileStoreDp);
			miscFolder = fileStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("misc_folder");
			rcFolder = fileStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("rc_folder");
			bolFolder = fileStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("bol_folder");
			stitchedPdfFolder = fileStoreDp.getProperties().getJSONObject("destinationConfiguration").getString("stitched_pdf_folder");
		} catch (JSONException e) {
			log.error("File Store Service Destination is not configured:" + e.getMessage());
			e.printStackTrace();
		}
	}

}
