/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import junit.framework.TestCase;

/** Tests of the ReplicaType enum class. */
public class ReplicaTypeTester extends TestCase {

    public void testFromOrdinal() {
        assertEquals(ReplicaType.BITARCHIVE, ReplicaType.fromOrdinal(1));
        assertEquals(ReplicaType.CHECKSUM, ReplicaType.fromOrdinal(2));
        try {
            ReplicaType.fromOrdinal(2);
            fail("Should throw ArgumentNotValid. The old NO_REPLICA_TYPE which used this value has been removed");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        try {
            ReplicaType.fromOrdinal(3);
            fail("Should throw ArgumentNotValid. Has ReplicaType been changed");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
     
    public void testFromSetting() {
        try {
            ReplicaType.fromSetting(null);
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        assertEquals(ReplicaType.BITARCHIVE, 
                ReplicaType.fromSetting(ReplicaType.BITARCHIVE_REPLICATYPE_AS_STRING));
        assertEquals(ReplicaType.CHECKSUM, 
                ReplicaType.fromSetting(ReplicaType.CHECKSUM_REPLICATYPE_AS_STRING));
        try {
            ReplicaType.fromSetting("");

            fail("Should throw ArgumentNotValid. This is not a valid replica type");
        } catch (ArgumentNotValid e) {
            // Expected
        }try {
            ReplicaType.fromSetting(null);

            fail("Should throw ArgumentNotValid. This is not a valid replica type");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
    }
}