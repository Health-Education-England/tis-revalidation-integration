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

package uk.nhs.hee.tis.revalidation.integration.cdc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RecommendationConstants {
  public static final String RECOMMENDATION_ID = "id";
  public static final String GMC_NUMBER = "gmcNumber";
  public static final String OUTCOME = "outcome";
  public static final String RECOMMENDATION_TYPE = "recommendationType";
  public static final String RECOMMENDATION_STATUS = "recommendationStatus";
  public static final String GMC_SUBMISSION_DATE = "gmcSubmissionDate";
  public static final String ACTUAL_SUBMISSION_DATE = "actualSubmissionDate";
  public static final String GMC_REVALIDATION_ID = "gmcRevalidationId";
  public static final String DEFERRAL_DATE = "deferralDate";
  public static final String DEFERRAL_REASON = "deferralReason";
  public static final String DEFERRAL_SUB_REASON = "deferralSubReason";
  public static final String COMMENTS = "comments";
  public static final String RECOMMENDATION_ADMIN = "admin";
}
