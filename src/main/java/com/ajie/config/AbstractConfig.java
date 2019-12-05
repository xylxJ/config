package com.ajie.config;

import java.util.Date;

import com.ajie.config.util.ConfigUtil;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * 抽象配置基类
 * <p>
 * 特别说明，AbstractConfig即其子类的属性只能添加，不能减少，如果有字段不用或调整，请使用@Deprecated注解标记，不能直接删除
 * 否则会导致版本回滚找不到字段而报错
 * 
 * @author niezhenjie
 */

public abstract class AbstractConfig implements Config {
	/** 唯一id （id规则一般为配置的全路径命名+业务对象标志） */
	protected String id;
	/** 版本 ，可通过version-Config.INIT_VERSION计算修改版本的次数 */
	protected String version;
	/** 创建时间 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	protected Date createTime;
	/** 最后修改时间 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	protected Date lastModifyTime;
	/** 修改次数 */
	protected int modifyCount;

	/**
	 * 生成id
	 * 
	 * @param bizId
	 *            业务对象的唯一表示（如：userid,blogid）
	 * @return
	 */
	abstract protected String getBiz();

	public AbstractConfig() {

	}

	public String getId() {
		if (null == id) {
			String biz = getBiz();

			id = ConfigUtil.genId(biz, this.getClass());
		}
		return id;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public Date getCreateTime() {
		return createTime;
	}

	@Override
	public Date getLastModifyTime() {
		return lastModifyTime;
	}

	@Override
	public int getModifyCount() {
		return modifyCount;
	}

	/**
	 * 提供反射使用
	 * 
	 * @param version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setCreateTime(Date date) {
		this.createTime = date;
	}

	public void setLastModifyTime(Date date) {
		this.lastModifyTime = date;
	}

	public void setModifyCount(int count) {
		this.modifyCount = count;
	}

	/**
	 * 版本号+1，只会处理主干版本号，如果传进来的是分支版本，也是取出主干版本后+1
	 * 
	 * @param config
	 * @return
	 */
	static void addVersion(Config config) {
		String version = config.getVersion();
		AbstractConfig conf = (AbstractConfig) config;
		if (null == version) {
			conf.setVersion(Config.INIT_VERSION);
			return;
		}
		if (ConfigUtil.isBrance(version)) {
			// 去除分支版本号，获取最大版本号
			version = ConfigUtil.getTrunkVersionFromBranceVersion(version);
		}
		String v = version.substring(2);
		// 注意.是正则元字符，要转义
		String[] vs = v.split("\\.");
		int v3 = Integer.valueOf(vs[2]);
		int v2 = Integer.valueOf(vs[1]);
		int v1 = Integer.valueOf(vs[0]);
		v3 = v3 + 1;
		if (v3 == 33) { // 小版本号最大32
			v2 += 1;
			v3 = 0;
		}
		if (v2 >= 17) { // 次版本号最大16
			v1 += 1;
			v2 = 0;
		}
		String newVersion = "v_" + v1 + Config.VERSION_SEPARATOR + v2
				+ Config.VERSION_SEPARATOR + v3;
		conf.setVersion(newVersion);
	}

	/**
	 * 获取版本号
	 * 
	 * @param step
	 *            整数表示后版本，负数表示前版本
	 * @return
	 */
	static String getVersion(String version, int step) {
		if (null == version) {
			version = Config.INIT_VERSION;
		}
		if (step < 0 && Config.INIT_VERSION.equals(version)) {
			return Config.INIT_VERSION;
		}
		if (ConfigUtil.isBrance(version)) {
			String v = ConfigUtil.getBranceVersionFrom(version);
			version = "v_" + v;
		}
		String v = version.substring(2);
		// 注意.是正则元字符，要转义
		String[] vs = v.split("\\.");
		int v3 = Integer.valueOf(vs[2]);
		int v2 = Integer.valueOf(vs[1]);
		int v1 = Integer.valueOf(vs[0]);
		if (step < 0) {
			// 减版本号，校验是否减过头了
			// 版本号一共修改的次数，公式解释：主版本号每升1表示需要修改32*16次，次版本号升1需修改32次
			int all = v3 + v2 * 32 + ((v1 - 1) * 16 * 32);
			if (all + step < 0) {
				return Config.INIT_VERSION;
			}
			v3 += step;
			if (v3 < 0) {
				// 实际上v3<0，则v2一定要减1了，（因为去倍数v3的绝对值不一定大于32，不同step为正的情况）
				if (v3 > -32) {
					v2 -= 1;
				} else {
					v2 -= (Math.abs(v3) / 32);
				}
				v3 = 32 - (Math.abs(v3) % 32);
			}
			if (v2 < 0) {
				if (v2 > -16) {
					v1 -= 1;
				} else {
					v1 -= (Math.abs(v2) / 16);
				}
				v2 = 16 - ((Math.abs(v2)) % 16);
			}
			return "v_" + v1 + Config.VERSION_SEPARATOR + v2
					+ Config.VERSION_SEPARATOR + v3;
		}
		v3 += step;
		if (v3 > 32) { // 小版本号最大32
			v2 += (v3 / 32);
			v3 %= 32;
		}
		if (v2 > 16) { // 次版本号最大16
			v1 += (v2 / 16);
			v2 %= 16;
		}
		return "v_" + v1 + Config.VERSION_SEPARATOR + v2
				+ Config.VERSION_SEPARATOR + v3;

	}

	static String genVersionNode(String version) {
		return Config.BRANCE_SEPARATOR + version;
	}

	// 1.0.10 - 15
	public static void main(String[] args) {
		String v = getVersion("v_2.1.32", -19 * 32);
		System.out.println(v.getClass().getPackage().getName());

	}
}
