/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Gong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dtflys.forest.http;

import com.dtflys.forest.callback.OnProgress;
import com.dtflys.forest.converter.ForestConverter;
import com.dtflys.forest.interceptor.InterceptorAttributes;
import com.dtflys.forest.multipart.ForestMultipart;
import com.dtflys.forest.retryer.Retryer;
import com.dtflys.forest.ssl.SSLKeyStore;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.backend.HttpBackend;
import com.dtflys.forest.backend.HttpExecutor;
import com.dtflys.forest.handler.LifeCycleHandler;
import com.dtflys.forest.interceptor.Interceptor;
import com.dtflys.forest.interceptor.InterceptorChain;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.RequestNameValue;
import com.dtflys.forest.utils.StringUtils;

import java.io.InputStream;
import java.util.*;

import static com.dtflys.forest.mapping.MappingParameter.*;

/**
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2016-03-24
 */
public class ForestRequest<T> {

    private final static long DEFAULT_PROGRESS_STEP = 1024 * 10;

    private final ForestConfiguration configuration;

    private String protocol;

    private String url;

    private Map<String, Object> query = new LinkedHashMap<>();

    private ForestRequestType type;

    private String charset;

    private String responseEncode = "UTF-8";

    private boolean async;

    private ForestDataType dataType;

    private int timeout = 3000;

    private String sslProtocol;

    private int retryCount = 0;

    private long maxRetryInterval = 0;

    private Map<String, Object> data = new LinkedHashMap<String, Object>();

    private List bodyList = new LinkedList<>();

    private ForestHeaderMap headers = new ForestHeaderMap();

    private List<ForestMultipart> multiparts = new LinkedList<>();

    private String filename;

    private final Object[] arguments;

    private String requestBody;

    private InputStream certificateInputStream;

    private OnSuccess onSuccess;

    private OnError onError;

    private boolean isDownloadFile = false;

    private long progressStep = DEFAULT_PROGRESS_STEP;

    private OnProgress onProgress;

    private InterceptorChain interceptorChain = new InterceptorChain();

    private Map<Class, InterceptorAttributes> interceptorAttributes = new HashMap<>();

    private Retryer retryer;

    private Map<String, Object> attachments = new HashMap<>();

    private ForestConverter decoder;

    private boolean logEnable = true;

    private SSLKeyStore keyStore;

    public ForestRequest(ForestConfiguration configuration, Object[] arguments) {
        this.configuration = configuration;
        this.arguments = arguments;
    }

    public ForestRequest(ForestConfiguration configuration) {
        this(configuration, new Object[0]);
    }


    public ForestConfiguration getConfiguration() {
        return configuration;
    }

    public String getProtocol() {
        return protocol;
    }

    public ForestRequest setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ForestRequest setUrl(String url) {
        this.url = url;
        return this;
    }

    public Map<String, Object> getQueryMap() {
        return query;
    }

    public Object getQuery(String name) {
        return query.get(name);
    }

