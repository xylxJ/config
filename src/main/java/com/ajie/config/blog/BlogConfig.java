package com.ajie.config.blog;

import com.ajie.config.AbstractConfig;
import com.ajie.dao.pojo.TbBlog;

public class BlogConfig extends AbstractConfig {

	String name;

	int readNum;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setReadNum(int readnum) {
		this.readNum = readnum;
	}

	public int getReadNum() {
		return readNum;
	}

	public BlogConfig() {

	}

	public BlogConfig(TbBlog blog) {
		super();
	}

	@Override
	protected String getBiz() {
		// TODO Auto-generated method stub
		return null;
	}

}
