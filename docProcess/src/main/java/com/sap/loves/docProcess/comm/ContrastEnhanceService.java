package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class ContrastEnhanceService implements IServer {
    private String url;
    private Context context;
    
    final static Logger log = LoggerFactory.getLogger(ApiController.class);
    
    public ContrastEnhanceService(Context context, String url) {
    	this.context = context;
    	this.url = url;
    }
    
	@Override
	public Context execute() {
		//this.log.info("Doc Index:"+String.valueOf(context.getIndex())+"Page Index:"+String.valueOf(context.getPageIndex()));
		// TODO Auto-generated method stub
		String base64content = context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].getContent();
		//String base64content = context.getLoad().getDocuments()[0].getPages()[0].getContent();
		// Call Contrast service using RestTemplate, get enhanced image and update context
		this.log.info("Content:"+base64content+"Doc Index:"+String.valueOf(context.getIndex())+"Page Index:"+String.valueOf(context.getPageIndex()));
		return context;
	}

	@Override
	public Context fallBack() {
		// TODO Auto-generated method stub
		// Default implementation
		return context;
	}

}
