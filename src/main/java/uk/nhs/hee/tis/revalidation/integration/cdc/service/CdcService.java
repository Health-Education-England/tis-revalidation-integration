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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
public abstract class CdcService<T> {

  private MasterDoctorElasticSearchRepository repository;

  private CdcMessagePublisher cdcMessagePublisher;

  protected CdcService(
      MasterDoctorElasticSearchRepository repository,
      CdcMessagePublisher cdcMessagePublisher
  ) {
    this.repository = repository;
    this.cdcMessagePublisher = cdcMessagePublisher;
  }

  protected MasterDoctorElasticSearchRepository getRepository() {
    return this.repository;
  }

  public abstract void upsertEntity(T entity);

  /**
   * Publish MasterDoctorView update using injected CdcMessagePublisher.
   *
   * @param masterDoctorView the updated MasterDoctorView to be published
   */
  public final void publishUpdate(MasterDoctorView masterDoctorView) {
    cdcMessagePublisher.publishCdcUpdate(masterDoctorView);
  }
}
