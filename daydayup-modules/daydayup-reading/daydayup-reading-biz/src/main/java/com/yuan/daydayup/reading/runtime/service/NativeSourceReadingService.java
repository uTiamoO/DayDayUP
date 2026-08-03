package com.yuan.daydayup.reading.runtime.service;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;

/**
 * 原生书源定向读取服务：统一出站（HttpFetcher）+ 挑战检测 + native parser 解析。
 *
 * <p>与 {@link SourceReadingService} 的 legacy RuleModel 路径产出同构的 {@link DirectedReadVO}，
 * 以复用下游 {@code ContentDiscovery}/{@code ChapterSync}/{@code ContentFetch} 装配链路。</p>
 */
public interface NativeSourceReadingService {

    DirectedReadVO search(SourceDefinition source, NativeSourceParser parser, String keyword, int page);

    DirectedReadVO detail(SourceDefinition source, NativeSourceParser parser, String bookUrl);

    DirectedReadVO toc(SourceDefinition source, NativeSourceParser parser, String tocUrl);

    DirectedReadVO content(SourceDefinition source, NativeSourceParser parser, String contentUrl);
}
