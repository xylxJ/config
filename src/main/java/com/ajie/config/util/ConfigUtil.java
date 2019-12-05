package com.ajie.config.util;

import com.ajie.chilli.utils.Toolkits;
import com.ajie.chilli.utils.common.StringUtils;
import com.ajie.config.Config;

public class ConfigUtil {
	/**
	 * 生成id,id规则一般为配置的全路径命名+业务对象标志
	 * 
	 * @param biz
	 *            业务对象标志
	 * @param clazz
	 * @return
	 */
	public static <T> String genId(String biz, Class<T> clazz) {
		if (null == biz) {
			return biz;
		}
		String name = clazz.getPackage().getName() + Config.VERSION_SEPARATOR
				+ clazz.getSimpleName() + biz;
		return Toolkits.toHex64(Toolkits.hashInt64(name, 0));
	}

	/**
	 * 是否为分支版本
	 * 
	 * @param version
	 * @return true为分支
	 */
	public static boolean isBrance(String version) {
		if (StringUtils.isEmpty(version)) {
			return false;
		}
		return version.indexOf(Config.BRANCE_TRUNK_SEPARATOR) > -1;
	}

	/**
	 * 从分支版本号（large_!_brance）中获取创建分支时的主干版本(large)
	 * 
	 * @param branceVersion
	 * @return
	 */
	public static String getTrunkVersionFromBranceVersion(String branceVersion) {
		if (null == branceVersion) {
			return null;
		}
		int idx = 0;
		if ((idx = branceVersion.indexOf(Config.BRANCE_TRUNK_SEPARATOR)) == -1) {
			return branceVersion;// 当前版本是主干版
		}
		return branceVersion.substring(0, idx);// 去除分支版本号
	}

	/**
	 * 获取从分支版本号（large_!_brance）中获取分支的版本（brance)
	 * 
	 * @param branceVersion
	 * @return
	 */
	public static String getBranceVersionFrom(String branceVersion) {
		if (null == branceVersion) {
			return null;
		}
		if (!isBrance(branceVersion)) {
			return branceVersion;
		}
		return branceVersion.substring(branceVersion
				.indexOf(Config.BRANCE_TRUNK_SEPARATOR));// 去除分支版本号
	}

	public static void main(String[] args) {
		String id = genId("1", String.class);
		System.out.println(id);
	}
}
