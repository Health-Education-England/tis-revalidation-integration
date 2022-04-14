/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.LAST_UPDATED_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.RecommendationConstants.OUTCOME;

import java.util.Collections;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.util.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcRecommendationService;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcRecommendationFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;


@ExtendWith(MockitoExtension.class)
class CdcRecommendationServiceTest {
    @InjectMocks
    CdcRecommendationService cdcRecommendationService;

    @Mock
    MasterDoctorElasticSearchRepository repository;

    @Mock
    CdcRecommendationFieldUpdateHelper fieldUpdateHelper;

    private MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();

    @Test
    void shouldAddNewFields() {
        when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

        var newRecommendation =
                CdcTestDataGenerator.getRecommendationInsertChangeStreamDocument();
        cdcRecommendationService.addNewEntity(newRecommendation.getFullDocument());

        verify(repository).save(any());
    }

    @Test
    void shouldUpdateSubsetOfFields() {
        when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

        var changes =
                CdcTestDataGenerator.getRecommendationUpdateChangeStreamDocument();
        cdcRecommendationService.updateSubsetOfFields(changes);

        verify(fieldUpdateHelper)
                .updateField(
                        masterDoctorView, OUTCOME, changes.getUpdateDescription().getUpdatedFields()
                );
        verify(fieldUpdateHelper)
                .updateField(
                        masterDoctorView, LAST_UPDATED_DATE, changes.getUpdateDescription().getUpdatedFields()
                );
    }

    @Test
    void shouldNotInsertRecordIfDoctorDoesNotExist() {
        when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

        var newRecommendation =
                CdcTestDataGenerator.getRecommendationInsertChangeStreamDocument();
        cdcRecommendationService.addNewEntity(newRecommendation.getFullDocument());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotUpdateFieldsIfDoctorDoesNotExist() {
        when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

        var changes =
                CdcTestDataGenerator.getRecommendationUpdateChangeStreamDocument();
        cdcRecommendationService.updateSubsetOfFields(changes);

        verify(fieldUpdateHelper, never())
                .updateField(
                        masterDoctorView, OUTCOME, changes.getUpdateDescription().getUpdatedFields()
                );
        verify(fieldUpdateHelper, never())
                .updateField(
                        masterDoctorView, LAST_UPDATED_DATE, changes.getUpdateDescription().getUpdatedFields()
                );
    }
}
