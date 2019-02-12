package com.sap.loves.docProcess.comm;

import com.sap.loves.docProcess.pojo.Context;

public class PDFConvertService implements IServer {
    private String url;
    private Context context;
    private boolean allPages;
      
	public PDFConvertService(Context context, String url, boolean allPages) {
		super();
		this.url = url;
		this.context = context;
		this.allPages = allPages;
	}

	@Override
	public Context execute() {
		// TODO Auto-generated method stub
		if(allPages) {
			//Loop through Document[index] get pages and send that to PDF Converter service
			for(int i=0; i<context.getLoad().getDocuments().length; i++) {
				for(int j=0; j<context.getLoad().getDocuments()[i].getPages().length;j++) {
					if(!context.getLoad().getDocuments()[i].getPages()[j].getDocumentFormat().equals("PDF")) {
						//Reconcile all page contents
					}	
				}		
			}			
		}else {
			//Loop through Document[index] get pages and send that to PDF Converter service
			for(int i=0; i<context.getLoad().getDocuments()[context.getIndex()].getPages().length; i++) {
				//Reconcile all page contents
			}
		}
		//Call PDF Converter service via RestTemplate
		return context;
	}

	@Override
	public Context fallBack() {
		// TODO Auto-generated method stub
		//Update Status message in context stating that PDF Converter service is down
		return context;
	}

}
