/*******************************************************************************
 * Copyright (c) 2005, Kobrix Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Borislav Iordanov - initial API and implementation
 *     Murilo Saraiva de Queiroz - initial API and implementation
 ******************************************************************************/
package disko.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

public class GoogleLanguageDetector {
	
	public static final String GOOGLE_LANGUAGE_DETECT_URL = "http://ajax.googleapis.com/ajax/services/language/detect?v=1.0&q=";
	public static final String DEFAULT_REFERRER = "http://miamidade.gov/wps/portal/index.asp";
   
	public static TreeMap<String, String> languages = new TreeMap<String, String>();
	static {
		languages.put("ar","ARABIC");
		languages.put("bg","BULGARIAN");
		languages.put("ca","CATALAN");
		languages.put("zh","CHINESE");
		languages.put("zh-CN","CHINESE_SIMPLIFIED");
		languages.put("zh-TW","CHINESE_TRADITIONAL");
		languages.put("hr","CROATIAN");
		languages.put("cs","CZECH");
		languages.put("da","DANISH");
		languages.put("nl","DUTCH");
		languages.put("en","ENGLISH");
		languages.put("et","ESTONIAN");
		languages.put("tl","FILIPINO");
		languages.put("fi","FINNISH");
		languages.put("fr","FRENCH");
		languages.put("de","GERMAN");
		languages.put("el","GREEK");
		languages.put("iw","HEBREW");
		languages.put("hi","HINDI");
		languages.put("hu","HUNGARIAN");
		languages.put("id","INDONESIAN");
		languages.put("it","ITALIAN");
		languages.put("ja","JAPANESE");
		languages.put("ko","KOREAN");
		languages.put("lv","LATVIAN");
		languages.put("lt","LITHUANIAN");
		languages.put("no","NORWEGIAN");
		languages.put("fa","PERSIAN");
		languages.put("pl","POLISH");
		languages.put("pt-PT","PORTUGUESE");
		languages.put("ro","ROMANIAN");
		languages.put("ru","RUSSIAN");
		languages.put("sr","SERBIAN");
		languages.put("sk","SLOVAK");
		languages.put("sl","SLOVENIAN");
		languages.put("es","SPANISH");
		languages.put("sv","SWEDISH");
		languages.put("th","THAI");
		languages.put("tr","TURKISH");
		languages.put("uk","UKRAINIAN");
		languages.put("vi","VIETNAMESE");
	}		
		
	private static Log log = LogFactory.getLog(GoogleLanguageDetector.class);

	/**
	 * Return true if the given text is in English, false if it's not, and
	 * null if the detection failed or is unreliable. 
	 * 
	 * @param s
	 * @return
	 */
	public static Boolean isEnglish(String s) {
		return "en".equals(detectLanguage(s));
	}

	/**
	 * Returns the language code (en, ja, es, pt, etc.) for the language of the given String,
	 * or null if the detection failed or was considered unreliable. 
	 *  
	 * @param text
	 * @return the language code
	 */
	public static String detectLanguage(String text) {

		String language = null;
		
		try {
			String encoded = URLEncoder.encode(text, "UTF-8");
			URL url = new URL(GOOGLE_LANGUAGE_DETECT_URL + encoded);
            URLConnection connection = DiscoProxySettings.newConnection(url);
            
			connection.addRequestProperty("Referer", DEFAULT_REFERRER);
	
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
	
			JSONObject json = new JSONObject(builder.toString());
			log.debug("Answer for '" + text + "' :" + json);
			
			JSONObject responseData = json.getJSONObject("responseData");
			double confidence = responseData.getDouble("confidence");
			boolean isReliable = responseData.getBoolean("isReliable");
			
			language = responseData.getString("language");
	
			log.debug(
					"Language " + language + 
					"\tConfidence: " + confidence + 
					"\tisReliable:" + isReliable
					);
			
			return (isReliable && language.length()>0) ? language : null;
			
		} catch (Exception e){
			log.error("Error detecting language of '" + text + "'", e);
			return null; 
		}
	}
	
	/**
	 * A simple test / example
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length==0) {
			System.out.println(isEnglish("¿Dónde está el baño?"));
			System.out.println(isEnglish("The book is on the table."));
			if ("jap".equals(detectLanguage("ウィキペディアは現在、百科事典としては動植物の写真や機械の動作などの図解が不足している部分があります。")))
				throw new AssertionError("Error detecting japanese language");
		} else {
			System.out.println(detectLanguage(args[0]));
		}
}


}
