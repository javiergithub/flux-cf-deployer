package org.cloudfoundry.client.lib.oauth2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JsonUtil {
	
	private final static ObjectMapper mapper = new ObjectMapper();


	public static Map<String, Object> convertJsonToMap(String json) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		if (json != null) {
			try {
				retMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return retMap;
	}

}
