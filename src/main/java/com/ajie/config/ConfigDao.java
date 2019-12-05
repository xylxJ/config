package com.ajie.config;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ajie.chilli.thread.ThreadPool;
import com.ajie.chilli.utils.TimeUtil;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.config.blog.BlogConfig;
import com.ajie.config.exception.ConfigException;
import com.ajie.config.util.ConfigUtil;
import com.ajie.dao.mapper.TbBranceConfigMapper;
import com.ajie.dao.mapper.TbConfigMapper;
import com.ajie.dao.pojo.TbBranceConfig;
import com.ajie.dao.pojo.TbConfig;
import com.ajie.dao.pojo.TbUser;
import com.alibaba.fastjson.JSONObject;

/**
 * 与数据库交互的一层
 * 
 * @author niezhenjie
 */
@Service(value = "configDao")
public class ConfigDao {
	private static final Logger logger = LoggerFactory
			.getLogger(ConfigDao.class);

	@Resource
	private TbConfigMapper configMapper;
	@Resource
	private TbBranceConfigMapper branceConfigMapper;
	/** 线程池 */
	@Resource
	private ThreadPool threadPool;

	public void setConfigMapper(TbConfigMapper mapper) {
		configMapper = mapper;
	}

	public void setTbBranceConfigMapper(TbBranceConfigMapper mapper) {
		branceConfigMapper = mapper;
	}

	public void save(Config config, TbUser operator, String note) {
		save(config, operator, note, false);
	}

	/**
	 * 保存配置并创建分支
	 * 
	 * @param config
	 *            保存的配置
	 * @param operator
	 *            操作者
	 * @param note
	 *            理由
	 * @param createBrance
	 *            是否创建分支 true创建
	 */
	private void save(Config config, TbUser operator, String note,
			boolean isRollback) {
		AbstractConfig abs = (AbstractConfig) config;
		if (!ConfigUtil.isBrance(config.getVersion())) {
			// 将分支的恢复到主干，修改次数直接使用分支的，否则+1
			abs.modifyCount = config.getModifyCount() + 1;
		}
		// 当前数据库配置
		TbConfig currentConfig = configMapper.getConfig(config.getId());
		if (null == currentConfig) {
			// 首次创建
			abs.createTime = new Date();
			abs.lastModifyTime = new Date();
			doSave(config, operator, note, true);
			return;
		}
		String version = currentConfig.getVersion();
		if (!isRollback) { // 处理版本号，非回滚操作版本号要+1
			AbstractConfig.addVersion(config);
		}
		if (ConfigUtil.isBrance(version)) {
			// 当前配置是分支版，不用执行备份（因为备份数据库已经存在这条记录）
			abs.lastModifyTime = new Date();
			doSave(config, operator, note, false);
			return;
		}
		// 当前数据库是非回滚的版本配置，需执行备份操作
		createBranceConfig(currentConfig, version);
		abs.lastModifyTime = new Date();
		doSave(config, operator, note, false);
		/*
		 * TbConfig old = configMapper.getConfig(config.getId()); AbstractConfig
		 * abstractConfig = (AbstractConfig) config; if (null ==
		 * config.getCreateTime()) { abstractConfig.createTime = new Date(); }
		 * abstractConfig.lastModifyTime = new Date(); String oldVersion =
		 * config.getVersion(); // 增加版本号 if (!isRollBack) { //
		 * 回滚不需要改变版本号，因为在回滚那里已经处理了 AbstractConfig.addVersion(config); } TbConfig
		 * conf = new TbConfig();// 要保存到主干的 conf.setId(config.getId());
		 * conf.setCreateTime(config.getCreateTime());
		 * conf.setLastModifyTime(config.getLastModifyTime());
		 * conf.setModifyCount(config.getModifyCount() + 1);
		 * conf.setVersion(config.getVersion());
		 * 
		 * if (null != operator) { note = operator.getId() + note; }
		 * conf.setNote(note); if (config.getModifyCount() != 0) { //
		 * 已经修改过了，没有版本号？不可能吧？但是以防万一，还是加一下判断吧 if (null == oldVersion) {
		 * oldVersion = Config.INIT_VERSION; } createBranceConfig(old,
		 * oldVersion);// 创建分支 // 修改次数+1 abstractConfig.modifyCount =
		 * config.getModifyCount() + 1;
		 * conf.setConfig(JsonUtils.toJSONString(config));// 将对象转换成json串
		 * configMapper.update(conf); } else { // 修改次数+1
		 * abstractConfig.modifyCount = config.getModifyCount() + 1;
		 * conf.setConfig(JsonUtils.toJSONString(config));// 将对象转换成json串
		 * configMapper.create(conf); }
		 */
	}

