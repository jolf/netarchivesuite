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
package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class UploadMessageTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public UploadMessageTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    public void tearDown() {
        rs.tearDown();
    }

    public void testInvalidArguments() {
        try {
            new UploadMessage(Channels.getTheBamon(),
                    Channels.getTheRepos(),
                    RemoteFileFactory.getInstance(null, true, false, true));
            fail("Should throw ArgumentNotValid on null file");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }
}