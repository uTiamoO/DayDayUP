package com.yuan.daydayup.reading.runtime.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.compiler.model.ActionRule;
import com.yuan.daydayup.reading.compiler.model.RequestSpec;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.runtime.engine.RuleExecutor;
import com.yuan.daydayup.reading.runtime.engine.ScriptExecutor;
import com.yuan.daydayup.reading.runtime.engine.TemplateRenderer;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.script.JsBridge;
import com.yuan.daydayup.reading.runtime.script.JsScriptEngine;
import com.yuan.daydayup.reading.runtime.service.NativeSourceReadingService;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import com.yuan.daydayup.reading.source.entity.SourceCompiledRule;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceCompiledRuleMapper;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParserRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 书源定向读取实现：加载编译产物 RuleModel → 请求构造（模板渲染 + 同域解析）
 * → 统一出站（{@link HttpFetcher}：SSRF + 限速）→ 抽取执行（{@link RuleExecutor}）。
 */
@Slf4j
@Service
public class SourceReadingServiceImpl implements SourceReadingService {

    /** 原生书源来源格式标记（SourceDefinition.originType）。 */
    private static final String ORIGIN_NATIVE = "native";

    private final SourceDefinitionMapper sourceMapper;
    private final SourceCompiledRuleMapper compiledMapper;
    private final HttpFetcher httpFetcher;
    private final RuleExecutor ruleExecutor;
    private final JsScriptEngine jsScriptEngine;
    private final ObjectMapper objectMapper;
    private final NativeSourceParserRegistry nativeParserRegistry;
    private final NativeSourceReadingService nativeSourceReadingService;

    public SourceReadingServiceImpl(SourceDefinitionMapper sourceMapper,
                                    SourceCompiledRuleMapper compiledMapper,
                                    HttpFetcher httpFetcher,
                                    RuleExecutor ruleExecutor,
                                    JsScriptEngine jsScriptEngine,
                                    ObjectMapper objectMapper,
                                    NativeSourceParserRegistry nativeParserRegistry,
                                    NativeSourceReadingService nativeSourceReadingService) {
        this.sourceMapper = sourceMapper;
        this.compiledMapper = compiledMapper;
        this.httpFetcher = httpFetcher;
        this.ruleExecutor = ruleExecutor;
        this.jsScriptEngine = jsScriptEngine;
        this.objectMapper = objectMapper;
        this.nativeParserRegistry = nativeParserRegistry;
        this.nativeSourceReadingService = nativeSourceReadingService;
    }

