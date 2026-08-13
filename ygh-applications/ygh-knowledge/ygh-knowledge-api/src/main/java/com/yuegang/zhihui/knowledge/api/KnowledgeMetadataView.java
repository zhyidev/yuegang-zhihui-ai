package com.yuegang.zhihui.knowledge.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record KnowledgeMetadataView(String documentId, String issuingAuthority, LocalDate effectiveFrom,
                                    OffsetDateTime expiresAt, String region, String classification, String sourceName,
                                    List<String> tags, long metadataVersion) {
}
