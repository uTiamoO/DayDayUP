package com.yuan.daydayup.reading.compiler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.CompileBatchReportVO;
import com.yuan.daydayup.reading.api.vo.CompileReportVO;
import com.yuan.daydayup.reading.compiler.RuleCompiler;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.service.RuleCompileService;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import com.yuan.daydayup.reading.source.entity.SourceCompiledRule;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceCompiledRuleMapper;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 书源编译服务实现。
 *
 * <p>编译产物按 {@code sourceId} 幂等 upsert（保留最新一版）；同时回写
 * {@code SourceDefinition.compileGrade}。单源失败隔离，不中断批量。</p>
 */
@Slf4j
@Service
public class RuleCompileServiceImpl implements RuleCompileService {

    private final SourceDefinitionMapper sourceMapper;
    private final SourceCompiledRuleMapper compiledMapper;
    private final RuleCompiler compiler;
    private final ObjectMapper objectMapper;
    private final ReadingMetrics metrics;

    public RuleCompileServiceImpl(SourceDefinitionMapper sourceMapper,
                                  SourceCompiledRuleMapper compiledMapper,
                                  RuleCompiler compiler,
                                  ObjectMapper objectMapper,
                                  ReadingMetrics metrics) {
        this.sourceMapper = sourceMapper;
        this.compiledMapper = compiledMapper;
        this.compiler = compiler;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    public CompileReportVO compileOne(Long sourceId) {
        SourceDefinition source = sourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "书源不存在: " + sourceId);
        }
        return doCompile(source);
    }

    @Override
    public CompileBatchReportVO compileAllEnabled() {
        List<SourceDefinition> sources = sourceMapper.selectList(
                new LambdaQueryWrapper<SourceDefinition>().eq(SourceDefinition::getStatus, 1));
        CompileBatchReportVO batch = new CompileBatchReportVO();
        batch.setTotal(sources.size());
        for (SourceDefinition source : sources) {
            try {
                CompileReportVO report = doCompile(source);
                batch.getDetails().add(report);
                switch (CompileGrade.valueOf(report.getGrade().toUpperCase())) {
                    case FULL -> batch.setFull(batch.getFull() + 1);
                    case DEGRADED -> batch.setDegraded(batch.getDegraded() + 1);
                    case REJECTED -> batch.setRejected(batch.getRejected() + 1);
                }
            } catch (Exception e) {
                batch.setFailed(batch.getFailed() + 1);
                metrics.recordCompile("failed");
                log.warn("[rule-compile] 书源编译失败 id={}", source.getId(), e);
            }
        }
        log.info("[rule-compile] 覆盖率 total={} full={} degraded={} rejected={} failed={} 覆盖率={}%",
                batch.getTotal(), batch.getFull(), batch.getDegraded(), batch.getRejected(),
                batch.getFailed(), batch.coveragePercent());
        return batch;
    }

    private CompileReportVO doCompile(SourceDefinition source) {
        try {
            JsonNode legado = objectMapper.readTree(source.getRawContent());
            RuleModel model = compiler.compile(legado);
            RuleModel.Health health = model.getHealth();
            String grade = health.getGrade().name().toLowerCase();

            SourceCompiledRule compiled = upsertCompiled(source, model, health, grade);
            metrics.recordCompile(grade);

            // 回写书源编译等级
            source.setCompileGrade(grade);
            sourceMapper.updateById(source);

            CompileReportVO report = new CompileReportVO();
            report.setSourceId(source.getId());
            report.setName(source.getName());
            report.setGrade(grade);
            report.setActionCount(model.getActions().size());
            report.setWarnings(health.getWarnings());
            report.setScriptDeps(health.getScriptDeps());
            report.setWebviewDeps(health.getWebviewDeps());
            log.debug("[rule-compile] id={} name={} grade={} actions={}",
                    source.getId(), source.getName(), grade, model.getActions().size());
            return report;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "编译失败: " + e.getMessage());
        }
    }

    private SourceCompiledRule upsertCompiled(SourceDefinition source, RuleModel model,
                                              RuleModel.Health health, String grade) throws Exception {
        String content = objectMapper.writeValueAsString(model);
        String warnings = objectMapper.writeValueAsString(health.getWarnings());
        String scriptDeps = objectMapper.writeValueAsString(health.getScriptDeps());

        SourceCompiledRule existing = compiledMapper.selectOne(
                new LambdaQueryWrapper<SourceCompiledRule>().eq(SourceCompiledRule::getSourceId, source.getId()));
        SourceCompiledRule entity = existing != null ? existing : new SourceCompiledRule();
        entity.setSourceId(source.getId());
        entity.setCompilerVersion(RuleCompiler.COMPILER_VERSION);
        entity.setDslSchemaVersion(model.getDslVersion());
        entity.setCompileStatus(grade);
        entity.setCompileWarnings(warnings);
        entity.setScriptDeps(scriptDeps);
        entity.setCompiledContent(content);
        entity.setCompiledAt(LocalDateTime.now());

        if (existing != null) {
            compiledMapper.updateById(entity);
        } else {
            compiledMapper.insert(entity);
        }
        return entity;
    }
}
