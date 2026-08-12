package com.yuegang.zhihui.knowledge.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.knowledge.api.KnowledgeProcessingJobView;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KnowledgeProcessingJobService {
    private final JdbcTemplate jdbc;
    private final KnowledgeParseDispatcher dispatcher;

    public KnowledgeProcessingJobService(DataSource dataSource, KnowledgeParseDispatcher dispatcher) {
        jdbc = new JdbcTemplate(dataSource);
        this.dispatcher = dispatcher;
    }

    private static KnowledgeProcessingJobView view(long id, long document, String type, String status, int progress, int retries, String error, Timestamp time) {
        return new KnowledgeProcessingJobView(Long.toString(id), Long.toString(document), type, status, progress, retries, error, time.toInstant().atOffset(ZoneOffset.UTC));
    }

    private static long positive(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (Exception failure) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public List<KnowledgeProcessingJobView> list(String document, String status, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id,document_id,task_type,status,progress,retry_count,last_error,updated_at FROM knowledge_processing_job WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (document != null && !document.isBlank()) {
            sql.append(" AND document_id=?");
            args.add(positive(document));
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status=?");
            args.add(status.strip().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY updated_at DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 100)));
        return jdbc.query(sql.toString(), (r, n) -> view(r.getLong(1), r.getLong(2), r.getString(3), r.getString(4), r.getInt(5), r.getInt(6), r.getString(7), r.getTimestamp(8)), args.toArray());
    }

    public KnowledgeProcessingJobView retry(String id) {
        long job = positive(id);
        if (jdbc.update("UPDATE knowledge_processing_job SET status='RETRY',progress=0,retry_count=0,next_retry_at=NOW(6),last_error=NULL WHERE id=? AND status='FAILED'", job) != 1)
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
        dispatcher.process(job);
        return jdbc.query("SELECT id,document_id,task_type,status,progress,retry_count,last_error,updated_at FROM knowledge_processing_job WHERE id=?", r -> {
            if (!r.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            return view(r.getLong(1), r.getLong(2), r.getString(3), r.getString(4), r.getInt(5), r.getInt(6), r.getString(7), r.getTimestamp(8));
        }, job);
    }
}
