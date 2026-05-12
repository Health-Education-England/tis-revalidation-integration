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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * A service class that updates hidden discrepancy fields.
 */
@Slf4j
@Service
public class CdcHiddenDiscrepancyService extends CdcService<HiddenDiscrepancy> {

  private final ElasticsearchOperations elasticsearchOperations;

  /**
   * Service responsible for updating the hidden discrepancy nested fields used for searching.
   */
  public CdcHiddenDiscrepancyService(
      MasterDoctorElasticSearchRepository repository,
      ElasticsearchOperations elasticsearchOperations
  ) {
    super(repository);
    this.elasticsearchOperations = elasticsearchOperations;
  }

  /**
   * Add new hidden discrepancy to index (this is an aggregation, updating an existing record).
   *
   * @param entity hidden discrepancy to add to index
   */
  @Override
  public void upsertEntity(HiddenDiscrepancy entity) {
    String gmcId = entity.getGmcId();
    final var repository = getRepository();

    try {
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

        hiddenDiscrepancies.add(entity);
        masterDoctorView = repository.findByGmcReferenceNumber(gmcId).get(0);
        masterDoctorView.setHiddenDiscrepancies(hiddenDiscrepancies);

        repository.save(masterDoctorView);
      }
    } catch (Exception e) {
      log.error("CDC error adding hidden discrepancy: {}, exception: {}", entity, e.getMessage(),
          e);
      throw e;
    }
  }

  @Override
  public void deleteEntity(String key) {
    String gmcId = findGmcIdAssociatedWithHiddenDiscrepancy(key);
    final var repository = getRepository();

    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        MasterDoctorView masterDoctorView = handleDuplicateRecords(masterDoctorViewList);

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

    } catch (Exception e) {
      log.error("CDC error removing hidden discrepancy: {}, exception: {}", key, e.getMessage(),
          e);
      throw e;
    }
  }

  private String findGmcIdAssociatedWithHiddenDiscrepancy(String key) {
    BoolQueryBuilder rootQuery = boolQuery();
    rootQuery.must(boolQuery().filter(nestedQuery("hiddenDiscrepancies", boolQuery()
        .must(matchQuery("hiddenDiscrepancies.id.keyword", key)), None)));
    NativeSearchQuery searchQueryEsResult = new NativeSearchQueryBuilder()
        .withQuery(rootQuery)
        .build();
    var result = elasticsearchOperations.search(searchQueryEsResult, MasterDoctorView.class)
        .getSearchHits()
        .stream()
        .findFirst();
    if (result.isPresent()) {
      return result.get().getContent().getGmcReferenceNumber();
    } else {
      throw new RuntimeException(
          String.format("No hidden discrepancy found to delete with id: %s", key));
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
