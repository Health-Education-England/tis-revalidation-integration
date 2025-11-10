/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.config;

/**
 * A utility class that defines constants related to Elasticsearch.
 */
public final class EsConstant {

  /**
   * Private constructor to prevent instantiation.
   */
  private EsConstant() {
    throw new IllegalStateException("Cannot instantiate EsConstant");
  }

  /**
   * Contains constant names for Elasticsearch index definitions.
   */
  public static final class Indexes {
    /**
     * Private constructor to prevent instantiation.
     */
    private Indexes() {
      throw new IllegalStateException("Cannot instantiate Indexes");
    }

    public static final String MASTER_DOCTOR_INDEX = "masterdoctorindex";
    public static final String RECOMMENDATION_INDEX = "recommendationindex";
  }

  /**
   * Contains constant names for Elasticsearch alias definitions.
   */
  public static final class Aliases {
    /**
     * Private constructor to prevent instantiation.
     */
    private Aliases() {
      throw new IllegalStateException("Cannot instantiate Aliases");
    }

    public static final String DISCREPANCIES_ALIAS = "discrepancies";
    public static final String CURRENT_CONNECTIONS_ALIAS = "current_connections";
  }
}
