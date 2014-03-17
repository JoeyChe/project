package webtools;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.contrib.ssl.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.*;
import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpManager
{
    public static final String DEFAULT_USER_AGENT = 
        "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1";

    // JSON data format
    public static final String K_URL = "url";
    public static final String K_METHOD = "method";
    public static final String K_M_POST = "POST";
    public static final String K_M_GET = "GET";
    public static final String K_CHARSET = "charset";
    public static final String K_HEADERS = "headers";
    public static final String K_CREDENTIALS = "credentials";
    public static final String K_USER = "user";
    public static final String K_PASSWD = "passwd";
    public static final String K_DATA = "data";
    public static final String K_FILE = "file";
    public static final String K_TIMEOUT = "timeout";
    public static final String K_CONTENT_TYPE = "contenttype";
    public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";

    /**
     *    {
     *       "url"         : "http://www.example.com",
     *       "method"      : "POST",
     *       "charset"     : "utf-8",
     *       "headers"     : {},
     *       "credentials" : {
     *              "user"   : "admin",
     *              "passwd" : "xxx"
     *       },
     *       "contenttype" : "",
     *       "data"        : ["abc", "bcd","fff"] or {}
     *    }
     */

    static {
        // registers default handling for https 
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    }

    private HttpClient client;
    private JSONObject request;


    /**
     * Constructor.
     */
    public HttpManager() {
        client = new HttpClient();
        
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setBooleanParameter("http.protocol.allow-circular-redirects", true);
        client.setParams(clientParams);
    }

    public void setCookiePolicy(String cookiePolicy) {
        if ( "browser".equalsIgnoreCase(cookiePolicy) ) {
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        } else if ( "ignore".equalsIgnoreCase(cookiePolicy) ) {
            client.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        } else if ( "netscape".equalsIgnoreCase(cookiePolicy) ) {
            client.getParams().setCookiePolicy(CookiePolicy.NETSCAPE);
        } else if ( "rfc_2109".equalsIgnoreCase(cookiePolicy) ) {
            client.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        } else {
            client.getParams().setCookiePolicy(CookiePolicy.DEFAULT);
        }
    }

    /**
     * Defines HTTP proxy for the client with specified host and port
     * @param hostName
     * @param hostPort
     */
    public void setHttpProxy(String hostName, int hostPort) {
        client.getHostConfiguration().setProxyHost(new ProxyHost(hostName, hostPort));
    }

    /**
     * Defines HTTP proxy for the client with specified host
     * @param hostName
     */
    public void setHttpProxy(String hostName) {
    	client.getHostConfiguration().setProxyHost(new ProxyHost(hostName));
    }


    /**
     * Defines user credintials for the HTTP proxy server
     * @param username
     * @param password
     */
    public void setHttpProxyCredentials(String username, String password, String host, String domain) {
        Credentials credentials =
                ( host == null || domain == null || "".equals(host.trim()) || "".equals(domain.trim()) ) ?
                    new UsernamePasswordCredentials(username, password) :
                    new NTCredentials(username, password, host, domain);
        client.getState().setProxyCredentials( AuthScope.ANY, credentials);
    }

    public String execute(String jsonReq) throws Exception
    { 
        JSONObject obj = new JSONObject(jsonReq);

        String     url        = obj.getString(K_URL);
        String     methodType = obj.getString(K_METHOD);
        String     charset    = obj.getString(K_CHARSET);

        if ( !url.startsWith("http://") && !url.startsWith("https://") ) {
            url = "http://" + url;
        }

        url = encodeUrl(url, charset);

        this.request = obj;
        // Setup credentials
        if (obj.has(K_CREDENTIALS)) {
            JSONObject cred = obj.getJSONObject(K_CREDENTIALS);
            String username = cred.getString(K_USER);
            String password = cred.getString(K_PASSWD);

        	try {
				URL urlObj = new URL(url);
	            client.getState().setCredentials(
                    new AuthScope(urlObj.getHost(), urlObj.getPort()),
                    new UsernamePasswordCredentials(username, password)
                );
        	} catch (MalformedURLException e) {
				e.printStackTrace();
			}
        }

        // Setup HTTP method
        HttpMethodBase method;
        if ( "post".equalsIgnoreCase(methodType) ) {
            method = createPostMethod(url, charset);
        } else {
            method = createGetMethod(url, charset);
        }

        if (request.has(K_TIMEOUT)) {
            Object data = request.get(K_TIMEOUT);

            if (data instanceof Integer) {
                method.getParams().setParameter("http.socket.timeout", (Integer)data);
            } else {
                throw new Exception("http.sochet.timeout is no an integer.");
            }
        }

        boolean isUserAgentSpecified = false;

        // define request headers, if any exist
        if (obj.has(K_HEADERS)) {
            JSONObject headers = obj.getJSONObject(K_HEADERS);
            Iterator it = headers.keys();
            while (it.hasNext()) {
                String headerName =  (String) it.next();
                if ("User-Agent".equalsIgnoreCase(headerName)) {
                    isUserAgentSpecified = true;
                }
                String headerValue = headers.getString(headerName);
                method.addRequestHeader(new Header(headerName, headerValue));
            }
        }

        if (!isUserAgentSpecified) {
            identifyAsDefaultBrowser(method);
        }

        try {
            int statusCode = client.executeMethod(method);
            
            // if there is redirection, try to download redirection page
            if ((statusCode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (statusCode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (statusCode == HttpStatus.SC_SEE_OTHER) ||
                (statusCode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                Header header = method.getResponseHeader("location");
                if (header != null) {
                    String newURI = header.getValue();
                    if ( !isEmptyString(newURI) ) {
                        method.releaseConnection();
                        method = new GetMethod(fullUrl(url, newURI));
                        identifyAsDefaultBrowser(method);
                        client.executeMethod(method);
                    }
                }
            }

            return method.getResponseBodyAsString();
        } catch (IOException e) {
//            throw new org.webharvest.exception.HttpException("IO error during HTTP execution for URL: " + url, e);
            throw e;
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Defines "User-Agent" HTTP header.
     * @param method
     */
    private void identifyAsDefaultBrowser(HttpMethodBase method) {
        method.addRequestHeader(new Header("User-Agent", DEFAULT_USER_AGENT));
    }

    private HttpMethodBase createPostMethod(String url, String charset) throws Exception
    {
        PostMethod method = new PostMethod(url);

        if (null == request) {
            return method;
        }

        if (request.has(K_FILE)) {
            Object data = request.get(K_FILE);

            if (!(data instanceof String)) {
                return method;
            }

            String contentType = "application/octet-stream";
            if (request.has(K_CONTENT_TYPE)) {
                contentType = request.getString(K_CONTENT_TYPE);
            }

            FileRequestEntity content = new FileRequestEntity(new File((String)data), contentType);
            method.setRequestEntity(content);
            return method;
        }

        if (!request.has(K_DATA)) {
            return method;
        }

        Object data = request.get(K_DATA);

        if (data instanceof JSONArray) {
            JSONArray dataset = (JSONArray)data;
            for (int i = 0; i < dataset.length(); i++) {
                // TODO:
            }
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject)data;
            Iterator it = obj.keys();
            while (it.hasNext()) {
                String name =  (String) it.next();
                Object value = obj.get(name);
                if (value instanceof String) {
                    method.addParameter(name, (String)value);
                } else if (value instanceof JSONArray) {
                    JSONArray valset = (JSONArray)value;
                    for (int i = 0; i < valset.length(); i++) {
                        method.addParameter(name, valset.getString(i));
                    }
                } else {
                    // Empty to do
                }
            }
        } else if (data instanceof String) {
            String contentType = DEFAULT_CONTENT_TYPE;
            if (request.has(K_CONTENT_TYPE)) {
                contentType = request.getString(K_CONTENT_TYPE);
            }

            StringRequestEntity content = new StringRequestEntity((String)data, contentType, charset);
            method.setRequestEntity(content);
        } else {
            // Empty to do
        }

        return method;
    }

    private GetMethod createGetMethod(String url, String charset) throws Exception
    {
        Object data = request.opt(K_DATA);
        String urlParams = "";

        if (null == request || !request.has(K_DATA)) {
            // Empty
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject)data;
            Iterator it = obj.keys();
            while (it.hasNext()) {
                String name =  (String) it.next();
                Object value = obj.get(name);
                if (value instanceof String) {
                    urlParams += name + "=" + URLEncoder.encode((String)value, charset) + "&";
                } else if (value instanceof JSONArray) {
                    JSONArray valset = (JSONArray)value;
                    for (int i = 0; i < valset.length(); i++) {
                        urlParams += name + "=" + URLEncoder.encode(valset.getString(i), charset) + "&";
                    }
                } else {
                    // Empty to do
                }
            }

            if (!"".equals(urlParams)) {
                if (url.indexOf("?") < 0) {
                    url += "?" + urlParams;
                } else if (url.endsWith("&")) {
                    url += urlParams;
                } else {
                    url += "&" + urlParams;
                }
            }
        }


        return new GetMethod(url);
    }

    public HttpClient getHttpClient() {
        return client;
    }


    public static String encodeUrl(String url, String charset) {
        if (url == null) {
            return "";
        }
        
        int index = url.indexOf("?");
        if (index >= 0) {
            try {
                String result = url.substring(0, index+1);
                String paramsPart = url.substring(index+1);
                StringTokenizer tokenizer = new StringTokenizer(paramsPart, "&");
                while (tokenizer.hasMoreTokens()) {
                    String definition = tokenizer.nextToken();
                    int eqIndex = definition.indexOf("=");
                    if (eqIndex >= 0) {
                        String paramName = definition.substring(0, eqIndex);
                        String paramValue = definition.substring(eqIndex + 1);
                        result += paramName + "=" + encodeUrlParam(paramValue, charset) + "&";
                    } else {
                        result += encodeUrlParam(definition, charset) + "&";
                    }
                }

                if( result.endsWith("&") ) {
                    result = result.substring(0, result.length() - 1);
                }

                return result;
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        return url;
    }
    
    public static boolean isEmptyString(Object o) {
        return o == null || "".equals(o.toString().trim());
    }

    /**
     * Calculates full URL for specified page URL and link
     * which could be full, absolute or relative like there can 
     * be found in A or IMG tags. 
     */
    public static String fullUrl(String pageUrl, String link) {
    	if ( isFullUrl(link) ) {
    		return link;
    	} else if ( link != null && link.startsWith("?") ) {
            int qindex = pageUrl.indexOf('?');
            int len = pageUrl.length();
            if (qindex < 0) {
                return pageUrl + link;
            } else if (qindex == len - 1) {
                return pageUrl.substring(0, len - 1) + link;
            } else {
                return pageUrl + "&" + link.substring(1);
            }
        }
    	
    	boolean isLinkAbsolute = link.startsWith("/");
    	
    	if ( !isFullUrl(pageUrl) ) {
    		pageUrl = "http://" + pageUrl;
    	}
    	
    	int slashIndex = isLinkAbsolute ? pageUrl.indexOf("/", 8) : pageUrl.lastIndexOf("/");
    	if (slashIndex <= 8) {
    		pageUrl += "/";
    	} else {
    		pageUrl = pageUrl.substring(0, slashIndex+1);
    	}
    	
    	return isLinkAbsolute ? pageUrl + link.substring(1) : pageUrl + link; 
    }

    /**
     * Checks if specified link is full URL.
     * @param link
     * @return True, if full URl, false otherwise.
     */
    public static boolean isFullUrl(String link) {
        if (link == null) {
            return false;
        }
        link = link.trim().toLowerCase();
        return link.startsWith("http://") || link.startsWith("https://") || link.startsWith("file://");
    }

    private static String encodeUrlParam(String value, String charset) throws UnsupportedEncodingException {
        if (value == null) {
            return "";
        }

        try {
    		String decoded = URLDecoder.decode(value, charset);
    		
    		String result = "";
    		for (int i = 0; i < decoded.length(); i++) {
    			char ch = decoded.charAt(i);
    			result += (ch == '#') ? "#" : URLEncoder.encode(String.valueOf(ch), charset);
    		}

    		return result;
    	} catch (IllegalArgumentException e) {
    		return value;
    	}
    }

    public static String sampleRequest()
    {
        String ret = "";
    
        ret += "{\n";
        ret += "   \"url\"         : \"http://www.example.com/\",\n";
        ret += "   \"method\"      : \"POST\",\n";
        ret += "   \"charset\"     : \"utf-8\",\n";
        ret += "   \"headers\"     : {},\n";
        ret += "   \"credentials\" : {\n";
        ret += "          \"user\"   : \"admin\",\n";
        ret += "          \"passwd\" : \"xxx\"\n";
        ret += "   },\n";
        ret += "   \"contenttype\" : \"" + DEFAULT_CONTENT_TYPE + "\",\n";
        ret += "   \"data\"        :  {\"a\":\"kkk\"}\n";
        ret += "}";
    
        return ret;
    }
}
