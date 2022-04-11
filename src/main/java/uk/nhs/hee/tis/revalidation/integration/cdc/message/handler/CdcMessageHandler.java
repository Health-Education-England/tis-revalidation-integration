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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.handler;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcService;
import uk.nhs.hee.tis.revalidation.integration.message.MessageHandler;

import javax.naming.OperationNotSupportedException;

@Component
public class CdcMessageHandler<T extends ChangeStreamDocument> implements MessageHandler<T> {

  CdcService cdcService;

  public CdcMessageHandler(CdcService cdcService) {
    this.cdcService = cdcService;
  }

  @Override
  public void handleMessage(T message) throws OperationNotSupportedException {
    final OperationType operation = message.getOperationType();
    switch(operation) {
      case INSERT:
        cdcService.addNewEntity(message.getFullDocument());
        break;
      case UPDATE:
        cdcService.updateSubsetOfFields(message);
        break;
      default:
        throw new OperationNotSupportedException("CDC operation not supported: " + operation);
    }
  }
}