package com.sap.loves.docProcess.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Page {
	private int pageNo;
	private String documentFormat;
	private String content;
	@JsonIgnore
	private double blurScore;
	@JsonIgnore
	private String henhancedContent;
	
	public Page(int pageNo, String documentFormat, String content) {
		this.pageNo = pageNo;
		this.documentFormat = documentFormat;
		this.content = content;
		this.blurScore = 2500; //Default value to pass blur score
	}
	public int getPageNo() {
		return pageNo;
	}
	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getDocumentFormat() {
		return documentFormat;
	}
	public void setDocumentFormat(String documentFormat) {
		this.documentFormat = documentFormat;
	}
	public double getBlurScore() {
		return blurScore;
	}
	public void setBlurScore(double blurScore) {
		this.blurScore = blurScore;
	}
	public String getHenhancedContent() {
		return henhancedContent;
	}
	public void setHenhancedContent(String henhancedContent) {
		this.henhancedContent = henhancedContent;
	}
	
	
}
