package com.ajie.config.exception;

public class ConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigException() {
		super();
	}

	public ConfigException(String msg) {
		super(msg);
	}

	public ConfigException(String msg, Exception e) {
		super(msg, e);
	}

}