    public String getQueryString() {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = query.keySet().iterator();
        while (iterator.hasNext()) {
            String name  = iterator.next();
            Object value = query.get(name);
            if (value != null) {
                builder.append(name);
                builder.append("=");
                builder.append(value);
            }
            if (iterator.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    public ForestRequest addQuery(String name, Object value) {
        this.query.put(name, value);
        return this;
    }

    public ForestRequestType getType() {
        return type;
    }

    public ForestRequest setType(ForestRequestType type) {
        this.type = type;
        return this;
    }

    public String getFilename() {
        if (filename == null) {
            synchronized (this) {
                if (filename == null) {
                    String[] strs = getUrl().split("/");
                    filename = strs[strs.length - 1];
                }
            }
        }
        return filename;
    }

    public String getContentEncoding() {
        return headers.getValue("Content-Encoding");
    }

    public ForestRequest setContentEncoding(String contentEncoding) {
        addHeader("Content-Encoding", contentEncoding);
        return this;
    }

    public String getUserAgent() {
        return headers.getValue("User-Agent");
    }

    public ForestRequest setUserAgent(String userAgent) {
        addHeader("User-Agent", userAgent);
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public ForestRequest setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public String getResponseEncode() {
        return responseEncode;
    }

    public ForestRequest<T> setResponseEncode(String responseEncode) {
        this.responseEncode = responseEncode;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public ForestRequest setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public List getBodyList() {
        return bodyList;
    }

    public void setBodyList(List bodyList) {
        this.bodyList = bodyList;
    }

    public ForestDataType getDataType() {
        return dataType;
    }

    public ForestRequest setDataType(ForestDataType dataType) {
        this.dataType = dataType;
        return this;
    }

    public String getContentType() {
        return headers.getValue("Content-Type");
    }

    public ForestRequest setContentType(String contentType) {
        addHeader("Content-Type", contentType);
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public ForestRequest setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public ForestRequest setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public ForestRequest setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public long getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public ForestRequest setMaxRetryInterval(long maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }


    @Deprecated
    public ForestRequest addData(String name, Object value) {
        this.data.put(name, value);
        return this;
    }

    @Deprecated
    public ForestRequest addData(RequestNameValue nameValue) {
        this.data.put(nameValue.getName(), nameValue.getValue());
        return this;
    }

    public ForestRequest addData(List<RequestNameValue> data) {
        putMapAddList(this.data, data);
        return this;
    }

    public ForestRequest addBody(Object bodyContent) {
        bodyList.add(bodyContent);
        return this;
    }

    public ForestRequest replaceBody(Object bodyContent) {
        bodyList.clear();
        bodyList.add(bodyContent);
        return this;
    }

    public ForestRequest addBody(String name, Object value) {
        this.data.put(name, value);
        return this;
    }

    public ForestRequest addBody(RequestNameValue nameValue) {
        this.data.put(nameValue.getName(), nameValue.getValue());
        return this;
    }

    public ForestRequest addBody(List<RequestNameValue> data) {
        putMapAddList(this.data, data);
        return this;
    }


    public List<RequestNameValue> getQueryNameValueList() {
        List<RequestNameValue> nameValueList = new ArrayList<>();
        for (Iterator<Map.Entry<String, Object>> iterator = query.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Object> entry = iterator.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                RequestNameValue nameValue = new RequestNameValue(name, value, TARGET_QUERY);
                nameValueList.add(nameValue);
            }
        }
        return nameValueList;

    }

    public List<RequestNameValue> getDataNameValueList() {
        List<RequestNameValue> nameValueList = new ArrayList<>();
        for (Iterator<Map.Entry<String, Object>> iterator = data.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Object> entry = iterator.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                RequestNameValue nameValue = new RequestNameValue(name, value, TARGET_BODY);
                nameValueList.add(nameValue);
            }
        }
        return nameValueList;
    }


    public List<RequestNameValue> getHeaderNameValueList() {
        List<RequestNameValue> nameValueList = new ArrayList<RequestNameValue>();
        for (Iterator<ForestHeader> iterator = headers.headerIterator(); iterator.hasNext(); ) {
            ForestHeader header = iterator.next();
            RequestNameValue nameValue = new RequestNameValue(header.getName(), header.getValue(), TARGET_HEADER);
            nameValueList.add(nameValue);
        }
        return nameValueList;
    }


    public Object getArgument(int index) {
        return arguments[index];
    }

    public Object[] getArguments() {
        return arguments;
    }

    public ForestHeaderMap getHeaders() {
        return headers;
    }

    public ForestHeader getHeader(String name) {
        return headers.getHeader(name);
    }

    public String getHeaderValue(String name) {
        return headers.getValue(name);
    }

    public ForestRequest addHeader(String name, Object value) {
        if (StringUtils.isEmpty(name)) {
            return this;
        }
        this.headers.setHeader(name, String.valueOf(value));
        return this;
    }

    public ForestRequest addHeader(RequestNameValue nameValue) {
        this.addHeader(nameValue.getName(), nameValue.getValue());
        return this;
    }


    public ForestRequest addHeaders(List<RequestNameValue> nameValues) {
        for (RequestNameValue nameValue : nameValues) {
            this.addHeader(nameValue.getName(), nameValue.getValue());
        }
        return this;
    }

    public List<ForestMultipart> getMultiparts() {
        return multiparts;
    }

    public ForestRequest setMultiparts(List<ForestMultipart> multiparts) {
        this.multiparts = multiparts;
        return this;
    }

    private void putMapAddList(Map<String, Object> map, List<RequestNameValue> source) {
        for (int i = 0; i < source.size(); i++) {
            RequestNameValue nameValue = source.get(i);
            if (nameValue.isInQuery()) {
                addQuery(nameValue.getName(), nameValue.getValue());
            } else if (nameValue.isInBody()) {
                map.put(nameValue.getName(), nameValue.getValue());
            }
        }
    }

    public String getRequestBody() {
        return requestBody;
    }

    public ForestRequest setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public InputStream getCertificateInputStream() {
        return certificateInputStream;
    }

    public void setCertificateInputStream(InputStream certificateInputStream) {
        this.certificateInputStream = certificateInputStream;
    }

    public OnSuccess getOnSuccess() {
        return onSuccess;
    }

    public ForestRequest setOnSuccess(OnSuccess onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public OnError getOnError() {
        return onError;
    }

    public ForestRequest setOnError(OnError onError) {
        this.onError = onError;
        return this;
    }

    public boolean isDownloadFile() {
        return isDownloadFile;
    }

    public void setDownloadFile(boolean downloadFile) {
        isDownloadFile = downloadFile;
    }

    public long getProgressStep() {
        return progressStep;
    }

    public ForestRequest setProgressStep(long progressStep) {
        this.progressStep = progressStep;
        return this;
    }

    public OnProgress getOnProgress() {
        return onProgress;
    }

    public ForestRequest setOnProgress(OnProgress onProgress) {
        this.onProgress = onProgress;
        return this;
    }

    public ForestRequest<T> addInterceptor(Interceptor interceptor) {
        interceptorChain.addInterceptor(interceptor);
        return this;
    }

    public InterceptorChain getInterceptorChain() {
        return interceptorChain;
    }

    public ForestRequest addInterceptorAttributes(Class interceptorClass, InterceptorAttributes attributes) {
        InterceptorAttributes oldAttributes = interceptorAttributes.get(interceptorClass);
        if (oldAttributes != null) {
            for (Map.Entry<String, Object> entry : attributes.getAttributeTemplates().entrySet()) {
                oldAttributes.addAttribute(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Object> entry : attributes.getAttributes().entrySet()) {
                oldAttributes.addAttribute(entry.getKey(), entry.getValue());
            }
        } else {
            interceptorAttributes.put(interceptorClass, attributes);
        }
        return this;
    }

    public ForestRequest addInterceptorAttribute(Class interceptorClass, String attributeName, Object attributeValue) {
        InterceptorAttributes attributes = getInterceptorAttributes(interceptorClass);
        if (attributes == null) {
            attributes = new InterceptorAttributes(interceptorClass, new HashMap<>());
            addInterceptorAttributes(interceptorClass, attributes);
        }
        attributes.addAttribute(attributeName, attributeValue);
        return this;
    }


    public Map<Class, InterceptorAttributes> getInterceptorAttributes() {
        return interceptorAttributes;
    }


    public InterceptorAttributes getInterceptorAttributes(Class interceptorClass) {
        return interceptorAttributes.get(interceptorClass);
    }


    public Object getInterceptorAttribute(Class interceptorClass, String attributeName) {
        InterceptorAttributes attributes = interceptorAttributes.get(interceptorClass);
        if (attributes == null) {
            return null;
        }
        return attributes.getAttribute(attributeName);
    }


    public Retryer getRetryer() {
        return retryer;
    }

    public ForestRequest setRetryer(Retryer retryer) {
        this.retryer = retryer;
        return this;
    }

    public ForestRequest addAttachment(String name, Object value) {
        attachments.put(name, value);
        return this;
    }

    public Object getAttachment(String name) {
        return attachments.get(name);
    }

    public ForestConverter getDecoder() {
        return decoder;
    }

    public ForestRequest setDecoder(ForestConverter decoder) {
        this.decoder = decoder;
        return this;
    }

    public boolean isLogEnable() {
        return logEnable;
    }

    public ForestRequest setLogEnable(boolean logEnable) {
        this.logEnable = logEnable;
        return this;
    }

    public SSLKeyStore getKeyStore() {
        return keyStore;
    }

    public ForestRequest setKeyStore(SSLKeyStore keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    /**
     * Execute request
     * @param backend
     * @param lifeCycleHandler
     */
    public void execute(HttpBackend backend, LifeCycleHandler lifeCycleHandler) {
        HttpExecutor executor  = backend.createExecutor(this, lifeCycleHandler);
        if (executor != null) {
            if (interceptorChain.beforeExecute(this)) {
                try {
                    executor.execute(lifeCycleHandler);
                } catch (ForestRuntimeException e) {
                    throw e;
                } finally {
                    executor.close();
                }
            }
        }
    }

}
