/**
 * Copyright 2016 Matthias Düsterhöft
 *
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
package de.matdue.isk.eve.crest;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.HashMap;

import static org.testng.Assert.*;

public class DownloadTypeTaskTests {

    @Test
    public void queryServer_type34_returnsData() {
        // Arrange
        HashMap<String, Type> result = new HashMap<>();
        DownloadTypeTask task = new DownloadTypeTask("34", result);

        // Act
        task.run();

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("34"));
        Type type = result.get("34");
        assertNotNull(type);
        assertEquals("34", type.id);
        assertEquals("Tritanium", type.name);
    }

    @Test
    public void queryServer_unknownType_returnsFailure() {
        // Arrange
        final MutableBoolean processCalled = new MutableBoolean();
        HashMap<String, Type> result = new HashMap<>();
        DownloadTypeTask task = new DownloadTypeTask("999999", result) {
            @Override
            protected void processFailure(int responseCode, String responseMessage, InputStream errorStream) {
                processCalled.setTrue();
            }
        };

        // Act
        task.run();

        // Assert
        assertEquals(0, result.size());
        assertTrue(processCalled.booleanValue());
    }

}
