package io.github.honhimw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;
import reactor.netty.Connection;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.RequestSender;
import reactor.netty.http.client.HttpClient.ResponseReceiver;
import reactor.netty.http.client.HttpClientForm;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.ConnectionProvider.Builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 响应式http工具类, 基于reactor-netty-http, 可以快速获取响应结果, 不需要等待请求发送完毕, 需要客户端和服务端同时支持
 * <dependency>
 *   <groupId>org.apache.httpcomponents</groupId>
 *   <artifactId>httpcore</artifactId>
 *   <version>${httpclient.version}</version>
 * </dependency>
 *
 * @author hon_him
 * @see RequestConfig.Builder 默认请求配置
 * @since 2023-02-22
 */
@SuppressWarnings({
    "unused",
    "UnusedReturnValue",
})
public class RxHttpUtils {

    private static final Logger log = LoggerFactory.getLogger(RxHttpUtils.class);

    private RxHttpUtils() {
    }

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_HEAD = "HEAD";

    public static RxHttpUtils getInstance() {
        RxHttpUtils rxHttpUtils = new RxHttpUtils();
        rxHttpUtils.init();
        return rxHttpUtils;
    }

    public static RxHttpUtils getInstance(Consumer<RequestConfig.Builder> configurer) {
        RxHttpUtils rxHttpUtils = new RxHttpUtils();
        RequestConfig.Builder copy = RequestConfig.DEFAULT_CONFIG.copy();
        configurer.accept(copy);
        RequestConfig requestConfig = copy.build();
        rxHttpUtils.init(requestConfig);
        return rxHttpUtils;
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
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
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);

    public static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private static final Charset defaultCharset = StandardCharsets.UTF_8;

    private static ObjectMapper OBJECT_MAPPER = JsonUtils.getMapper();

    private HttpClient httpClient;

    private void init() {
        init(RequestConfig.DEFAULT_CONFIG);
    }

    private void init(RequestConfig requestConfig) {
        Builder connectionProviderBuilder = ConnectionProvider.builder("RtHttpUtils");
        connectionProviderBuilder.maxConnections(MAX_TOTAL_CONNECTIONS);
        connectionProviderBuilder.pendingAcquireMaxCount(MAX_TOTAL_CONNECTIONS);

        httpClient = HttpClient.create(connectionProviderBuilder.build());
        httpClient = requestConfig.config(httpClient);
    }

    public HttpResult get(String url) throws URISyntaxException {
        return request(METHOD_GET, url, configurer -> {
        });
    }

    public HttpResult post(String url) throws URISyntaxException {
        return request(METHOD_POST, url, configurer -> {
        });
    }

    public HttpResult put(String url) throws URISyntaxException {
        return request(METHOD_PUT, url, configurer -> {
        });
    }

    public HttpResult delete(String url) throws URISyntaxException {
        return request(METHOD_DELETE, url, configurer -> {
        });
    }

    public HttpResult options(String url) throws URISyntaxException {
        return request(METHOD_OPTIONS, url, configurer -> {
        });
    }

    public HttpResult head(String url) throws URISyntaxException {
        return request(METHOD_HEAD, url, configurer -> {
        });
    }

