/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.distribute;

import java.util.List;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.testutils.ReflectUtils;
import junit.framework.TestCase;

public class ReplicaClientFactoryTester extends TestCase {
    
    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();
    }

    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
    }
    
    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(ReplicaClientFactory.class);
    }
    
    public void testList() {
        List<ReplicaClient> clients = ReplicaClientFactory.getReplicaClients();
        
        for(ReplicaClient client : clients) {
            if(client instanceof ChecksumClient) {
                assertEquals("ChecksumClients must be of type " + ReplicaType.CHECKSUM,
                        ReplicaType.CHECKSUM, client.getType());
            } else if(client instanceof BitarchiveClient) {
                assertEquals("BitarchiveClients must be of type " + ReplicaType.BITARCHIVE,
                        ReplicaType.BITARCHIVE, client.getType());
            } else {
                fail("Unknown replica type: " + client.getType());
            }
        }
    }
}