package com.ajie.config;

import java.util.Date;

/**
 * 配置
 * <p>
 * 1.正常版本号如：v_1.0.10,回退版本号，如前面版本回退5个版本：v_1.0.10_!_1.0.5，回退后v_1.0.10分支被创建
 * <p>
 * 2.回退版本不支持修改，如果修改，则版本号自动恢复成最大版本号，然后+1，不创建分支（因为分支本来就有了）<br>
 * 如v_1.0.10_!_1.0.5修改后保存，版本变成v_1.0.11,不创建分支
 * <p>
 * 3.回退版本继续切换至分支版本，版本后缀改变，不创建分支，如：<br>
 * v_1.0.10_!_1.0.5回退2个版本变成：v_1.0.10_!_1.0.3，不创建分支，如果执行修改操作，回到步骤2<br>
 * 总结：<br>
 * 修改保存总是在最大版本号上+1，分支版前缀永远带着最新的主干版<br>
 * 只有在最新版本上执行操作，才会创建分支（即分支保存的版本不会出现如v_1.0 .10_!_1.0.5版本号）
 * 
 * <p>
 * 1.0.10 +1-> 1.0.11 分支=> 1.0.10<br>
 * 1.0.11 +1-> 1.0.12 =分支=> 1.0.11<br>
 * 1.0.12 back=> 1.0.10 分支=> 1.0.12 -->current v:1.0.12_!_1.0.10<br>
 * 1.0.12_!_1.0.10 +1-> 1.0.13 不创建分支<br>
 * 或：<br>
 * 1.0.12_!_1.0.10 back=> 1.0.11 --> current v : 1.0.12_!_1.0.11 不创建分支<br>
 * 
 * @author niezhenjie
 */
public interface Config {
	/** 初始版本 */
	public static final String INIT_VERSION = "v_1.0.0";
	/** 版本号分隔符 */
	public static final char VERSION_SEPARATOR = '.';
	/** 分支分隔符 ,后面接上版本号 */
	public static final String BRANCE_SEPARATOR = "!V_B_!";
	/** 分支主干版本分隔 */
	public static final String BRANCE_TRUNK_SEPARATOR = "_!_";

	/**
	 * 获取id
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 获取版本
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * 创建时间
	 * 
	 * @return
	 */
	Date getCreateTime();

	/***
	 * 最后修改时间
	 * 
	 * @return
	 */
	Date getLastModifyTime();

	/**
	 * 修改次数
	 * 
	 * @return
	 */
	int getModifyCount();

	/**
	 * 设置版本号
	 * 
	 * @param version
	 * @return
	 */
	// void setVersion(String version);

}