    public HttpResult get(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_GET, url, configurer);
    }

    public HttpResult post(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_POST, url, configurer);
    }

    public HttpResult put(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_PUT, url, configurer);
    }

    public HttpResult delete(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_DELETE, url, configurer);
    }

    public HttpResult options(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_OPTIONS, url, configurer);
    }

    public HttpResult head(String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(METHOD_HEAD, url, configurer);
    }

    public HttpResult request(String method, String url, Consumer<Configurer> configurer) throws URISyntaxException {
        return request(method, url, configurer, httpResult -> httpResult);
    }

    public RxHttpResult rGet(String url) throws URISyntaxException {
        return receiver(METHOD_GET, url, configurer -> {
        });
    }

    public RxHttpResult rPost(String url) throws URISyntaxException {
        return receiver(METHOD_POST, url, configurer -> {
        });
    }

    public RxHttpResult rPut(String url) throws URISyntaxException {
        return receiver(METHOD_PUT, url, configurer -> {
        });
    }

    public RxHttpResult rDelete(String url) throws URISyntaxException {
        return receiver(METHOD_DELETE, url, configurer -> {
        });
    }

    public RxHttpResult rOptions(String url) throws URISyntaxException {
        return receiver(METHOD_OPTIONS, url, configurer -> {
        });
    }

    public RxHttpResult rHead(String url) throws URISyntaxException {
        return receiver(METHOD_HEAD, url, configurer -> {
        });
    }

    public RxHttpResult rGet(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_GET, url, configurer);
    }

    public RxHttpResult rPost(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_POST, url, configurer);
    }

    public RxHttpResult rPut(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_PUT, url, configurer);
    }

    public RxHttpResult rDelete(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_DELETE, url, configurer);
    }

    public RxHttpResult rOptions(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_OPTIONS, url, configurer);
    }

    public RxHttpResult rHead(String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        return receiver(METHOD_HEAD, url, configurer);
    }

    /**
     * 阻塞式请求
     *
     * @param method       HTTP method
     * @param url          HTTP url
     * @param configurer   配置
     * @param resultMapper 响应结果转换
     */
    public <T> T request(String method, String url, Consumer<Configurer> configurer,
        Function<HttpResult, T> resultMapper) throws URISyntaxException {
        _assertState(StringUtils.isNotBlank(url), "URL should not be blank");
        _assertState(Objects.nonNull(configurer), "String should not be null");
        Configurer requestConfigurer = new Configurer()
            .method(method)
            .charset(defaultCharset)
            .url(url);
        configurer.accept(requestConfigurer);
        HttpResult httpResult = request(requestConfigurer);
        return resultMapper.apply(httpResult);
    }

    private HttpResult request(Configurer configurer) throws URISyntaxException {
        HttpResult httpResult = new HttpResult();
        long start = System.currentTimeMillis();
        Mono<byte[]> byteMono = _request(configurer).responseSingle((httpClientResponse, byteBufMono) -> {
            httpResult.setHttpClientResponse(httpClientResponse);
            httpResult.init();
            return byteBufMono.asByteArray();
        });
        byte[] content = byteMono.block();
        if (log.isDebugEnabled()) {
            log.debug("response: cost=" + (System.currentTimeMillis() - start) + "ms, code="
                + httpResult.getStatusCode() + ", length=" + httpResult.contentLength);
        }
        httpResult.setContent(content);
        return httpResult;
    }

    /**
     * 直接返回响应式句柄
     *
     * @param method     HTTP method
     * @param url        HTTP url
     * @param configurer 配置
     */
    public RxHttpResult receiver(String method, String url, Consumer<Configurer> configurer)
        throws URISyntaxException {
        _assertState(StringUtils.isNotBlank(url), "URL should not be blank");
        _assertState(Objects.nonNull(configurer), "String should not be null");
        Configurer requestConfigurer = new Configurer()
            .method(method)
            .charset(defaultCharset)
            .url(url)
            .config(RequestConfig.DEFAULT_CONFIG.copy().build());
        configurer.accept(requestConfigurer);
        ResponseReceiver<?> responseReceiver = _request(requestConfigurer);
        return new RxHttpResult(responseReceiver);
    }

    private ResponseReceiver<?> _request(Configurer configurer) throws URISyntaxException {
        // 构建URI
        URI uri = new URIBuilder(configurer.url, configurer.charset).addParameters(configurer.params).build();
        // 配置请求
        HttpClient client = Optional.ofNullable(configurer.config)
            .map(requestConfig -> requestConfig.config(httpClient))
            .orElse(httpClient);
        // 设置请求头
        if (MapUtils.isNotEmpty(configurer.headers)) {
            client = client.headers(entries -> configurer.headers.forEach(entries::add));
        }

        Configurer.Body body = Optional.ofNullable(configurer.bodyConfigurer)
            .map(bodyModelConsumer -> {
                Configurer.BodyModel bodyModel = new Configurer.BodyModel();
                bodyModelConsumer.accept(bodyModel);
                return bodyModel.getBody();
            }).orElse(null);
        ;
        if (Objects.nonNull(body) && StringUtils.isNotBlank(body.contentType())) {
            client = client.headers(
                entries -> entries.add(HttpHeaderNames.CONTENT_TYPE.toString(), body.contentType()));
        }
        // 构建请求
        ResponseReceiver<?> responseReceiver;
        switch (configurer.method) {
            case "GET" -> responseReceiver = client.get();
            case "DELETE" -> responseReceiver = client.delete();
            case "HEAD" -> responseReceiver = client.head();
            case "OPTIONS" -> responseReceiver = client.options();
            case "POST" -> responseReceiver = client.post();
            case "PUT" -> responseReceiver = client.put();
            default ->
                throw new IllegalArgumentException(String.format("not support http method [%s]", configurer.method));
        }

        responseReceiver = responseReceiver.uri(uri);

        if (responseReceiver instanceof RequestSender requestSender && Objects.nonNull(body)) {
            responseReceiver = body.sender(requestSender, configurer.charset);
        }
        return responseReceiver;
    }

    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestConfig {

        public static RequestConfig DEFAULT_CONFIG = RequestConfig.builder().build();

        private final Duration connectTimeout;
        private final Duration readTimeout;
        private final HttpProtocol[] httpProtocols;
        private final boolean followRedirect;
        private final boolean keepalive;
        private final boolean proxyWithSystemProperties;
        private final boolean enableCompress;
        private final boolean enableRetry;
        private final boolean noSSL;

        private HttpClient config(HttpClient httpClient) {
            HttpClient client = httpClient;
            client = client
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Long.valueOf(connectTimeout.toMillis()).intValue())
//                .option(ChannelOption.SO_TIMEOUT,
//                    Long.valueOf(socketTimeout.toMillis()).intValue()) // 响应式不需要SO_TIMEOUT
                .option(ChannelOption.SO_KEEPALIVE, keepalive)
                .protocol(httpProtocols)
                .keepAlive(keepalive)
                .followRedirect(followRedirect)
                .compress(enableCompress)
                .disableRetry(!enableRetry)
                .responseTimeout(readTimeout)
            ;
            if (proxyWithSystemProperties) {
                client = client.proxyWithSystemProperties();
            }
            if (noSSL) {
                client = client.noSSL();
            }
            return client;
        }

        private Builder copy() {
            Builder builder = RequestConfig.builder();
            builder.connectTimeout(this.connectTimeout);
            builder.readTimeout(this.readTimeout);
            builder.httpProtocol(this.httpProtocols);
            builder.followRedirect(this.followRedirect);
            builder.keepalive(this.keepalive);
            builder.proxyWithSystemProperties(this.proxyWithSystemProperties);
            builder.enableCompress(this.enableCompress);
            builder.enableRetry(this.enableRetry);
            builder.noSSL(this.noSSL);
            return builder;
        }

        private static Builder copy(RequestConfig config) {
            Builder builder = RequestConfig.builder();
            builder.connectTimeout(config.connectTimeout);
            builder.readTimeout(config.readTimeout);
            builder.httpProtocol(config.httpProtocols);
            builder.followRedirect(config.followRedirect);
            builder.keepalive(config.keepalive);
            builder.proxyWithSystemProperties(config.proxyWithSystemProperties);
            builder.enableCompress(config.enableCompress);
            builder.enableRetry(config.enableRetry);
            builder.noSSL(config.noSSL);
            return builder;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * 请求配置Builder, 默认值放在此处
         */
        public static class Builder {

            private Duration connectTimeout = CONNECT_TIMEOUT;
            private Duration readTimeout = READ_TIMEOUT;
            private HttpProtocol[] httpProtocols = {HttpProtocol.HTTP11};
            private boolean followRedirect = true;
            private boolean keepalive = true;
            private boolean proxyWithSystemProperties = true;
            private boolean enableCompress = true;
            private boolean enableRetry = true;
            private boolean noSSL = true;

            public Builder connectTimeout(Duration connectTimeout) {
                this.connectTimeout = connectTimeout;
                return this;
            }

            /**
             * @deprecated 客户端不需要配置{@link {@link ChannelOption#SO_TIMEOUT}
             */
            @Deprecated
            public Builder socketTimeout(Duration socketTimeout) {
                return this;
            }

            public Builder readTimeout(Duration readTimeout) {
                this.readTimeout = readTimeout;
                return this;
            }

            public Builder httpProtocol(HttpProtocol... httpProtocols) {
                this.httpProtocols = httpProtocols;
                return this;
            }

            public Builder followRedirect(boolean followRedirect) {
                this.followRedirect = followRedirect;
                return this;
            }

            public Builder keepalive(boolean keepalive) {
                this.keepalive = keepalive;
                return this;
            }

            public Builder proxyWithSystemProperties(boolean proxyWithSystemProperties) {
                this.proxyWithSystemProperties = proxyWithSystemProperties;
                return this;
            }

            public Builder enableCompress(boolean enableCompress) {
                this.enableCompress = enableCompress;
                return this;
            }

            public Builder enableRetry(boolean enableRetry) {
                this.enableRetry = enableRetry;
                return this;
            }

            public Builder noSSL(boolean noSSL) {
                this.noSSL = noSSL;
                return this;
            }

            public RequestConfig build() {
                return new RequestConfig(
                    connectTimeout,
                    readTimeout,
                    httpProtocols,
                    followRedirect,
                    keepalive,
                    proxyWithSystemProperties,
                    enableCompress,
                    enableRetry,
                    noSSL
                );
            }
        }


    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public final static class Configurer {

        private String method;

        private Charset charset;

        private String url;

        private final Map<String, List<String>> headers = new HashMap<>();

        private final List<Entry<String, String>> params = new ArrayList<>();

        private Consumer<BodyModel> bodyConfigurer;

        private RequestConfig config;

        public Configurer method(String method) {
            this.method = method;
            return this;
        }

        public Configurer charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Configurer url(String url) {
            this.url = url;
            return this;
        }

        public Configurer header(String name, String value) {
            List<String> list = this.headers.get(name);
            if (Objects.isNull(list)) {
                list = new ArrayList<>();
                this.headers.put(name, list);
            }
            list.add(value);
            return this;
        }

        public Configurer headers(Map<String, String> headers) {
            headers.forEach(this::header);
            return this;
        }

        public Configurer param(String name, String value) {
            params.add(Map.entry(name, value));
            return this;
        }

        public Configurer params(Map<String, String> headers) {
            headers.forEach(this::param);
            return this;
        }

        public Configurer config(RequestConfig config) {
            this.config = config;
            return this;
        }

        public Configurer config(Consumer<RequestConfig.Builder> consumer) {
            RequestConfig.Builder copy = RequestConfig.copy(config);
            consumer.accept(copy);
            this.config = copy.build();
            return this;
        }

        public Configurer body(Consumer<BodyModel> configurer) {
            bodyConfigurer = configurer;
            return this;
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class BodyModel {

            private Body body;

            protected Body getBody() {
                return body;
            }

            public BodyModel raw(Consumer<Raw> configurer) {
                return type(Raw::new, configurer);
            }

            public BodyModel formData(Consumer<FormData> configurer) {
                return type(FormData::new, configurer);
            }

            public BodyModel binary(Consumer<Binary> configurer) {
                return type(Binary::new, configurer);
            }

            public BodyModel formUrlEncoded(Consumer<FormUrlEncoded> configurer) {
                return type(FormUrlEncoded::new, configurer);
            }

            public <T extends Body> BodyModel type(
                Supplier<T> buildable, Consumer<T> configurer) {
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

            protected abstract ResponseReceiver<?> sender(RequestSender sender, Charset charset);
        }


        public static class Raw extends Body {

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
            protected ResponseReceiver<?> sender(RequestSender sender, Charset charset) {
                return sender.send(Mono.justOrEmpty(raw)
                    .map(s -> s.getBytes(charset))
                    .map(Unpooled::wrappedBuffer));
            }

            public Raw text(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = TEXT_PLAIN;
                }
                return this;
            }

            public Raw json(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = APPLICATION_JSON;
                }
                return this;
            }

            public Raw json(Object obj) {
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

            public Raw html(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = TEXT_HTML;
                }
                return this;
            }

            public Raw xml(String text) {
                if (Objects.isNull(raw)) {
                    this.raw = text;
                    this.contentType = APPLICATION_XML;
                }
                return this;
            }
        }

        public static class Binary extends Body {

            private ContentType contentType;

            private Supplier<byte[]> bytesSupplier;

            @Override
            protected String contentType() {
                return Optional.ofNullable(contentType).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected ResponseReceiver<?> sender(RequestSender sender, Charset charset) {
                if (Objects.nonNull(bytesSupplier)) {
                    return sender.send(Mono.fromSupplier(bytesSupplier)
                        .map(Unpooled::wrappedBuffer));
                }
                return sender;
            }

            public Binary file(File file) {
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

            public Binary bytes(byte[] bytes) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> bytes;
                }
                return this;
            }

            public Binary inputStream(InputStream ips) {
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

            public Binary file(File file, ContentType contentType) {
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

            public Binary bytes(byte[] bytes, ContentType contentType) {
                if (Objects.isNull(bytesSupplier)) {
                    this.bytesSupplier = () -> bytes;
                    this.contentType = contentType;
                }
                return this;
            }

            public Binary inputStream(InputStream ips, ContentType contentType) {
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

        public static class FormData extends Body {

            public static final ContentType MULTIPART_FORM_DATA = ContentType.MULTIPART_FORM_DATA;

            private final List<Function<HttpClientForm, HttpClientForm>> parts = new ArrayList<>();

            @Override
            protected String contentType() {
                return null;
//                return Optional.ofNullable(MULTIPART_FORM_DATA).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected ResponseReceiver<?> sender(RequestSender sender, Charset charset) {
                return sender.sendForm((httpClientRequest, httpClientForm) -> {
                    HttpClientForm form = httpClientForm
                        .multipart(true)
                        .charset(charset);
                    for (Function<HttpClientForm, HttpClientForm> part : parts) {
                        form = part.apply(form);
                    }
                });
            }

            public FormData text(String name, String value) {
                parts.add(form -> {
                    form.attr(name, value);
                    return form;
                });
                return this;
            }

            public FormData file(String name, File file) {
                return file(name, name, file, MULTIPART_FORM_DATA.getMimeType());
            }

            public FormData bytes(String name, byte[] bytes) {
                return bytes(name, name, bytes, MULTIPART_FORM_DATA.getMimeType());
            }

            public FormData inputStream(String name, InputStream ips) {
                return inputStream(name, name, ips, MULTIPART_FORM_DATA.getMimeType());
            }

            public FormData file(String name, String fileName, File file, String contentType) {
                parts.add(form -> {
                    form.file(name, fileName, file, contentType);
                    return form;
                });
                return this;
            }

            public FormData bytes(String name, String fileName, byte[] bytes, String contentType) {
                parts.add(form -> {
                    form.file(name, fileName, new ByteArrayInputStream(bytes), contentType);
                    return form;
                });
                return this;
            }

            public FormData inputStream(String name, String filename, InputStream ips, String contentType) {
                parts.add(form -> {
                    form.file(name, name, ips, MULTIPART_FORM_DATA.getMimeType());
                    return form;
                });
                return this;
            }

        }

        public static class FormUrlEncoded extends Body {

            public static final ContentType APPLICATION_FORM_URLENCODED = ContentType.APPLICATION_FORM_URLENCODED;

            private final List<Pair> pairs = new ArrayList<>();

            @Override
            protected String contentType() {
                return Optional.ofNullable(APPLICATION_FORM_URLENCODED).map(ContentType::getMimeType).orElse(null);
            }

            @Override
            protected ResponseReceiver<?> sender(RequestSender sender, Charset charset) {
                return sender.sendForm((httpClientRequest, form) -> {
                    form.charset(charset);
                    form.multipart(false);
                    for (Pair pair : pairs) {
                        form = form.attr(pair.name(), pair.value());
                    }
                });
            }

            public FormUrlEncoded text(String name, String value) {
                pairs.add(new Pair(name, value));
                return this;
            }

            public record Pair(String name, String value) {

            }

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HttpResult {

        @Getter
        private int statusCode;
        @Setter(AccessLevel.PRIVATE)
        private HttpClientResponse httpClientResponse;
        private final Map<String, List<String>> headers = new HashMap<>();
        @Setter(AccessLevel.PRIVATE)
        private String contentType;
        @Setter(AccessLevel.PRIVATE)
        private String contentLength;
        private Charset charset = StandardCharsets.UTF_8;
        @Setter(AccessLevel.PRIVATE)
        private byte[] content;
        @Setter(AccessLevel.PRIVATE)
        private Map<CharSequence, Set<Cookie>> cookies;

        private void init() {
            this.statusCode = httpClientResponse.status().code();
            HttpHeaders entries = httpClientResponse.responseHeaders();
            setHeaders(entries);
            Optional.ofNullable(getHeader(HttpHeaderNames.CONTENT_TYPE.toString()))
                .map(contentType -> {
                    setContentType(contentType);
                    return new BasicHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
                })
                .map(BasicHeader::getElements)
                .filter(headerElements -> headerElements.length > 0)
                .map(headerElements -> headerElements[0])
                .map(headerElement -> headerElement.getParameterByName("charset"))
                .map(NameValuePair::getValue)
                .ifPresent(HttpResult.this::setCharset);
            Optional.ofNullable(getHeader(HttpHeaderNames.CONTENT_LENGTH.toString()))
                .ifPresent(HttpResult.this::setContentLength);
            cookies = httpClientResponse.cookies();
        }

        @Override
        public String toString() {
            return "HttpResult [statusCode=" + getStatusCode() + ", content-type=" + contentType + ", content-length="
                + contentLength + "]";
        }

        public boolean isOK() {
            return this.statusCode == HttpResponseStatus.OK.code();
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

        private void setHeader(Entry<String, String> header) {
            setHeader(header.getKey(), header.getValue());
        }

        private void setHeaders(HttpHeaders headers) {
            for (Entry<String, String> header : headers) {
                setHeader(header);
            }
        }

        public Map<CharSequence, Set<Cookie>> getCookies() {
            return cookies;
        }

        public Set<Cookie> getCookie(String name) {
            return Optional.ofNullable(cookies).map(_map -> _map.get(name)).orElse(Collections.emptySet());
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

    /**
     * 无缓存, 订阅事件触发后才发起请求
     */
    public static class RxHttpResult {

        private final ResponseReceiver<?> responseReceiver;

        private RxHttpResult(ResponseReceiver<?> responseReceiver) {
            this.responseReceiver = responseReceiver;

        }

        public Mono<HttpClientResponse> response() {
            return responseReceiver.response().map(httpClientResponse1 -> httpClientResponse1);
        }

        public ByteBufFlux responseContent() {
            return responseReceiver.responseContent();
        }

        public <V> Mono<V> responseSingle(
            BiFunction<? super HttpClientResponse, ? super ByteBufMono, ? extends Mono<V>> receiver) {
            return responseReceiver.responseSingle(receiver);
        }

        public <V> Flux<V> response(
            BiFunction<? super HttpClientResponse, ? super ByteBufFlux, ? extends Publisher<V>> receiver) {
            return responseReceiver.response(receiver);
        }

        public <V> Flux<V> responseConnection(
            BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher<V>> receiver) {
            return responseReceiver.responseConnection(receiver);
        }

    }

    private void _assertState(boolean state, String message) {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

}
