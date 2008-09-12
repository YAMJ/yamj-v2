package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class HTMLTools {

	private static final Map<Character, String> AGGRESSIVE_HTML_ENCODE_MAP = new HashMap<Character, String>();
	private static final Map<Character, String> DEFENSIVE_HTML_ENCODE_MAP = new HashMap<Character, String>();
	private static final Map<String, Character> HTML_DECODE_MAP = new HashMap<String, Character>();

	static {
		// Html encoding mapping according to the HTML 4.0 spec
		// http://www.w3.org/TR/REC-html40/sgml/entities.html

		// Special characters for HTML
		AGGRESSIVE_HTML_ENCODE_MAP.put('\u0026', "&amp;");
		AGGRESSIVE_HTML_ENCODE_MAP.put('\u003C', "&lt;");
		AGGRESSIVE_HTML_ENCODE_MAP.put('\u003E', "&gt;");
		AGGRESSIVE_HTML_ENCODE_MAP.put('\u0022', "&quot;");

		DEFENSIVE_HTML_ENCODE_MAP.put('\u0152', "&OElig;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0153', "&oelig;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0160', "&Scaron;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0161', "&scaron;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0178', "&Yuml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u02C6', "&circ;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u02DC', "&tilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2002', "&ensp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2003', "&emsp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2009', "&thinsp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u200C', "&zwnj;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u200D', "&zwj;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u200E', "&lrm;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u200F', "&rlm;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2013', "&ndash;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2014', "&mdash;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2018', "&lsquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2019', "&rsquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u201A', "&sbquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u201C', "&ldquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u201D', "&rdquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u201E', "&bdquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2020', "&dagger;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2021', "&Dagger;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2030', "&permil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2039', "&lsaquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u203A', "&rsaquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u20AC', "&euro;");

		// Character entity references for ISO 8859-1 characters
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A0', "&nbsp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A1', "&iexcl;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A2', "&cent;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A3', "&pound;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A4', "&curren;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A5', "&yen;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A6', "&brvbar;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A7', "&sect;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A8', "&uml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00A9', "&copy;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AA', "&ordf;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AB', "&laquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AC', "&not;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AD', "&shy;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AE', "&reg;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00AF', "&macr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B0', "&deg;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B1', "&plusmn;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B2', "&sup2;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B3', "&sup3;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B4', "&acute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B5', "&micro;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B6', "&para;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B7', "&middot;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B8', "&cedil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00B9', "&sup1;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BA', "&ordm;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BB', "&raquo;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BC', "&frac14;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BD', "&frac12;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BE', "&frac34;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00BF', "&iquest;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C0', "&Agrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C1', "&Aacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C2', "&Acirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C3', "&Atilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C4', "&Auml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C5', "&Aring;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C6', "&AElig;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C7', "&Ccedil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C8', "&Egrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00C9', "&Eacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CA', "&Ecirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CB', "&Euml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CC', "&Igrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CD', "&Iacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CE', "&Icirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00CF', "&Iuml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D0', "&ETH;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D1', "&Ntilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D2', "&Ograve;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D3', "&Oacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D4', "&Ocirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D5', "&Otilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D6', "&Ouml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D7', "&times;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D8', "&Oslash;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00D9', "&Ugrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DA', "&Uacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DB', "&Ucirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DC', "&Uuml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DD', "&Yacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DE', "&THORN;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00DF', "&szlig;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E0', "&agrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E1', "&aacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E2', "&acirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E3', "&atilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E4', "&auml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E5', "&aring;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E6', "&aelig;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E7', "&ccedil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E8', "&egrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00E9', "&eacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00EA', "&ecirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00EB', "&euml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00EC', "&igrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00ED', "&iacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00EE', "&icirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00EF', "&iuml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F0', "&eth;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F1', "&ntilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F2', "&ograve;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F3', "&oacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F4', "&ocirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F5', "&otilde;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F6', "&ouml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F7', "&divide;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F8', "&oslash;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00F9', "&ugrave;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FA', "&uacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FB', "&ucirc;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FC', "&uuml;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FD', "&yacute;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FE', "&thorn;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u00FF', "&yuml;");

		// Mathematical, Greek and Symbolic characters for HTML
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0192', "&fnof;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0391', "&Alpha;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0392', "&Beta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0393', "&Gamma;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0394', "&Delta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0395', "&Epsilon;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0396', "&Zeta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0397', "&Eta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0398', "&Theta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u0399', "&Iota;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039A', "&Kappa;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039B', "&Lambda;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039C', "&Mu;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039D', "&Nu;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039E', "&Xi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u039F', "&Omicron;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A0', "&Pi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A1', "&Rho;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A3', "&Sigma;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A4', "&Tau;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A5', "&Upsilon;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A6', "&Phi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A7', "&Chi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A8', "&Psi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03A9', "&Omega;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B1', "&alpha;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B2', "&beta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B3', "&gamma;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B4', "&delta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B5', "&epsilon;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B6', "&zeta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B7', "&eta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B8', "&theta;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03B9', "&iota;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BA', "&kappa;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BB', "&lambda;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BC', "&mu;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BD', "&nu;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BE', "&xi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03BF', "&omicron;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C0', "&pi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C1', "&rho;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C2', "&sigmaf;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C3', "&sigma;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C4', "&tau;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C5', "&upsilon;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C6', "&phi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C7', "&chi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C8', "&psi;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03C9', "&omega;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03D1', "&thetasym;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03D2', "&upsih;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u03D6', "&piv;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2022', "&bull;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2026', "&hellip;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2032', "&prime;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2033', "&Prime;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u203E', "&oline;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2044', "&frasl;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2118', "&weierp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2111', "&image;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u211C', "&real;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2122', "&trade;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2135', "&alefsym;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2190', "&larr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2191', "&uarr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2192', "&rarr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2193', "&darr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2194', "&harr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21B5', "&crarr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21D0', "&lArr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21D1', "&uArr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21D2', "&rArr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21D3', "&dArr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u21D4', "&hArr;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2200', "&forall;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2202', "&part;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2203', "&exist;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2205', "&empty;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2207', "&nabla;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2208', "&isin;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2209', "&notin;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u220B', "&ni;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u220F', "&prod;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2211', "&sum;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2212', "&minus;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2217', "&lowast;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u221A', "&radic;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u221D', "&prop;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u221E', "&infin;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2220', "&ang;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2227', "&and;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2228', "&or;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2229', "&cap;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u222A', "&cup;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u222B', "&int;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2234', "&there4;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u223C', "&sim;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2245', "&cong;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2248', "&asymp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2260', "&ne;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2261', "&equiv;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2264', "&le;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2265', "&ge;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2282', "&sub;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2283', "&sup;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2284', "&nsub;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2286', "&sube;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2287', "&supe;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2295', "&oplus;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2297', "&otimes;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u22A5', "&perp;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u22C5', "&sdot;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2308', "&lceil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2309', "&rceil;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u230A', "&lfloor;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u230B', "&rfloor;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2329', "&lang;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u232A', "&rang;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u25CA', "&loz;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2660', "&spades;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2663', "&clubs;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2665', "&hearts;");
		DEFENSIVE_HTML_ENCODE_MAP.put('\u2666', "&diams;");

		Set<Map.Entry<Character, String>> aggresive_entries = AGGRESSIVE_HTML_ENCODE_MAP.entrySet();
		for (Map.Entry<Character, String> entry : aggresive_entries) {
			HTML_DECODE_MAP.put(entry.getValue(), entry.getKey());
		}

		Set<Map.Entry<Character, String>> defensive_entries = DEFENSIVE_HTML_ENCODE_MAP.entrySet();
		for (Map.Entry<Character, String> entry : defensive_entries) {
			HTML_DECODE_MAP.put(entry.getValue(), entry.getKey());
		}
	}

	public static String decodeHtml(String source) {
		if (null == source || 0 == source.length()) {
			return source;
		}

		int current_index = 0;
		int delimiter_start_index = 0;
		int delimiter_end_index = 0;

		StringBuilder result = null;

		while (current_index <= source.length()) {
			delimiter_start_index = source.indexOf('&', current_index);
			if (delimiter_start_index != -1) {
				delimiter_end_index = source.indexOf(';', delimiter_start_index + 1);
				if (delimiter_end_index != -1) {
					// ensure that the string builder is setup correctly
					if (null == result) {
						result = new StringBuilder();
					}

					// add the text that leads up to this match
					if (delimiter_start_index > current_index) {
						result.append(source.substring(current_index, delimiter_start_index));
					}

					// add the decoded entity
					String entity = source.substring(delimiter_start_index, delimiter_end_index + 1);

					current_index = delimiter_end_index + 1;

					// try to decoded numeric entities
					if (entity.charAt(1) == '#') {
						int start = 2;
						int radix = 10;
						// check if the number is hexadecimal
						if (entity.charAt(2) == 'X' || entity.charAt(2) == 'x') {
							start++;
							radix = 16;
						}
						try {
							Character c = new Character((char) Integer.parseInt(entity.substring(start, entity.length() - 1), radix));
							result.append(c);
						}
						// when the number of the entity can't be parsed, add
						// the entity as-is
						catch (NumberFormatException e) {
							result.append(entity);
						}
					} else {
						// try to decode the entity as a literal
						Character decoded = HTML_DECODE_MAP.get(entity);
						if (decoded != null) {
							result.append(decoded);
						}
						// if there was no match, add the entity as-is
						else {
							result.append(entity);
						}
					}
				} else {
					break;
				}
			} else {
				break;
			}
		}

		if (null == result) {
			return source;
		} else if (current_index < source.length()) {
			result.append(source.substring(current_index));
		}

		return result.toString();
	}

	public static String getTextAfterElem(String src, String findStr) {
		return getTextAfterElem(src, findStr, 0);
	}

	public static String getTextAfterElem(String src, String findStr, int skip) {
		return getTextAfterElem(src, findStr, skip, 0);
	}

	/**
	 * Example:
	 * src = "<a id="specialID"><br/><img src="a.gif"/>my text</a>
	 * findStr = "specialID"
	 * result = "my text"
	 *
	 * @param src			 html text
	 * @param findStr	 string to find in src
	 * @param skip			count of found texts to skip
	 * @param fromIndex begin index in src
	 * @return string	 from html text which is plain text without html tags
	 */
	public static String getTextAfterElem(String src, String findStr, int skip, int fromIndex) {
		int beginIndex = src.indexOf(findStr, fromIndex);
		if (beginIndex == -1) {
			return Movie.UNKNOWN;
		}
		StringTokenizer st = new StringTokenizer(src.substring(beginIndex + findStr.length()), "<");
		int i = 0;
		while (st.hasMoreElements()) {
			String elem = st.nextToken().replaceAll("&nbsp;|&#160;", "").trim();
			if (elem.length() != 0 && !elem.endsWith(">") && i++ >= skip) {
				String[] elems = elem.split(">");
				if (elems.length > 1) {
					return HTMLTools.decodeHtml(elems[1].trim());
				} else {
					return HTMLTools.decodeHtml(elems[0].trim());
				}
			}
		}
		return Movie.UNKNOWN;
	}

	public static String extractTag(String src, String findStr) {
		return extractTag(src, findStr, 0);
	}

	public static String extractTag(String src, String findStr, int skip) {
		return extractTag(src, findStr, skip, "><");
	}

	public static String extractTag(String src, String findStr, int skip, String separator) {
		int beginIndex = src.indexOf(findStr);
		StringTokenizer st = new StringTokenizer(src.substring(beginIndex + findStr.length()), separator);
		for (int i = 0; i < skip; i++) {
			st.nextToken();
		}

		String value = HTMLTools.decodeHtml(st.nextToken().trim());
		if (value.indexOf("uiv=\"content-ty") != -1 || value.indexOf("cast") != -1 || value.indexOf("title") != -1
				|| value.indexOf("<") != -1) {
			value = Movie.UNKNOWN;
		}

		return value;
	}

	public static ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd) {
		return extractTags(src, sectionStart, sectionEnd, null, "|");
	}

	public static ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag,
																							String endTag) {
		ArrayList<String> tags = new ArrayList<String>();
		int index = src.indexOf(sectionStart);
		if (index == -1) {
			return tags;
		}
		index += sectionStart.length();
		int endIndex = src.indexOf(sectionEnd, index);
		if (endIndex == -1) {
			return tags;
		}

		String sectionText = src.substring(index, endIndex);
		int lastIndex = sectionText.length();
		index = 0;
		int startLen = 0;
		int endLen = endTag.length();

		if (startTag != null) {
			index = sectionText.indexOf(startTag);
			startLen = startTag.length();
		}

		while (index != -1) {
			index += startLen;
			int close = sectionText.indexOf('>', index);
			if (close != -1) {
				index = close + 1;
			}
			endIndex = sectionText.indexOf(endTag, index);
			if (endIndex == -1) {
				endIndex = lastIndex;
			}
			String text = sectionText.substring(index, endIndex);

			tags.add(HTMLTools.decodeHtml(text.trim()));
			endIndex += endLen;
			if (endIndex > lastIndex) {
				break;
			}
			if (startTag != null) {
				index = sectionText.indexOf(startTag, endIndex);
			} else {
				index = endIndex;
			}
		}
		return tags;
	}

	public static String request(URL url) throws IOException {
		StringWriter content = null;

		try {
			content = new StringWriter();

			BufferedReader in = null;
			try {
				URLConnection cnx = url.openConnection();
				cnx.setRequestProperty("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
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

				in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), charset));

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
}
