/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.cdc.service;


import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcHiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.mapper.CdcHiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * A service class that updates hidden discrepancy fields.
 */
@Slf4j
@Service
public class CdcHiddenDiscrepancyService extends CdcService<CdcHiddenDiscrepancyDto> {

  private final ElasticsearchOperations elasticsearchOperations;
  private final CdcHiddenDiscrepancyMapper cdcHiddenDiscrepancyMapper;

  /**
   * Service responsible for updating the hidden discrepancy nested fields used for searching.
   */
  public CdcHiddenDiscrepancyService(
      MasterDoctorElasticSearchRepository repository,
      ElasticsearchOperations elasticsearchOperations,
      CdcHiddenDiscrepancyMapper cdcHiddenDiscrepancyMapper
  ) {
    super(repository);
    this.elasticsearchOperations = elasticsearchOperations;
    this.cdcHiddenDiscrepancyMapper = cdcHiddenDiscrepancyMapper;
  }

  /**
   * Add new hidden discrepancy to index (this is an aggregation, updating an existing record).
   *
   * @param entity hidden discrepancy to add to index
   */
  @Override
  public void upsertEntity(CdcHiddenDiscrepancyDto entity) {
    String gmcId = entity.getGmcId();
    final var repository = getRepository();

    List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
    if (!masterDoctorViewList.isEmpty()) {
      MasterDoctorView masterDoctorView = handleDuplicateRecords(masterDoctorViewList);

      List<HiddenDiscrepancy> hiddenDiscrepancies = new ArrayList<>();

      if (masterDoctorView.getHiddenDiscrepancies() != null) {
        hiddenDiscrepancies.addAll(masterDoctorView.getHiddenDiscrepancies());
      }

      boolean alreadyHidden = hiddenDiscrepancies.stream().anyMatch(h ->
          h.getHiddenForDesignatedBodyCode().equals(entity.getHiddenForDesignatedBodyCode()));

      if (alreadyHidden) {
        log.info(
            "gmcReferenceNumber: {} already has a hidden discrepancy for designated body: {}",
            entity.getGmcId(), entity.getHiddenForDesignatedBodyCode());
        return;
      }

      hiddenDiscrepancies.add(cdcHiddenDiscrepancyMapper.toEntity(entity));
      masterDoctorView = repository.findByGmcReferenceNumber(gmcId).get(0);
      masterDoctorView.setHiddenDiscrepancies(hiddenDiscrepancies);

      repository.save(masterDoctorView);
    }
  }

  @Override
  public void deleteEntity(String key) {
    MasterDoctorView masterDoctorView = findRecordAssociatedWithHiddenDiscrepancy(key);
    String gmcId = masterDoctorView.getGmcReferenceNumber();
    final var repository = getRepository();

    List<HiddenDiscrepancy> updatedList = new ArrayList<>();
    if (masterDoctorView.getHiddenDiscrepancies() != null) {
      updatedList = masterDoctorView.getHiddenDiscrepancies().stream()
          .filter(h -> !h.getId()
              .equals(key)).toList();
    }

    masterDoctorView = repository.findByGmcReferenceNumber(gmcId).get(0);
    masterDoctorView.setHiddenDiscrepancies(updatedList);

    repository.save(masterDoctorView);
  }

  private MasterDoctorView findRecordAssociatedWithHiddenDiscrepancy(String key) {
    BoolQueryBuilder rootQuery = boolQuery();
    rootQuery.must(boolQuery().filter(nestedQuery("hiddenDiscrepancies", boolQuery()
        .must(matchQuery("hiddenDiscrepancies.id.keyword", key)), None)));
    NativeSearchQuery searchQueryEsResult = new NativeSearchQueryBuilder()
        .withQuery(rootQuery)
        .build();
    List<MasterDoctorView> result = elasticsearchOperations.search(searchQueryEsResult,
            MasterDoctorView.class)
        .getSearchHits()
        .stream()
        .map(SearchHit::getContent)
        .toList();
    if (!result.isEmpty()) {
      return handleDuplicateRecords(result);
    } else {
      throw new ResourceNotFoundException(
          String.format("No elasticsearch record found to delete hidden discrepancy with id: %s",
              key));
    }
  }

  private MasterDoctorView handleDuplicateRecords(List<MasterDoctorView> masterDoctorViewList) {
    if (masterDoctorViewList.size() > 1) {
      log.error("Multiple doctors assigned to the same GMC number: {}",
          masterDoctorViewList.get(0).getGmcReferenceNumber());
    }
    return masterDoctorViewList.get(0);
  }
}
