package com.amt.push.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

public final class PropertyUtil {

	private static Logger logger = LoggerFactory.getLogger(PropertyUtil.class);

	public static final String DEFAUL = "amt-push";
	
	protected static HashMap<String,Properties> propertiesSets = new HashMap<String, Properties>();

	private PropertyUtil() {
		
	}
	
	protected static void init() {
		init(DEFAUL);
	}
	
	protected static void init(String setName) {

		ResourceBundle rb = ResourceBundle.getBundle(setName);
		Properties properties = new Properties();
		Enumeration<String> eu = rb.getKeys();
		while(eu.hasMoreElements()){
			String key = eu.nextElement().trim();
			String value = rb.getString(key).trim();
			try{
				value = new String(value.getBytes("ISO8859-1"),"UTF-8");
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
			properties.put(key.toUpperCase(), value);
		}
		
		propertiesSets.put(setName, properties);
		
	}
	
	public static String getProperty(String key){
		if(propertiesSets.get(DEFAUL) == null){
			init();
		}
		return propertiesSets.get(DEFAUL).getProperty(key.toUpperCase());
	}
	
	public static Integer getPropertyInt(String key){
		int value = 0;
		try{
			value = Integer.parseInt(getProperty(key));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			System.exit(1);
		}
		return value;
	}
	
	public static Float getPropertyFloat(String key){
		float value = 0;
		try{
			value = Float.parseFloat(getProperty(key));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			System.exit(1);
		}
		return value;
	}
	
	public static String getProperty(String setName, String key){
		if(propertiesSets.get(setName) == null){
			init(setName);
		}
		String value = propertiesSets.get(setName).getProperty(key.toUpperCase());
		if(value == null){
			return "";
		}
		return value;
	}
	
	public static Map<String, Properties> getAllProperties() {
		Set<Entry<String, Properties>> propSet = propertiesSets.entrySet();
		Map<String, Properties> propMap = new HashMap<String, Properties>();
		for (Entry<String, Properties> next : propSet) {
			propMap.put(next.getKey(), next.getValue());
		}
		return propMap;
	}
}