    /**
     * 若来源为原生书源（originType=native 且注册表命中），返回其解析器；否则空。
     * 命中则走自研 native 主线，未命中回落 legacy RuleModel。
     */
    private Optional<NativeParsed> nativeParserFor(Long sourceId) {
        SourceDefinition source = sourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "书源不存在: " + sourceId);
        }
        if (!ORIGIN_NATIVE.equalsIgnoreCase(source.getOriginType())) {
            return Optional.empty();
        }
        NativeSourceParser parser = nativeParserRegistry.find(source.getTags())
                .or(() -> nativeParserRegistry.findByBaseUrl(source.getBookSourceUrl()))
                .orElseThrow(() -> new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                        "原生书源无匹配解析器: " + sourceId + " (" + source.getBookSourceUrl() + ")"));
        return Optional.of(new NativeParsed(source, parser));
    }

    private record NativeParsed(SourceDefinition source, NativeSourceParser parser) {
    }

    @Override
    public DirectedReadVO search(Long sourceId, String keyword, int page) {
        if (!StringUtils.hasText(keyword)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "keyword 不能为空");
        }
        Optional<NativeParsed> nativeParsed = nativeParserFor(sourceId);
        if (nativeParsed.isPresent()) {
            NativeParsed np = nativeParsed.get();
            return nativeSourceReadingService.search(np.source(), np.parser(), keyword, page);
        }
        Loaded loaded = load(sourceId);
        ActionRule action = requireAction(loaded.model(), "search");
        RequestSpec spec = action.getRequest();
        if (spec == null || !StringUtils.hasText(spec.getUrlTemplate())) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                    "书源无可用 searchUrl: " + sourceId);
        }

        Charset cs = charsetOf(spec.getCharset());
        Map<String, String> vars = TemplateRenderer.vars(URLEncoder.encode(keyword, cs), Math.max(1, page));
        String url = resolveUrl(loaded.baseUrl(), TemplateRenderer.render(spec.getUrlTemplate(), vars, null));
        String body = TemplateRenderer.render(spec.getBody(), vars, null);

        long start = System.currentTimeMillis();
        String response = httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey(loaded.baseUrl())
                .baseUrl(loaded.baseUrl())
                .url(url)
                .method(spec.getMethod())
                .body(body)
                .charset(spec.getCharset())
                .headers(mergeHeaders(loaded.model(), spec))
                .concurrentRate(loaded.model().getHttp().getConcurrentRate())
                .build());

        DirectedReadVO vo = baseVo(loaded.source(), "search", url, start);
        ScriptExecutor js = scriptExecutor(loaded, Map.of("key", keyword, "page", Math.max(1, page)));
        vo.setRecords(execute(() -> ruleExecutor.extractList(action.getResponseType(), response, action, js)));
        vo.setElapsedMs(System.currentTimeMillis() - start);
        return vo;
    }

    @Override
    public DirectedReadVO detail(Long sourceId, String bookUrl) {
        if (!StringUtils.hasText(bookUrl)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "bookUrl 不能为空");
        }
        Optional<NativeParsed> nativeParsed = nativeParserFor(sourceId);
        if (nativeParsed.isPresent()) {
            NativeParsed np = nativeParsed.get();
            return nativeSourceReadingService.detail(np.source(), np.parser(), bookUrl);
        }
        Loaded loaded = load(sourceId);
        ActionRule action = requireAction(loaded.model(), "detail");
        String url = resolveUrl(loaded.baseUrl(), bookUrl);

        long start = System.currentTimeMillis();
        String response = httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey(loaded.baseUrl())
                .baseUrl(loaded.baseUrl())
                .url(url)
                .headers(mergeHeaders(loaded.model(), null))
                .concurrentRate(loaded.model().getHttp().getConcurrentRate())
                .build());

        DirectedReadVO vo = baseVo(loaded.source(), "detail", url, start);
        ScriptExecutor js = scriptExecutor(loaded, Map.of());
        vo.setRecord(execute(() -> ruleExecutor.extractObject(action.getResponseType(), response, action, js)));
        vo.setElapsedMs(System.currentTimeMillis() - start);
        return vo;
    }

    @Override
    public DirectedReadVO toc(Long sourceId, String tocUrl) {
        if (!StringUtils.hasText(tocUrl)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "tocUrl 不能为空");
        }
        Optional<NativeParsed> nativeParsed = nativeParserFor(sourceId);
        if (nativeParsed.isPresent()) {
            NativeParsed np = nativeParsed.get();
            return nativeSourceReadingService.toc(np.source(), np.parser(), tocUrl);
        }
        Loaded loaded = load(sourceId);
        ActionRule action = requireAction(loaded.model(), "toc");
        String url = resolveUrl(loaded.baseUrl(), tocUrl);

        long start = System.currentTimeMillis();
        String response = httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey(loaded.baseUrl())
                .baseUrl(loaded.baseUrl())
                .url(url)
                .headers(mergeHeaders(loaded.model(), null))
                .concurrentRate(loaded.model().getHttp().getConcurrentRate())
                .build());

        DirectedReadVO vo = baseVo(loaded.source(), "toc", url, start);
        ScriptExecutor js = scriptExecutor(loaded, Map.of());
        vo.setRecords(execute(() -> ruleExecutor.extractList(action.getResponseType(), response, action, js)));
        vo.setElapsedMs(System.currentTimeMillis() - start);
        return vo;
    }

    @Override
    public DirectedReadVO content(Long sourceId, String contentUrl) {
        if (!StringUtils.hasText(contentUrl)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "contentUrl 不能为空");
        }
        Optional<NativeParsed> nativeParsed = nativeParserFor(sourceId);
        if (nativeParsed.isPresent()) {
            NativeParsed np = nativeParsed.get();
            return nativeSourceReadingService.content(np.source(), np.parser(), contentUrl);
        }
        Loaded loaded = load(sourceId);
        ActionRule action = requireAction(loaded.model(), "content");
        String url = resolveUrl(loaded.baseUrl(), contentUrl);

        long start = System.currentTimeMillis();
        String response = httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey(loaded.baseUrl())
                .baseUrl(loaded.baseUrl())
                .url(url)
                .headers(mergeHeaders(loaded.model(), null))
                .concurrentRate(loaded.model().getHttp().getConcurrentRate())
                .build());

        DirectedReadVO vo = baseVo(loaded.source(), "content", url, start);
        ScriptExecutor js = scriptExecutor(loaded, Map.of());
        vo.setRecord(execute(() -> ruleExecutor.extractObject(action.getResponseType(), response, action, js)));
        vo.setElapsedMs(System.currentTimeMillis() - start);
        return vo;
    }

    // ── 装载与公共逻辑 ─────────────────────────────────────────────

    private record Loaded(SourceDefinition source, RuleModel model, String baseUrl) {
    }

    private Loaded load(Long sourceId) {
        SourceDefinition source = sourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "书源不存在: " + sourceId);
        }
        SourceCompiledRule compiled = compiledMapper.selectOne(
                new LambdaQueryWrapper<SourceCompiledRule>().eq(SourceCompiledRule::getSourceId, sourceId));
        if (compiled == null) {
            throw new BizException(ErrorCode.READING_RULE_COMPILE_FAILED,
                    "书源尚未编译，请先 POST /ops/sources/" + sourceId + "/compile");
        }
        if ("rejected".equalsIgnoreCase(compiled.getCompileStatus())) {
            throw new BizException(ErrorCode.READING_RULE_COMPILE_FAILED,
                    "书源编译等级为 rejected（依赖浏览器内核），不可运行: " + sourceId);
        }
        try {
            RuleModel model = objectMapper.readValue(compiled.getCompiledContent(), RuleModel.class);
            String baseUrl = model.getIdentity().getBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "书源缺少 baseUrl: " + sourceId);
            }
            return new Loaded(source, model, baseUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                    "编译产物反序列化失败: " + e.getMessage());
        }
    }

    private static ActionRule requireAction(RuleModel model, String name) {
        ActionRule action = model.getActions().get(name);
        if (action == null) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "书源未定义 " + name + " 规则");
        }
        return action;
    }

    /** 书源级 header 与动作级 header 合并（动作级优先） */
    private static Map<String, String> mergeHeaders(RuleModel model, RequestSpec spec) {
        Map<String, String> headers = new LinkedHashMap<>(model.getHttp().getHeaders());
        if (spec != null) {
            headers.putAll(spec.getHeaders());
        }
        return headers;
    }

    /**
     * 按请求上下文装配 ScriptStep 执行器：GraalJS + java 桥。
     * 桥内 {@code java.ajax/get} 强制回到 {@link HttpFetcher} 统一出口（SSRF + 限速不可绕行）。
     */
    private ScriptExecutor scriptExecutor(Loaded loaded, Map<String, Object> extraBindings) {
        Map<String, String> scriptVars = new LinkedHashMap<>();
        JsBridge bridge = new JsBridge(scriptVars, (url, headers) -> {
            Map<String, String> merged = mergeHeaders(loaded.model(), null);
            merged.putAll(headers);
            return httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                    .sourceKey(loaded.baseUrl())
                    .baseUrl(loaded.baseUrl())
                    .url(resolveUrl(loaded.baseUrl(), url))
                    .headers(merged)
                    .concurrentRate(loaded.model().getHttp().getConcurrentRate())
                    .build());
        });
        return (scriptBody, currentResult) -> {
            Map<String, Object> bindings = new LinkedHashMap<>(extraBindings);
            bindings.put("result", currentResult);
            bindings.put("baseUrl", loaded.baseUrl());
            return jsScriptEngine.eval(scriptBody, bindings, bridge);
        };
    }

    /** 相对 URL 按书源 baseUrl 解析为绝对（是否同域交由 SSRF 校验判定） */
    static String resolveUrl(String baseUrl, String url) {
        String u = url.strip();
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        return URI.create(baseUrl).resolve(u.startsWith("/") ? u : "/" + u).toString();
    }

    private static Charset charsetOf(String name) {
        try {
            return StringUtils.hasText(name) ? Charset.forName(name) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static DirectedReadVO baseVo(SourceDefinition source, String action, String url, long start) {
        DirectedReadVO vo = new DirectedReadVO();
        vo.setSourceId(source.getId());
        vo.setSourceName(source.getName());
        vo.setAction(action);
        vo.setRequestUrl(url);
        return vo;
    }

    /** 抽取阶段异常统一映射 60202（含 ScriptStep 未支持等） */
    private static <T> T execute(java.util.function.Supplier<T> extraction) {
        try {
            return extraction.get();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                    "规则执行失败: " + e.getMessage());
        }
    }
}
