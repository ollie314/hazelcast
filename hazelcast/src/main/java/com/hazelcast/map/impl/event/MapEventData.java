/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.map.impl.event;

import com.hazelcast.nio.Address;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.impl.BinaryInterface;

import java.io.IOException;

/**
 * Map wide event's data.
 */
@BinaryInterface
public class MapEventData extends AbstractEventData {

    protected int numberOfEntries;

    public MapEventData() {
    }

    public MapEventData(String source, String mapName, Address caller, int eventType, int numberOfEntries) {
        super(source, mapName, caller, eventType);
        this.numberOfEntries = numberOfEntries;
    }

    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        super.writeData(out);
        out.writeInt(numberOfEntries);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        super.readData(in);
        numberOfEntries = in.readInt();
    }

    @Override
    public String toString() {
        return "MapEventData{"
                + super.toString()
                + ", numberOfEntries=" + numberOfEntries
                + '}';
    }
}
