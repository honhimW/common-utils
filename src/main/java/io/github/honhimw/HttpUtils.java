package io.github.honhimw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * http客户端, JDK11后自带{@link java.net.http.HttpClient}路由数无法限制(可能有?)
 *
 * @author hon_him
 * @since 2022-05-31
 */

@Slf4j
@SuppressWarnings({
    "unused",
    "UnusedReturnValue",
})
public class HttpUtils {

    private HttpUtils() {
    }

    /**
     * 最大连接数
     */
    public static final int MAX_TOTAL_CONNECTIONS = 1_000;

    /**
     * 每个路由最大连接数
     */
    public static final int MAX_ROUTE_CONNECTIONS = 200;

    /**
     * 连接超时时间
     */
    public static final int CONNECT_TIMEOUT = 2_000;

    private static HttpUtils instance;

    private final Charset defaultCharset = StandardCharsets.UTF_8;

    private PoolingHttpClientConnectionManager cm;

    private CloseableHttpClient httpClient;

    @Getter
    private RequestConfig defaultRequestConfig;

    private static ObjectMapper OBJECT_MAPPER;

    private void init(BuildFunction buildFunction) {
        SSLContext ctx = null;

        // 忽略证书错误
        try {
            ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(ctx, "证书异常"); // never happen
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
            .<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE))
            .build();
        // 忽略证书错误

        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);

        Builder reqConfigBuilder = RequestConfig.custom()
            .setConnectionRequestTimeout(CONNECT_TIMEOUT)
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setSocketTimeout(CONNECT_TIMEOUT);

        SocketConfig.Builder builder = SocketConfig.custom();
        builder.setSoKeepAlive(true);
        cm.setDefaultSocketConfig(builder.build());

        HttpClientBuilder clientBuilder = HttpClients.custom();
        httpClient = buildFunction.build(clientBuilder, cm, reqConfigBuilder);
        defaultRequestConfig = reqConfigBuilder.build();
        OBJECT_MAPPER = JsonUtils.getMapper();
    }

    @FunctionalInterface
    public interface BuildFunction {

        CloseableHttpClient build(HttpClientBuilder httpClientBuilder,
                                  PoolingHttpClientConnectionManager clientConnectionManager, Builder requestConfigBuilder);

    }

    /**
     * 获取实例
     */
    public static HttpUtils getInstance(String route, int max) {
        setMaxPerRoute(route, max);
        return getInstance();
    }

    public static HttpUtils getInstance() {
        return getInstance((httpClientBuilder, clientConnectionManager, requestConfigBuilder) ->
            httpClientBuilder
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setConnectionManager(clientConnectionManager)
                .setRetryHandler((e, i, httpContext) -> i < 3
                    && (e instanceof InterruptedIOException
                    || e instanceof SocketException
                    || e instanceof NoHttpResponseException))
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .build(), false);
    }

    public static synchronized HttpUtils getInstance(BuildFunction buildFunction, boolean force) {
        if (instance == null || force) {
            HttpUtils newInstance = new HttpUtils();
            newInstance.init(buildFunction);
            instance = newInstance;
        }
        return instance;
    }

    /**
     * 设置每路由最大连接数
     *
     * @param route url/uri
     * @param max   改路有最大连接数
     */
    public static void setMaxPerRoute(String route, int max) {
        try {
            URL url = new URL(route);
            HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
            instance.cm.setMaxPerRoute(new HttpRoute(host), max);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("url格式错误: " + route);
        }
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        OBJECT_MAPPER = objectMapper;
    }

    public static HttpClient getHttpClient() {
        return getInstance().httpClient;
    }

    public HttpResult request(String method, String url, Consumer<RequestConfigurer> configurer) throws URISyntaxException, IOException {
        return request(method, url, configurer, httpResult -> httpResult);
    }

    public <T> T request(String method, String url, Consumer<RequestConfigurer> configurer, Function<HttpResult, T> resultMapper) throws URISyntaxException, IOException {
        _assertState(StringUtils.isNotBlank(url), "URL should not be blank");
        _assertState(Objects.nonNull(configurer), "String should not be null");
        RequestConfigurer requestConfigurer = new RequestConfigurer()
            .method(method)
            .charset(defaultCharset)
            .url(url)
            .config(RequestConfig.copy(defaultRequestConfig)
                .build());
        configurer.accept(requestConfigurer);
        HttpResult httpResult = request(requestConfigurer);
        return resultMapper.apply(httpResult);
    }

    public HttpResult get(String url) throws URISyntaxException, IOException {
        return request(HttpGet.METHOD_NAME, url, requestConfigurer -> {
        });
    }

    public HttpResult post(String url) throws URISyntaxException, IOException {
        return request(HttpPost.METHOD_NAME, url, requestConfigurer -> {
        });
    }

    public HttpResult put(String url) throws URISyntaxException, IOException {
        return request(HttpPut.METHOD_NAME, url, requestConfigurer -> {
        });
    }

    public HttpResult delete(String url) throws URISyntaxException, IOException {
        return request(HttpDelete.METHOD_NAME, url, requestConfigurer -> {
        });
    }

    public HttpResult get(String url, Consumer<RequestConfigurer> configurer) throws URISyntaxException, IOException {
        return request(HttpGet.METHOD_NAME, url, configurer);
    }

    public HttpResult post(String url, Consumer<RequestConfigurer> configurer) throws URISyntaxException, IOException {
        return request(HttpPost.METHOD_NAME, url, configurer);
    }

    public HttpResult put(String url, Consumer<RequestConfigurer> configurer) throws URISyntaxException, IOException {
        return request(HttpPut.METHOD_NAME, url, configurer);
    }

    public HttpResult delete(String url, Consumer<RequestConfigurer> configurer) throws URISyntaxException, IOException {
        return request(HttpDelete.METHOD_NAME, url, configurer);
    }

    public <T> T get(String url, Consumer<RequestConfigurer> configurer, Function<HttpResult, T> resultMapper) throws URISyntaxException, IOException {
        return request(HttpGet.METHOD_NAME, url, configurer, resultMapper);
    }

    public <T> T post(String url, Consumer<RequestConfigurer> configurer, Function<HttpResult, T> resultMapper) throws URISyntaxException, IOException {
        return request(HttpPost.METHOD_NAME, url, configurer, resultMapper);
    }

    public <T> T put(String url, Consumer<RequestConfigurer> configurer, Function<HttpResult, T> resultMapper) throws URISyntaxException, IOException {
        return request(HttpPut.METHOD_NAME, url, configurer, resultMapper);
    }

    public <T> T delete(String url, Consumer<RequestConfigurer> configurer, Function<HttpResult, T> resultMapper) throws URISyntaxException, IOException {
        return request(HttpDelete.METHOD_NAME, url, configurer, resultMapper);
    }

    public HttpResult request(RequestConfigurer configurer) throws URISyntaxException, IOException {
        CloseableHttpResponse response = null;
        HttpResult result = new HttpResult();
        long t = System.currentTimeMillis();

        try {
            HttpRequestBase request;
            if (log.isDebugEnabled()) {
                log.debug("url: " + configurer.url);
            }

            switch (configurer.method) {
                case HttpGet.METHOD_NAME -> request = new HttpGet();
                case HttpDelete.METHOD_NAME -> request = new HttpDelete();
                case HttpHead.METHOD_NAME -> request = new HttpHead();
                case HttpOptions.METHOD_NAME -> request = new HttpOptions();
                case HttpTrace.METHOD_NAME -> request = new HttpTrace();
                case HttpPost.METHOD_NAME -> {
                    HttpPost httpPost = new HttpPost();
                    Optional.ofNullable(configurer.httpEntity).ifPresent(httpPost::setEntity);
                    request = httpPost;
                }
                case HttpPut.METHOD_NAME -> {
                    HttpPut httpPut = new HttpPut();
                    Optional.ofNullable(configurer.httpEntity).ifPresent(httpPut::setEntity);
                    request = httpPut;
                }
                default ->
                    throw new IllegalArgumentException(String.format("not support http method [%s]", configurer.method));
            }
            URI uri = new URIBuilder(configurer.url, configurer.charset).addParameters(configurer.params).build();
            request.setURI(uri);

            if (MapUtils.isNotEmpty(configurer.headers)) {
                configurer.headers.forEach((name, values) -> values.forEach(value -> request.addHeader(name, value)));
            }

            Optional.ofNullable(configurer.config).ifPresent(request::setConfig);

            HttpClientContext context = configurer.getContext();
            response = httpClient.execute(request, context);
            Optional.of(context)
                .map(HttpClientContext::getCookieStore)
                .ifPresent(result::setCookieStore);
            Optional.ofNullable(response)
                .map(HttpResponse::getStatusLine)
                .map(StatusLine::getStatusCode)
                .ifPresent(result::setStatusCode);

            if (Objects.nonNull(response)) {
                HttpEntity entity = response.getEntity();
                if (Objects.nonNull(entity)) {
                    result.setEntity(entity);
                    Optional.ofNullable(configurer.inputConfig)
                        .ifPresent(result::setInputConfig);
                    result.read();
                }
            }
            Optional.ofNullable(response)
                .map(HttpResponse::getAllHeaders)
                .ifPresent(result::setHeaders);
            if (log.isDebugEnabled()) {
                log.debug("response: cost=" + (System.currentTimeMillis() - t) + "ms, code="
                    + result.getStatusCode() + ", " + result.getEntity());
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("exception: cost=" + (System.currentTimeMillis() - t) + "ms, {}", e.getMessage());
            }
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public final static class RequestConfigurer {

        private String method;

        private Charset charset;

        private String url;

        private final Map<String, List<String>> headers = new HashMap<>();

        private final List<NameValuePair> params = new ArrayList<>();

        private HttpEntity httpEntity;

        private HttpContext context = HttpClientContext.create();

        private RequestConfig config;

        private Consumer<InputStream> inputConfig;

        public RequestConfigurer method(String method) {
            this.method = method;
            return this;
        }

        public RequestConfigurer charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public RequestConfigurer url(String url) {
            this.url = url;
            return this;
        }

        public RequestConfigurer header(String name, String value) {
            List<String> list = this.headers.get(name);
            if (Objects.isNull(list)) {
                list = new ArrayList<>();
                this.headers.put(name, list);
            }
            list.add(value);
            return this;
        }

        public RequestConfigurer headers(Map<String, String> headers) {
            headers.forEach(this::header);
            return this;
        }

        public RequestConfigurer param(String name, String value) {
            BasicNameValuePair basicNameValuePair = new BasicNameValuePair(name, value);
            params.add(basicNameValuePair);
            return this;
        }

        public RequestConfigurer params(Map<String, String> headers) {
            headers.forEach(this::param);
            return this;
        }

        public RequestConfigurer config(RequestConfig config) {
            this.config = config;
            return this;
        }

        public RequestConfigurer config(Consumer<Builder> consumer) {
            Builder copy = RequestConfig.copy(config);
            consumer.accept(copy);
            this.config = copy.build();
            return this;
        }

        public RequestConfigurer context(HttpContext context) {
            this.context = context;
            return this;
        }

        public RequestConfigurer body(Consumer<BodyModel> configurer) {
            if (Objects.isNull(httpEntity)) {
                BodyModel bodyModel = new BodyModel();
                configurer.accept(bodyModel);
                Body body = bodyModel.getBody();
                if (Objects.nonNull(body)) {
                    httpEntity = body.toEntity(this.charset);
                    if (StringUtils.isNotBlank(body.contentType())) {
                        header(HttpHeaders.CONTENT_TYPE, body.contentType());
                    }
                }
            }
            return this;
        }

        public RequestConfigurer result(Consumer<InputStream> configurer) {
            inputConfig = configurer;
            return this;
        }

        private HttpClientContext getContext() {
            HttpClientContext ctx;
            if (Objects.isNull(context)) {
                ctx = HttpClientContext.create();
                ctx.setCookieStore(new BasicCookieStore());
            } else if (!(context instanceof HttpClientContext)) {
                ctx = HttpClientContext.adapt(context);
                ctx.setCookieStore(new BasicCookieStore());
            } else {
                ctx = (HttpClientContext) context;
            }
            return ctx;

        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class BodyModel {

            private Body body;

            protected Body getBody() {
                return body;
            }

            public BodyModel raw(Consumer<RawBodyModel> configurer) {
                return type(RawBodyModel::new, configurer);
            }

            public BodyModel formData(Consumer<FormDataBodyModel> configurer) {
                return type(FormDataBodyModel::new, configurer);
            }

            public BodyModel binary(Consumer<BinaryBodyModel> configurer) {
                return type(BinaryBodyModel::new, configurer);
            }

            public BodyModel formUrlEncoded(Consumer<FormUrlEncodedBodyModel> configurer) {
                return type(FormUrlEncodedBodyModel::new, configurer);
            }

            public <T extends Body> BodyModel type(Supplier<T> buildable, Consumer<T> configurer) {
                Objects.requireNonNull(buildable);
                Objects.requireNonNull(configurer);
                if (Objects.isNull(body)) {
                    T built = buildable.get();
                    if (Objects.nonNull(built)) {
                        configurer.accept(built);
                        body = built;
                    }
                }
                return this;
            }
        }


        public static abstract class Body {
            protected void init() {

            }

            protected abstract String contentType();

            protected abstract HttpEntity toEntity(Charset charset);
        }


        public static class RawBodyModel extends Body {

            public static final ContentType TEXT_PLAIN = ContentType.TEXT_PLAIN;
            public static final ContentType APPLICATION_JSON = ContentType.APPLICATION_JSON;
            public static final ContentType TEXT_HTML = ContentType.TEXT_HTML;
            public static final ContentType APPLICATION_XML = ContentType.TEXT_XML;

            private String raw;

            private ContentType contentType = TEXT_PLAIN;

            @Override
            protected String contentType() {
                return Optional.ofNullable(contentType).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected HttpEntity toEntity(Charset charset) {
                return new StringEntity(raw, charset);
            }

            public RawBodyModel text(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = TEXT_PLAIN;
                }
                return this;
            }

            public RawBodyModel json(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = APPLICATION_JSON;
                }
                return this;
            }

            public RawBodyModel json(Object obj) {
                if (Objects.isNull(raw) && Objects.nonNull(obj)) {
                    try {
                        this.raw = OBJECT_MAPPER.writeValueAsString(obj);
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                    this.contentType = APPLICATION_JSON;
                }
                return this;
            }

            public RawBodyModel html(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = TEXT_HTML;
                }
                return this;
            }

            public RawBodyModel xml(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = APPLICATION_XML;
                }
                return this;
            }
        }

        public static class BinaryBodyModel extends Body {

            private ContentType contentType;

            private Supplier<byte[]> bytesSupplier;

            @Override
            protected String contentType() {
                return Optional.ofNullable(contentType).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected HttpEntity toEntity(Charset charset) {
                return Optional.ofNullable(bytesSupplier)
                    .map(Supplier::get)
                    .map(bytes -> new ByteArrayEntity(bytes, 0, bytes.length, contentType))
                    .orElse(null);
            }

            public BinaryBodyModel file(File file) {
                if (Objects.isNull(bytesSupplier)) {
                    bytesSupplier = () -> {
                        try {
                            return FileUtils.readFileToByteArray(file);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                }
                return this;
            }

            public BinaryBodyModel bytes(byte[] bytes) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> bytes;
                }
                return this;
            }

            public BinaryBodyModel inputStream(InputStream ips) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> {
                        try {
                            return ips.readAllBytes();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                }
                return this;
            }

            public BinaryBodyModel file(File file, ContentType contentType) {
                if (Objects.isNull(bytesSupplier)) {
                    bytesSupplier = () -> {
                        try {
                            return FileUtils.readFileToByteArray(file);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                    this.contentType = contentType;
                }
                return this;
            }

            public BinaryBodyModel bytes(byte[] bytes, ContentType contentType) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> bytes;
                    this.contentType = contentType;
                }
                return this;
            }

            public BinaryBodyModel inputStream(InputStream ips, ContentType contentType) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> {
                        try {
                            return ips.readAllBytes();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                    this.contentType = contentType;
                }
                return this;
            }

        }

        public static class FormDataBodyModel extends Body {

            public static final ContentType MULTIPART_FORM_DATA = ContentType.MULTIPART_FORM_DATA;

            private final MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            @Override
            protected String contentType() {
                return null;
//                return Optional.ofNullable(MULTIPART_FORM_DATA).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected HttpEntity toEntity(Charset charset) {
                return builder.setCharset(charset).setContentType(ContentType.MULTIPART_FORM_DATA).build();
            }

            public FormDataBodyModel text(String name, String value) {
                builder.addTextBody(name, value, MULTIPART_FORM_DATA);
                return this;
            }

            public FormDataBodyModel file(String name, File file) {
                builder.addBinaryBody(name, file, MULTIPART_FORM_DATA, name);
                return this;
            }

            public FormDataBodyModel bytes(String name, byte[] bytes) {
                builder.addBinaryBody(name, bytes, MULTIPART_FORM_DATA, name);
                return this;
            }

            public FormDataBodyModel inputStream(String name, InputStream ips) {
                builder.addBinaryBody(name, ips, MULTIPART_FORM_DATA, name);
                return this;
            }

            public FormDataBodyModel text(String name, String value, ContentType contentType) {
                builder.addTextBody(name, value, contentType);
                return this;
            }

            public FormDataBodyModel file(String name, File file, ContentType contentType, String fileName) {
                builder.addBinaryBody(name, file, contentType, fileName);
                return this;
            }

            public FormDataBodyModel bytes(String name, byte[] bytes, ContentType contentType, String fileName) {
                builder.addBinaryBody(name, bytes, contentType, fileName);
                return this;
            }

            public FormDataBodyModel inputStream(String name, InputStream ips, ContentType contentType, String fileName) {
                builder.addBinaryBody(name, ips, contentType, fileName);
                return this;
            }

        }

        public static class FormUrlEncodedBodyModel extends Body {

            public static final ContentType APPLICATION_FORM_URLENCODED = ContentType.APPLICATION_FORM_URLENCODED;

            private final List<NameValuePair> nameValuePairs = new ArrayList<>();

            @Override
            protected String contentType() {
                return Optional.ofNullable(APPLICATION_FORM_URLENCODED).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected HttpEntity toEntity(Charset charset) {
                return new UrlEncodedFormEntity(nameValuePairs, charset);
            }

            public FormUrlEncodedBodyModel text(String name, String value) {
                BasicNameValuePair basicNameValuePair = new BasicNameValuePair(name, value);
                nameValuePairs.add(basicNameValuePair);
                return this;
            }

        }
    }

    public static class ResponseWrapper {

//        public Function<HttpResult, HttpResult> defaultWrapper() {
//            return httpResult -> {
//                HttpEntity entity = httpResult.getEntity();
//                InputStream content = entity.getContent();
//                return httpResult;
//            }
//        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HttpResult {

        private int statusCode;
        private HttpEntity entity;
        private final Map<String, List<String>> headers = new HashMap<>();
        private Charset charset = StandardCharsets.UTF_8;
        private byte[] content;
        private CookieStore cookieStore;

        private Consumer<InputStream> inputConfig = inputStream -> {
            ByteArrayOutputStream baops = new ByteArrayOutputStream();
            try {
                IOUtils.copy(inputStream, baops);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            content = baops.toByteArray();
        };

        @Override
        public String toString() {
            return "HttpResult [statusCode=" + statusCode + ", entity=" + entity + "]";
        }

        public boolean isOK() {
            return statusCode == HttpStatus.SC_OK;
        }

        public int getStatusCode() {
            return statusCode;
        }

        private void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        private void setCharset(String charset) {
            try {
                this.charset = Charset.forName(charset);
            } catch (Exception ignored) {
            }
        }

        public Charset getCharset() {
            return charset;
        }

        public HttpEntity getEntity() {
            return entity;
        }

        private void setEntity(HttpEntity entity) {
            this.entity = entity;
        }

        private void setCookieStore(CookieStore cookieStore) {
            this.cookieStore = cookieStore;
        }

        private void setInputConfig(Consumer<InputStream> inputConfig) {
            this.inputConfig = inputConfig;
        }

        private void read() throws IOException {
            Optional.ofNullable(this.entity)
                .map(HttpEntity::getContentType)
                .map(Header::getElements)
                .map(headerElements -> headerElements[0])
                .map(headerElement -> headerElement.getParameterByName("charset"))
                .map(NameValuePair::getValue)
                .ifPresent(this::setCharset);
            if (Objects.nonNull(entity)) {
                InputStream ips = entity.getContent();
                inputConfig.accept(ips);
            }
        }

        public Map<String, List<String>> getAllHeaders() {
            return headers;
        }

        public String getHeader(String name) {
            List<String> list = headers.get(name);
            if (CollectionUtils.isNotEmpty(list)) {
                return list.get(0);
            }
            return null;
        }

        public List<String> getHeaders(String name) {
            return headers.get(name);
        }

        private void setHeader(String name, String value) {
            List<String> list = this.headers.get(name);
            if (Objects.isNull(list)) {
                list = new ArrayList<>();
                this.headers.put(name, list);
            }
            list.add(value);
        }

        private void setHeader(Header header) {
            setHeader(header.getName(), header.getValue());
        }

        private void setHeaders(Header[] headers) {
            for (Header header : headers) {
                setHeader(header.getName(), header.getValue());
            }
        }

        public List<Cookie> getCookies() {
            return cookieStore.getCookies();
        }

        public List<Cookie> getCookie(String name) {
            return cookieStore.getCookies().stream().filter(cookie -> StringUtils.equals(cookie.getName(), name)).toList();
        }

        public byte[] content() {
            return wrap(bytes -> bytes);
        }

        public String str() {
            return wrap(bytes -> new String(bytes, charset));
        }

        public <T> T json(Class<T> type) {
            return wrap(bytes -> {
                try {
                    return OBJECT_MAPPER.readValue(bytes, type);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public <T> T wrap(Function<byte[], T> wrapper) {
            Objects.requireNonNull(wrapper, "wrapper should not be null");
            return Optional.ofNullable(content).map(wrapper).orElse(null);
        }

    }

    private void _assertState(boolean state, String message) {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

}
