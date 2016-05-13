package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.Serializable;

class TradedPoint implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6798454124119160136L;
	private String sender;
	private String receiver;
	private Integer amount;
	
	TradedPoint(String s,String r,Integer a) {
		this.sender = s;
		this.receiver = r;
		this.amount = a;
	}

	String getSender() {
		return sender;
	}

	void setSender(String sender) {
		this.sender = sender;
	}

	String getReceiver() {
		return receiver;
	}

	void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	Integer getAmount() {
		return amount;
	}

	void setAmount(Integer amount) {
		this.amount = amount;
	}
}
