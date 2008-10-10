package com.moviejukebox.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web browser with simple cookies support
 */
public class WebBrowser {
	private Map<String, String> browserProperties;
	private Map<String, Map<String, String>> cookies;

	public WebBrowser() {
		browserProperties = new HashMap<String, String>();
		browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
		cookies = new HashMap<String, Map<String, String>>();
	}

	public String request(String url) throws IOException {
		return request(new URL(url));
	}

	public String request(URL url) throws IOException {
		StringWriter content = null;

		try {
			content = new StringWriter();

			BufferedReader in = null;
			try {
				URLConnection cnx = url.openConnection();
				sendHeader(cnx);
				readHeader(cnx);

				in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), getCharset(cnx)));
				String line;
				while ((line = in.readLine()) != null) {
					content.write(line);
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
			return content.toString();
		} finally {
			if (content != null) {
				content.close();
			}
		}
	}

	private void sendHeader(URLConnection cnx) {
		// send browser properties
		for (Map.Entry<String, String> browserProperty : browserProperties.entrySet()) {
			cnx.setRequestProperty(browserProperty.getKey(), browserProperty.getValue());
		}
		// send cookies
		String cookieHeader = createCookieHeader(cnx);
		if (!cookieHeader.isEmpty()) {
			cnx.setRequestProperty("Cookie", cookieHeader);
		}
	}

	private String createCookieHeader(URLConnection cnx) {
		String host = cnx.getURL().getHost();
		StringBuffer cookiesHeader = new StringBuffer();
		for (Map.Entry<String, Map<String, String>> domainCookies : cookies.entrySet()) {
			if (host.endsWith(domainCookies.getKey())) {
				for (Iterator<Map.Entry<String, String>> it = domainCookies.getValue().entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, String> cookie = it.next();
					cookiesHeader.append(cookie.getKey());
					cookiesHeader.append("=");
					cookiesHeader.append(cookie.getValue());
					if (it.hasNext()) {
						cookiesHeader.append(";");
					}
				}
			}
		}
		return cookiesHeader.toString();
	}

	private void readHeader(URLConnection cnx) {
		// read new cookies and update our cookies
		for (Map.Entry<String, List<String>> header : cnx.getHeaderFields().entrySet()) {
			if ("Set-Cookie".equals(header.getKey())) {
				for (String cookieHeader : header.getValue()) {
					String[] cookieElements = cookieHeader.split(" *; *");
					if (cookieElements.length >= 1) {
						String[] firstElem = cookieElements[0].split(" *= *");
						String cookieName = firstElem[0];
						String cookieValue = firstElem.length > 1 ? firstElem[1] : null;
						String cookieDomain = null;
						// find cookie domain
						for (int i = 1; i < cookieElements.length; i++) {
							String[] cookieElement = cookieElements[i].split(" *= *");
							if ("domain".equals(cookieElement[0])) {
								cookieDomain = cookieElement.length > 1 ? cookieElement[1] : null;
								break;
							}
						}
						if (cookieDomain == null) {
							// if domain isn't set take current host
							cookieDomain = cnx.getURL().getHost();
						}
						Map<String, String> domainCookies = cookies.get(cookieDomain);
						if (domainCookies == null) {
							domainCookies = new HashMap<String, String>();
							cookies.put(cookieDomain, domainCookies);
						}
						// add or replace cookie
						domainCookies.put(cookieName, cookieValue);
					}
				}
			}
		}
	}

	private Charset getCharset(URLConnection cnx) {
		Charset charset = null;
		// content type will be string like "text/html; charset=UTF-8" or "text/html"
		String contentType = cnx.getContentType();
		if (contentType != null) {
			Matcher m = Pattern.compile("charset *=[ '\"]*([^ '\"]+)[ '\"]*").matcher(contentType);
			if (m.find()) {
				String encoding = m.group(1);
				try {
					charset = Charset.forName(encoding);
				} catch (UnsupportedCharsetException e) {
					// there will be used default charset
				}
			}
		}
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		return charset;
	}
}
