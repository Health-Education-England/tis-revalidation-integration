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

package uk.nhs.hee.tis.revalidation.integration.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to log exceptions from SQS message listeners with full context.
 * This allows exceptions to propagate naturally while ensuring detailed error logging.
 */
@Slf4j
@Aspect
@Component
public class SqsListenerLoggingAspect {

  /**
   * Intercepts all @SqsListener methods to log exceptions with the message content.
   * The exception is rethrown to ensure proper SQS retry and DLQ behavior.
   *
   * @param joinPoint the intercepted method
   * @return the result of the method execution
   * @throws Throwable any exception from the listener method (rethrown after logging)
   */
  @Around("@annotation(io.awspring.cloud.messaging.listener.annotation.SqsListener)")
  public Object logSqsListenerExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();
    String message = args.length > 0 ? String.valueOf(args[0]) : "No message content";

    try {
      return joinPoint.proceed();
    } catch (Exception e) {
      // Log the exception with full context
      log.error("Exception in SQS listener method '{}'. Message content: {}",
          methodName, message, e);

      // Rethrow to ensure SQS handles the failure (retry + DLQ)
      throw e;
    }
  }
}

