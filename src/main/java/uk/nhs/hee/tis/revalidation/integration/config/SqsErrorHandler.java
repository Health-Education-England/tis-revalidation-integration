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

package uk.nhs.hee.tis.revalidation.integration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.util.ErrorHandler;

/**
 * Custom error handler for SQS message listeners.
 * Logs exceptions with full context before they cause messages to be sent to the DLQ.
 *
 * <p>This handler ensures that:
 * <ul>
 *   <li>All exceptions are logged with complete stack traces</li>
 *   <li>Message content is preserved in logs for debugging</li>
 *   <li>Exceptions are rethrown to trigger SQS retry mechanism</li>
 *   <li>After max retries, messages are sent to the configured DLQ</li>
 * </ul>
 */
@Slf4j
public class SqsErrorHandler implements ErrorHandler {

  /**
   * Handles errors from SQS message processing.
   * Logs the exception with full context and rethrows it to ensure proper SQS behavior.
   *
   * @param t the throwable that occurred during message processing
   * @throws RuntimeException always rethrows to prevent message acknowledgment
   */
  @Override
  public void handleError(Throwable t) {
    log.error("Exception in SQS message listener. Message will be retried and eventually sent to DLQ if retries are exhausted.", t);

    // Log additional context if available
    logExceptionDetails(t);

    // Rethrow the exception to ensure SQS does NOT acknowledge the message
    // This allows SQS to retry the message and eventually send it to the DLQ
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else {
      throw new RuntimeException("SQS message processing failed", t);
    }
  }

  /**
   * Handles errors with access to the Spring Message object.
   * This method provides additional context from the message headers and payload.
   *
   * @param message the Spring message that failed processing
   * @param t the throwable that occurred
   * @throws RuntimeException always rethrows to prevent message acknowledgment
   */
  public void handleError(Message<?> message, Throwable t) {
    log.error("Exception processing SQS message. Message ID: {}, Payload: {}",
        message.getHeaders().getId(),
        message.getPayload(),
        t);

    // Log message headers for additional debugging context
    log.debug("Message headers: {}", message.getHeaders());

    // Rethrow to prevent acknowledgment
    handleError(t);
  }

  /**
   * Logs detailed information about the exception for troubleshooting.
   *
   * @param t the throwable to log details for
   */
  private void logExceptionDetails(Throwable t) {
    // Log the root cause if different from the top-level exception
    Throwable rootCause = getRootCause(t);
    if (rootCause != t) {
      log.error("Root cause: {}", rootCause.getMessage());
    }

    // Log exception class for filtering/alerting
    log.error("Exception type: {}", t.getClass().getName());
  }

  /**
   * Gets the root cause of an exception by traversing the cause chain.
   *
   * @param t the throwable to find the root cause of
   * @return the root cause throwable
   */
  private Throwable getRootCause(Throwable t) {
    Throwable cause = t;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause;
  }
}