	private void doSave(Config config, TbUser operator, String note,
			boolean isCreate) {
		TbConfig conf = new TbConfig();// 要保存到主干的
		conf.setId(config.getId());
		conf.setCreateTime(config.getCreateTime());
		conf.setLastModifyTime(config.getLastModifyTime());
		conf.setModifyCount(config.getModifyCount());
		String version = config.getVersion();
		if (null == version) {
			version = Config.INIT_VERSION;
		}
		conf.setVersion(version);
		if (null != operator) {
			note = operator.getId() + note;
		}
		conf.setConfig(JsonUtils.toJSONString(config));// 将对象转换成json串
		conf.setNote(note);
		if (isCreate) {
			configMapper.create(conf);
		} else {
			configMapper.update(conf);
		}
	}

	/**
	 * 创建分支，异步
	 * 
	 * @param config
	 */
	private void createBranceConfig(final TbConfig tbConfig,
			final String version) {
		if (null == tbConfig) {
			return;
		}
		threadPool.execute(new Runnable() {
			TbBranceConfigMapper mapper = ConfigDao.this.branceConfigMapper;

			@Override
			public void run() {
				try {
					String id = tbConfig.getId();
					TbBranceConfig conf = mapper.getBranceConfig(id);
					if (null == conf) {
						conf = new TbBranceConfig();
						conf.setId(id);
						conf.setBranceConfigs(AbstractConfig
								.genVersionNode(version)
								+ tbConfig.getConfig()
								+ AbstractConfig.genVersionNode(version));
						mapper.create(conf);
					} else {
						String branceConfigs = conf.getBranceConfigs();
						branceConfigs += (AbstractConfig
								.genVersionNode(version) + tbConfig.getConfig() + AbstractConfig
								.genVersionNode(version));
						conf.setBranceConfigs(branceConfigs);
						mapper.update(conf);
					}
				} catch (Exception e) {
					logger.error("执行分支操作失败", e);
					throw e;
				}

			}
		}, 100);
	}

	/**
	 * 回滚配置至指定版本
	 * <p>
	 * 回滚操作效率较低，从数据库获取所有的分支版本再找出需要回滚的版本的json，
	 * 遍历json获取所有的key，在取出值，利用反射在clazz中找到相应的属性
	 * ，如果属性找不到，则回滚失败（所以要求配置的废弃属性使用@Deprecated处理，而不是删除）
	 * 
	 * @param id
	 * @param version
	 *            回滚值版本
	 * @param clazz
	 * @throws ConfigException
	 */

