/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.operation;

import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.EntryView;
import com.hazelcast.map.impl.EntryViews;
import com.hazelcast.map.impl.MapEventPublisher;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.map.impl.MapServiceContext;
import com.hazelcast.map.impl.NearCacheProvider;
import com.hazelcast.map.impl.RecordStore;
import com.hazelcast.map.impl.record.Record;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.BackupAwareOperation;
import com.hazelcast.spi.impl.MutatingOperation;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionAwareOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Puts records to map which are loaded from map store by {@link com.hazelcast.core.IMap#loadAll}
 */
public class PutFromLoadAllOperation extends AbstractMapOperation implements PartitionAwareOperation, MutatingOperation,
        BackupAwareOperation {

    private List<Data> keyValueSequence;

    public PutFromLoadAllOperation() {
        keyValueSequence = Collections.emptyList();
    }

    public PutFromLoadAllOperation(String name, List<Data> keyValueSequence) {
        super(name);
        this.keyValueSequence = keyValueSequence;
    }

    @Override
    public void run() throws Exception {
        final List<Data> keyValueSequence = this.keyValueSequence;
        if (keyValueSequence == null || keyValueSequence.isEmpty()) {
            return;
        }
        final int partitionId = getPartitionId();
        final MapService mapService = this.mapService;
        MapServiceContext mapServiceContext = mapService.getMapServiceContext();
        final RecordStore recordStore = mapServiceContext.getRecordStore(partitionId, name);
        for (int i = 0; i < keyValueSequence.size(); i += 2) {
            final Data key = keyValueSequence.get(i);
            final Data dataValue = keyValueSequence.get(i + 1);
            // here object conversion is for interceptors.
            final Object objectValue = mapServiceContext.toObject(dataValue);
            final Object previousValue = recordStore.putFromLoad(key, objectValue);

            callAfterPutInterceptors(objectValue);
            publishEntryEvent(key, mapServiceContext.toData(previousValue), dataValue);
            publishWanReplicationEvent(key, dataValue, recordStore.getRecord(key));
        }
    }

    private void callAfterPutInterceptors(Object value) {
        mapService.getMapServiceContext().interceptAfterPut(name, value);
    }

    private void publishEntryEvent(Data key, Data previousValue, Data newValue) {
        final EntryEventType eventType = previousValue == null ? EntryEventType.ADDED : EntryEventType.UPDATED;
        final MapServiceContext mapServiceContext = mapService.getMapServiceContext();
        final MapEventPublisher mapEventPublisher = mapServiceContext.getMapEventPublisher();
        mapEventPublisher.publishEvent(getCallerAddress(), name, eventType, key, previousValue, newValue);
    }

    private void publishWanReplicationEvent(Data key, Data value, Record record) {
        if (record == null) {
            return;
        }

        if (mapContainer.getWanReplicationPublisher() != null && mapContainer.getWanMergePolicy() != null) {
            final EntryView entryView = EntryViews.createSimpleEntryView(key, value, record);
            MapServiceContext mapServiceContext = mapService.getMapServiceContext();
            MapEventPublisher mapEventPublisher = mapServiceContext.getMapEventPublisher();
            mapEventPublisher.publishWanReplicationUpdate(name, entryView);
        }
    }

    @Override
    public void afterRun() throws Exception {
        final List<Data> keyValueSequence = this.keyValueSequence;
        if (keyValueSequence == null || keyValueSequence.isEmpty()) {
            return;
        }
        final int size = keyValueSequence.size();
        final List<Data> dataKeys = new ArrayList<Data>(size / 2);
        for (int i = 0; i < size; i += 2) {
            final Data key = keyValueSequence.get(i);
            dataKeys.add(key);
        }
        NearCacheProvider nearCacheProvider = mapService.getMapServiceContext().getNearCacheProvider();
        nearCacheProvider.invalidateNearCache(name, dataKeys);
    }

    @Override
    public Object getResponse() {
        return true;
    }

    @Override
    public boolean shouldBackup() {
        return !keyValueSequence.isEmpty();
    }

    @Override
    public final int getAsyncBackupCount() {
        return mapContainer.getAsyncBackupCount();
    }

    @Override
    public final int getSyncBackupCount() {
        return mapContainer.getBackupCount();
    }

    @Override
    public Operation getBackupOperation() {
        return new PutFromLoadAllBackupOperation(name, keyValueSequence);
    }

    @Override
    public String toString() {
        return "PutFromLoadAllOperation{}";
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        final List<Data> keyValueSequence = this.keyValueSequence;
        final int size = keyValueSequence.size();
        out.writeInt(size);
        for (Data data : keyValueSequence) {
            out.writeData(data);
        }
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        final int size = in.readInt();
        if (size < 1) {
            keyValueSequence = Collections.emptyList();
        } else {
            final List<Data> tmpKeyValueSequence = new ArrayList<Data>(size);
            for (int i = 0; i < size; i++) {
                final Data data = in.readData();
                tmpKeyValueSequence.add(data);
            }
            keyValueSequence = tmpKeyValueSequence;
        }
    }
}
