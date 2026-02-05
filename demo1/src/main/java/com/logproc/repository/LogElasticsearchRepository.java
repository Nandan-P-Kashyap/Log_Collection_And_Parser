package com.logproc.repository;

import com.logproc.model.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogElasticsearchRepository extends ElasticsearchRepository<LogDocument, String> {
}
