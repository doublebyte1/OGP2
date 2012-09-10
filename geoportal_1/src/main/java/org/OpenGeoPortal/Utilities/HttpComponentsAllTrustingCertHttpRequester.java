package org.OpenGeoPortal.Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpComponentsAllTrustingCertHttpRequester implements HttpRequester {
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String contentType;
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	private HttpClient getHttpClient(){
		//since the php script we are accessing is https, but doesn't require certs, we need to create a context that essentially 
		//ignores certs
		TrustManager[] trustAllCerts = new TrustManager[] { new AllTrustingTrustManager() };
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e1) {
			logger.error("NoSuchAlgorithm :" + e1.getMessage());
			e1.printStackTrace();
		}
		try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e1) {
			logger.error("KeyManagementException :" + e1.getMessage());
			e1.printStackTrace();
		}
		SchemeSocketFactory sf = (SchemeSocketFactory) new SSLSocketFactory(sc, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme sch = new Scheme("https", 443, sf);
		DefaultHttpClient httpclient = new DefaultHttpClient();

		httpclient.getConnectionManager().getSchemeRegistry().register(sch);
		return httpclient;
	}
	
	public InputStream sendGetRequest(String url){

		HttpClient httpclient = getHttpClient();
		InputStream replyStream = null;
		try {
			HttpGet httpget = new HttpGet(url);
			logger.debug("executing get request " + httpget.getURI());

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			this.setContentType(entity.getContentType().getValue());
			if (entity != null) {
				 replyStream = entity.getContent();
			} 
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			//httpclient.getConnectionManager().shutdown();
		}
		return replyStream;
	}
	
	@Override
	public InputStream sendRequest(String serviceURL, String requestString,
			String requestMethod) throws IOException {
		return sendRequest(serviceURL, requestString, requestMethod, "text/xml");
	}

	private InputStream sendPostRequest(String serviceURL,
			String requestBody, String contentType) {
		HttpClient httpclient = getHttpClient();

		InputStream replyStream = null;
		try {
			HttpPost httppost = new HttpPost(serviceURL);
			StringEntity postEntity = new StringEntity(requestBody, contentType, "UTF-8");
			httppost.setEntity(postEntity);
			logger.info("executing POST request to " + httppost.getURI());
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			this.setContentType(entity.getContentType().getValue());
			if (entity != null) {
				 replyStream = entity.getContent();
			} 
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			//httpclient.getConnectionManager().shutdown();
		}
		return replyStream;
	}

	@Override
	public InputStream sendRequest(String serviceURL, String requestString,
			String requestMethod, String contentType) throws IOException {
		if (requestMethod.equals("POST")){
			return sendPostRequest(serviceURL, requestString, contentType);
		} else if (requestMethod.equals("GET")){
			return sendGetRequest(serviceURL + "?" + requestString);
		} else {
			//throw new Exception("The method " + requestMethod + " is not supported.");
			return null;
		}
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

}
