package am.xmx.service;

import com.gilecode.yagson.YaGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapperService implements IMapperService {

	private static final Logger logger = LoggerFactory.getLogger(MapperService.class);
	private static final YaGson jsonMapper = new YaGson();
	private static final String NOT_AVAILABLE = "N/A";
	private static final long JSON_DISABLE_ON_ERROR_SECONDS = 30;

	private volatile long toJsonDisabledUntilTime = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String safeToJson(Object obj) {
		if (toJsonDisabledUntilTime > 0) {
			if (toJsonDisabledUntilTime > System.currentTimeMillis()) {
				// still disabled
				return NOT_AVAILABLE;
			} else {
				// clear flag for faster further checks. NOTE: data race is possible, but we do not care much
				//   about synchronization here
				toJsonDisabledUntilTime = 0;
			}
		}
		try {
			return jsonMapper.toJson(obj, Object.class);
		} catch (Throwable e) {
			logger.warn("toJson() failed for an instance of {}", obj.getClass(), e);

			toJsonDisabledUntilTime = System.currentTimeMillis() + 1000 * JSON_DISABLE_ON_ERROR_SECONDS;
			return NOT_AVAILABLE;
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
