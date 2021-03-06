/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests JMSConnection, the class that handles all JMS operations for Netarkivet.
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused", "serial"})
public class JMSConnectionTester {
    private SecurityManager originalSecurityManager;

    ReloadSettings rs = new ReloadSettings();
    PreventSystemExit pse = new PreventSystemExit();
    MockupJMS mj = new MockupJMS();

    @Before
    public void setUp() {
        rs.setUp();
        pse.setUp();
        mj.setUp();
    }

    @After
    public void tearDown() {
        mj.tearDown();
        pse.tearDown();
        rs.tearDown();
    }

    /**
     * Test that asking for a fake JMSConnection actually gets you just that.
     */
    @Test
    public void testFakeJMSConnection() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        assertTrue("Fake JMS connection must be of type JMSConnectionMockupMQ",
                JMSConnectionFactory.getInstance() instanceof JMSConnectionMockupMQ);
    }

    /**
     * Tests for null parameters.
     */
    @Test
    public void testUnpackParameterIsNull() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        try {
            JMSConnection.unpack(null);
            fail("Should throw an ArgumentNotValidException when given a " + "null parameter");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests for wrong parameters.
     */
    @Test
    public void testUnpackParameterIsAnObjectMessage() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        try {
            JMSConnection.unpack(new DummyMapMessage());
            fail("Should throw an ArgumentNotValid exception when given a " + "wrong message type");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests for correct error handling if ObjectMessage has the wrong payload.
     */
    @Test
    public void testUnpackInvalidPayload() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        try {
            JMSConnection.unpack(new JMSConnectionMockupMQ.TestObjectMessage(new DummySerializableClass()));
            fail("Should throw an ArgumentNotValidException when given a " + "wrong payload");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests if correct payload is unwrapped.
     */
    @Test
    public void testUnpackOfCorrectPayload() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        String testID = "42";
        TestMessage testMessage = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), testID);
        JMSConnectionMockupMQ.updateMsgID(testMessage, "ID89");
        TestMessage msg = (TestMessage) JMSConnection.unpack(new JMSConnectionMockupMQ.TestObjectMessage(testMessage));
        assertEquals("Unpacking should have given correct ID", msg.testID, testID);
    }

    /**
     * Test resend() methods arguments.
     */
    @Test
    public void testResendArgumentsNotNull() {
        /*
         * Check it is the correct resend method which is invoked and not an overloaded version in fx.
         * JMSConnectionMockupMQ. Resend should be declared final.
         */
        Class parameterTypes[] = {NetarkivetMessage.class, ChannelID.class};
        assertMethodIsFinal(JMSConnection.class, "resend", parameterTypes);

        /*
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");

        /*
         * Test if ArgumentNotValid is thrown if null is given as first parameter.
         */
        try {
            JMSConnectionFactory.getInstance().resend(null, Channels.getError());
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /*
         * Test if ArgumentNotValid is thrown if null is given as second parameter
         */
        try {
            JMSConnectionFactory.getInstance().resend(new TestMessage(Channels.getError(), Channels.getError(), ""),
                    null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Test the resend method. Message shouldn't be sent according to the address specified in the "to" field of the
     * message. It should be sent to the address given in the "to" parameter of the resend() method.
     */
    @Test
    public void testResendCorrectSendBehaviour() {
        /**
         * Check it is the correct resend method which is invoked and not an overloaded version in fx.
         * JMSConnectionMockupMQ. Resend should be declared final.
         */
        Class parameterTypes[] = {NetarkivetMessage.class, ChannelID.class};
        assertMethodIsFinal(JMSConnection.class, "resend", parameterTypes);

        /**
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();

        // Create dummy server and listen on the Error queue.
        DummyServer serverErrorQueue = new DummyServer();
        serverErrorQueue.reset();
        con.setListener(Channels.getError(), serverErrorQueue);

        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheArcreposQueue = new DummyServer();
        serverTheArcreposQueue.reset();
        con.setListener(Channels.getTheRepos(), serverTheArcreposQueue);

        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheBamonQueue = new DummyServer();
        serverTheBamonQueue.reset();
        con.setListener(Channels.getTheBamon(), serverTheBamonQueue);

        /**
         * The actual test.
         */
        assertEquals("Server should not have received any messages", 0, serverErrorQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheBamonQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheArcreposQueue.msgReceived);

        NetarkivetMessage msg = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "testMSG");
        con.resend(msg, Channels.getError());

        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        assertEquals("Server should not have received any messages", 0, serverTheArcreposQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheBamonQueue.msgReceived);
        assertEquals("Server should have received 1 message", 1, serverErrorQueue.msgReceived);
    }

    /**
     * Tests that initconnection actually starts a topic connection and a queue connection.
     *
     * @throws Exception On failures
     */
    @Test
    public void testInitConnection() throws Exception {
        /*
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        assertTrue("Should have started the queue connection",
                ((JMSConnectionMockupMQ.TestConnection) con.connection).isStarted);
    }

    @Test
    public void testSendToQueue() throws JMSException, NoSuchFieldException, IllegalAccessException {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        ChannelID sendChannel = Channels.getTheRepos();
        ChannelID replyChannel = Channels.getTheBamon();
        NetarkivetMessage msg = new TestMessage(sendChannel, replyChannel, "testMSG");

        con.send(msg);

        String sendName = sendChannel.getName();

        // Find message producer for queue.
        JMSConnectionMockupMQ.TestMessageProducer queueSender = (JMSConnectionMockupMQ.TestMessageProducer) con.producers
                .get(sendName);

        assertNotNull("Should have created a sender for " + sendName, queueSender);
        ObjectMessage sentSerialMsg = queueSender.messages.get(0);
        assertNotNull("Should have a sent message", sentSerialMsg);
        assertEquals("Received message should be the same as was sent", sentSerialMsg.getObject(), msg);
        assertNotNull("Message should now have an id", msg.getID());
    }

    @Test
    public void testSendToTopic() throws JMSException, NoSuchFieldException, IllegalAccessException {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        Map<String, MessageProducer> publishersMap = con.producers;

        ChannelID sendChannel = Channels.getAllBa();
        ChannelID replyChannel = Channels.getTheBamon();
        NetarkivetMessage msg = new TestMessage(sendChannel, replyChannel, "testMSG");

        con.send(msg);

        String sendName = sendChannel.getName();

        JMSConnectionMockupMQ.TestMessageProducer topicPublisher = (JMSConnectionMockupMQ.TestMessageProducer) publishersMap
                .get(sendName);

        assertNotNull("Should have created a publisher for " + sendName, topicPublisher);
        ObjectMessage sentSerialMsg = topicPublisher.messages.get(0);
        assertNotNull("Should have a published message", sentSerialMsg);
        assertEquals("Received message should be the same as was published", sentSerialMsg.getObject(), msg);
        assertNotNull("Message should now have an id", msg.getID());
    }

    @Test
    public void testSetListener() throws JMSException, NoSuchFieldException, IllegalAccessException {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        MessageListener listener1 = new MessageListener() {
            public void onMessage(Message message) {
                throw new NotImplementedException("Not implemented");
            }

            public String toString() {
                return "listener1";
            }
        };
        MessageListener listener2 = new MessageListener() {
            public void onMessage(Message message) {
                throw new NotImplementedException("Not implemented");
            }

            public String toString() {
                return "listener2";
            }
        };
        ChannelID anyBa = Channels.getAnyBa();
        con.setListener(anyBa, listener1);
        Map<String, MessageConsumer> consumerMap = con.consumers;
        MessageConsumer messageConsumer = consumerMap.get(anyBa.getName() + "##listener1");
        assertEquals("Should have added listener for queue", listener1, messageConsumer.getMessageListener());

        ChannelID allBa = Channels.getAllBa();
        con.setListener(allBa, listener2);
        messageConsumer = consumerMap.get(allBa.getName() + "##listener2");
        assertEquals("Should have added listener for topic", listener2, messageConsumer.getMessageListener());

        con.removeListener(anyBa, listener1);
        assertFalse("Should have lost the listener", consumerMap.containsKey(anyBa.getName() + "##listener1"));
        messageConsumer = consumerMap.get(allBa.getName() + "##listener2");
        assertEquals("Should still have listener for topic", listener2, messageConsumer.getMessageListener());
        con.setListener(anyBa, listener1);
        assertEquals("Should have two listeners now", 2, consumerMap.size());
    }

    @Test
    public void testGetConsumerKey() throws NoSuchMethodException, IllegalAccessException {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        MessageListener listener = new MessageListener() {
            public void onMessage(Message message) {
                throw new NotImplementedException("Not implemented");
            }

            public String toString() {
                return "ourListener";
            }
        };
        assertEquals("Should have expected key for topic", Channels.getAllBa().getName() + "##" + listener.toString(),
                JMSConnection.getConsumerKey(Channels.getAllBa().getName(), listener));
        assertEquals("Should have expected key for queue",
                Channels.getTheBamon().getName() + "##" + listener.toString(),
                JMSConnection.getConsumerKey(Channels.getTheBamon().getName(), listener));
    }

    @Test
    public void testReply() throws JMSException, NoSuchFieldException, IllegalAccessException {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();

        NetarkivetMessage msg = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "testMSG");

        Map<String, MessageProducer> sendersMap = con.producers;

        con.send(msg);
        String sendName = Channels.getTheRepos().getName();
        JMSConnectionMockupMQ.TestMessageProducer queueSender = (JMSConnectionMockupMQ.TestMessageProducer) sendersMap
                .get(sendName);
        ObjectMessage sentSerialMsg = (queueSender.messages.get(0));
        NetarkivetMessage sentMessage = (NetarkivetMessage) sentSerialMsg.getObject();
        sentMessage.setNotOk("Test error");
        con.reply(sentMessage);

        String replyName = Channels.getTheBamon().getName();
        queueSender = (JMSConnectionMockupMQ.TestMessageProducer) sendersMap.get(replyName);
        assertNotNull("Should have a sender for " + replyName, queueSender);

        ObjectMessage receivedSerialMsg = queueSender.messages.get(0);
        NetarkivetMessage received = (NetarkivetMessage) receivedSerialMsg.getObject();
        assertEquals("Should have sent a message on " + queueSender, received, msg);
        assertFalse("Message should now be notOk", received.isOk());

        msg = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "testMSG");
        try {
            con.reply(msg);
            fail("Shouldn't be able to reply to unsent message.");
        } catch (PermissionDenied e) {
            // expected - msg has not been sent.
        }

        try {
            con.reply(null);
            fail("Should not allow null messages");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    private void assertMethodIsFinal(Class aClass, String name, Class[] parameterTypes) {
        try {
            Method m = aClass.getMethod(name, parameterTypes);
            assertTrue(name + "() in JMSConnection is not declared final!", Modifier.isFinal(m.getModifiers()));
        } catch (Exception e) {
            fail("Method " + name + " in JMSConnection doesn't exist!");
        }
    }

    public static class DummyServer implements MessageListener {
        public int msgOK = 0;
        public int msgNotOK = 0;
        public int msgReceived = 0;

        /*
         * (non-Javadoc)
         * 
         * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
         */
        public void onMessage(Message msg) {
            NetarkivetMessage netMsg = JMSConnection.unpack(msg);
            msgReceived = (netMsg.isOk() ? ++msgOK : ++msgNotOK);
        }

        public void reset() {
            msgOK = 0;
            msgNotOK = 0;
            msgReceived = 0;
        }
    }

    private static class DummyMapMessage implements MapMessage {

        public boolean getBoolean(String arg0) throws JMSException {
            return false;
        }

        public byte getByte(String arg0) throws JMSException {
            return 0;
        }

        public short getShort(String arg0) throws JMSException {
            return 0;
        }

        public char getChar(String arg0) throws JMSException {
            return 0;
        }

        public int getInt(String arg0) throws JMSException {
            return 0;
        }

        public long getLong(String arg0) throws JMSException {
            return 0;
        }

        public float getFloat(String arg0) throws JMSException {
            return 0;
        }

        public double getDouble(String arg0) throws JMSException {
            return 0;
        }

        public String getString(String arg0) throws JMSException {
            return null;
        }

        public byte[] getBytes(String arg0) throws JMSException {
            return null;
        }

        public Object getObject(String arg0) throws JMSException {
            return null;
        }

        public Enumeration getMapNames() throws JMSException {
            return null;
        }

        public void setBoolean(String arg0, boolean arg1) throws JMSException {
        }

        public void setByte(String arg0, byte arg1) throws JMSException {
        }

        public void setShort(String arg0, short arg1) throws JMSException {
        }

        public void setChar(String arg0, char arg1) throws JMSException {
        }

        public void setInt(String arg0, int arg1) throws JMSException {
        }

        public void setLong(String arg0, long arg1) throws JMSException {
        }

        public void setFloat(String arg0, float arg1) throws JMSException {
        }

        public void setDouble(String arg0, double arg1) throws JMSException {
        }

        public void setString(String arg0, String arg1) throws JMSException {
        }

        public void setBytes(String arg0, byte[] arg1) throws JMSException {
        }

        public void setBytes(String arg0, byte[] arg1, int arg2, int arg3) throws JMSException {
        }

        public void setObject(String arg0, Object arg1) throws JMSException {
        }

        public boolean itemExists(String arg0) throws JMSException {
            return false;
        }

        public String getJMSMessageID() throws JMSException {
            return null;
        }

        public void setJMSMessageID(String arg0) throws JMSException {
        }

        public long getJMSTimestamp() throws JMSException {
            return 0;
        }

        public void setJMSTimestamp(long arg0) throws JMSException {
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            return null;
        }

        public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        }

        public void setJMSCorrelationID(String arg0) throws JMSException {
        }

        public String getJMSCorrelationID() throws JMSException {
            return null;
        }

        public Destination getJMSReplyTo() throws JMSException {
            return null;
        }

        public void setJMSReplyTo(Destination arg0) throws JMSException {
        }

        public Destination getJMSDestination() throws JMSException {
            return null;
        }

        public void setJMSDestination(Destination arg0) throws JMSException {
        }

        public int getJMSDeliveryMode() throws JMSException {
            return 0;
        }

        public void setJMSDeliveryMode(int arg0) throws JMSException {
        }

        public boolean getJMSRedelivered() throws JMSException {
            return false;
        }

        public void setJMSRedelivered(boolean arg0) throws JMSException {
        }

        public String getJMSType() throws JMSException {
            return null;
        }

        public void setJMSType(String arg0) throws JMSException {
        }

        public long getJMSExpiration() throws JMSException {
            return 0;
        }

        public void setJMSExpiration(long arg0) throws JMSException {
        }

        public int getJMSPriority() throws JMSException {
            return 0;
        }

        public void setJMSPriority(int arg0) throws JMSException {
        }

        public void clearProperties() throws JMSException {
        }

        public boolean propertyExists(String arg0) throws JMSException {
            return false;
        }

        public boolean getBooleanProperty(String arg0) throws JMSException {
            return false;
        }

        public byte getByteProperty(String arg0) throws JMSException {
            return 0;
        }

        public short getShortProperty(String arg0) throws JMSException {
            return 0;
        }

        public int getIntProperty(String arg0) throws JMSException {
            return 0;
        }

        public long getLongProperty(String arg0) throws JMSException {
            return 0;
        }

        public float getFloatProperty(String arg0) throws JMSException {
            return 0;
        }

        public double getDoubleProperty(String arg0) throws JMSException {
            return 0;
        }

        public String getStringProperty(String arg0) throws JMSException {
            return null;
        }

        public Object getObjectProperty(String arg0) throws JMSException {
            return null;
        }

        public Enumeration getPropertyNames() throws JMSException {
            return null;
        }

        public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
        }

        public void setByteProperty(String arg0, byte arg1) throws JMSException {
        }

        public void setShortProperty(String arg0, short arg1) throws JMSException {
        }

        public void setIntProperty(String arg0, int arg1) throws JMSException {
        }

        public void setLongProperty(String arg0, long arg1) throws JMSException {
        }

        public void setFloatProperty(String arg0, float arg1) throws JMSException {
        }

        public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        }

        public void setStringProperty(String arg0, String arg1) throws JMSException {
        }

        public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        }

        public void acknowledge() throws JMSException {
        }

        public void clearBody() throws JMSException {
        }

    }

    private static class DummySerializableClass implements Serializable {

    }

    private static class TestMessage extends NetarkivetMessage {
        private String testID;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo);
            this.testID = testID;
        }

        public String getTestID() {
            return testID;
        }
    }
}
