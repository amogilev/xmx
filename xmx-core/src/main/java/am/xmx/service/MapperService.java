package am.xmx.service;

import com.gilecode.yagson.YaGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapperService implements IMapperService {

	private final static Logger logger = LoggerFactory.getLogger(MapperService.class);
	private static final YaGson jsonMapper = new YaGson();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String safeToJson(Object obj) {
		try {
			return jsonMapper.toJson(obj, Object.class);
		} catch (Throwable e) {
			logger.warn("toJson() failed for an instance of {}", obj.getClass(), e);
			return "";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String safeToString(Object obj) {
		try {
			return obj == null ? "null" : obj.toString();
		} catch (Throwable e) {
			return "N/A: " + e.toString();
		}
	}
}