	public <T> T rollBack(String id, String version, TbUser operator,
			Class<T> clazz) throws ConfigException {
		// 获取所有的分支
		final TbBranceConfig conf = branceConfigMapper.getBranceConfig(id);
		if (null == conf) {
			throw new ConfigException("回滚失败，找不到分支配置，id:" + id);
		}
		final TbConfig trunk = configMapper.getConfig(id);
		String trunkVersion = trunk.getVersion();// 当前主干版本
		if (trunkVersion.equals(version)) {
			throw new ConfigException("不能回退至当前版本，期望回退版本version:" + version
					+ "，当前版本：" + trunkVersion);
		}
		String configStr = conf.getBranceConfigs();
		String node = AbstractConfig.genVersionNode(version);
		int idxs = configStr.indexOf(node);
		int idxe = configStr.lastIndexOf(node);
		if (idxs == -1 || idxe == -1 || idxs == idxe) {
			throw new ConfigException("回滚失败，找不到指定版本的配置，version:" + version);
		}
		configStr = configStr.substring(idxs + node.length(), idxe);
		try {
			JSONObject json = JsonUtils.toBean(configStr, JSONObject.class);
			Iterator<String> it = json.keySet().iterator();
			T newInstance = clazz.newInstance();
			// 获取clazz所有的属性（包括父类的）
			Map<String, Field> fields = new HashMap<String, Field>();
			Class<?> cla = clazz;
			while (null != cla) {
				for (Field f : cla.getDeclaredFields()) {
					if (null != fields.get(f.getName())) {
						continue;
					}
					fields.put(f.getName(), f);
				}
				cla = cla.getSuperclass();
			}

			while (it.hasNext()) {
				String key = it.next();
				Field field = fields.get(key);
				if (null == field) {
					throw new ConfigException("回滚失败，找不到属性：" + key);
				}
				field.setAccessible(true);
				// 日期类型
				Object val = json.get(key);
				if ("class java.util.Date".equals(field.getGenericType()
						.toString())) {
					String sdate = (String) val;
					Date date = TimeUtil.parse(sdate);
					field.set(newInstance, date);
					continue;
				}
				field.set(newInstance, val);
			}
			// 从分支版本中组织好的新配置
			AbstractConfig config;
			if (newInstance instanceof AbstractConfig) {
				config = (AbstractConfig) newInstance;
			} else {
				throw new ConfigException("回滚失败，对象不是AbstractConfig实现类");
			}
			// 组织成分支版本号large_!_brance
			config.version = ConfigUtil
					.getTrunkVersionFromBranceVersion(trunkVersion)
					+ Config.BRANCE_TRUNK_SEPARATOR + config.getVersion();
			String note;
			if (null != operator) {
				note = operator.getId() + "回滚版本至" + version;
			} else {
				note = "System回滚版本至" + version;
			}
			save(config, operator, note, true);
			return newInstance;
		} catch (Exception e) {
			throw new ConfigException("回滚失败:" + configStr, e);
		}
	}

	/**
	 * 回滚指定个版本，
	 * 
	 * @param id
	 * @param step
	 *            负数表示前abs(step)个版本，整数表示后step个版本
	 * @param operator
	 * @param clazz
	 * @throws ConfigException
	 */
	public <T> T rollBack(String id, int step, TbUser operator, Class<T> clazz)
			throws ConfigException {
		TbConfig conf = configMapper.getConfig(id);
		if (null == conf) {
			throw new ConfigException("回滚失败，找不到配置，id:" + id);
		}
		String version = AbstractConfig.getVersion(conf.getVersion(), step);
		return rollBack(id, version, operator, clazz);
	}

	public <T> T openConfig(String id, Class<T> clazz) throws ConfigException {
		T config = getConfig(id, clazz);
		if (null != config) {
			return config;
		}
		try {
			T ins = clazz.newInstance();
			if (ins instanceof AbstractConfig) {
				AbstractConfig abs = (AbstractConfig) ins;
				abs.id = id;
				abs.version = Config.INIT_VERSION;
			}
			return ins;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ConfigException("无法创建配置", e);
		}
	}

	/**
	 * 通过id获取配置
	 * 
	 * @param id
	 * @param clazz
	 * @return
	 */
	public <T> T getConfig(String id, Class<T> clazz) {
		TbConfig conf = configMapper.getConfig(id);
		if (null == conf) {
			return null;
		}
		String config = conf.getConfig();
		return JsonUtils.toBean(config, clazz);
	}

	public static void main(String[] args) throws Exception {
		Config config = new BlogConfig();
		if (config instanceof BlogConfig) {
			BlogConfig conf = (BlogConfig) config;
			conf.setName("jhaa");
			conf.setReadNum(20);
			System.out.println(config.getVersion());
		}
	}
}
