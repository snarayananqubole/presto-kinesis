/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.kinesis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorRecordSetProvider;

import com.facebook.presto.spi.session.PropertyMetadata;
import com.facebook.presto.spi.transaction.IsolationLevel;
import static com.facebook.presto.spi.transaction.IsolationLevel.READ_COMMITTED;
import static com.facebook.presto.spi.transaction.IsolationLevel.checkConnectorSupports;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Kinesis connector implementation that includes a record set provider.
 *
 * The first 3 methods are mandatory, the remaining Connector methods have defaults.
 * Here a ConnectorRecordSetProvider is applicable.
 */
public class KinesisConnector
            implements Connector
{
    private final KinesisMetadata metadata;
    private final KinesisSplitManager splitManager;
    private final KinesisRecordSetProvider recordSetProvider;
    private final ArrayList<PropertyMetadata<?>> propertyList;

    @Inject
    public KinesisConnector(
            KinesisMetadata metadata,
            KinesisSplitManager splitManager,
            KinesisRecordSetProvider recordSetProvider)
    {
        this.metadata = checkNotNull(metadata, "metadata is null");
        this.splitManager = checkNotNull(splitManager, "splitManager is null");
        this.recordSetProvider = checkNotNull(recordSetProvider, "recordSetProvider is null");

        this.propertyList = new ArrayList<PropertyMetadata<?>>();
        buildPropertyList();
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle)
    {
        return metadata;
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean b)
    {
        checkConnectorSupports(READ_COMMITTED, isolationLevel);
        return KinesisTransactionHandle.INSTANCE;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return recordSetProvider;
    }

    /**
     * Return the session properties.
     *
     * @return the system properties for this connector
     */
    @Override
    public List<PropertyMetadata<?>> getSessionProperties()
    {
        return this.propertyList;
    }

    /** Build the list of session properties we support to supply them to Presto. */
    protected void buildPropertyList()
    {
        KinesisConnectorConfig cfg = this.metadata.getConnectorConfig();
        this.propertyList.add(PropertyMetadata.integerSessionProperty(
                SessionVariables.ITERATION_NUMBER, "checkpoint iteration number", cfg.getIterationNumber(), false));
        this.propertyList.add(PropertyMetadata.stringSessionProperty(
                SessionVariables.CHECKPOINT_LOGICAL_NAME, "checkpoint logical name", cfg.getLogicalProcessName(), false));
        this.propertyList.add(PropertyMetadata.integerSessionProperty(
                SessionVariables.MAX_BATCHES, "max number of calls to Kinesis per query", cfg.getMaxBatches(), false));
        this.propertyList.add(PropertyMetadata.integerSessionProperty(
                SessionVariables.BATCH_SIZE, "Record limit in calls to Kinesis", cfg.getBatchSize(), false));
    }
}
