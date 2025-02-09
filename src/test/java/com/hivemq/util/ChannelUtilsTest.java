/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.util;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.security.auth.ClientToken;
import com.hivemq.security.ssl.SslClientCertificateImpl;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import org.junit.Test;
import util.DummyHandler;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelUtilsTest {

    @Test
    public void test_channel_ip() throws Exception {
        final Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(0));

        final Optional<String> channelIP = ChannelUtils.getChannelIP(channel);

        assertEquals(true, channelIP.isPresent());
        assertEquals("0.0.0.0", channelIP.get());
    }

    @Test
    public void test_no_socket_address_available() throws Exception {
        final Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(null);

        assertEquals(false, ChannelUtils.getChannelIP(channel).isPresent());
    }

    @Test
    public void test_no_inet_socket_address_available() throws Exception {
        final Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new LocalAddress("myId"));

        assertEquals(false, ChannelUtils.getChannelIP(channel).isPresent());
    }

    @Test
    public void test_token_from_channel_only_client_id() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, null));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("theId");

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals(false, clientToken.getUsername().isPresent());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_cert() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, null));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("theId");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthCertificate(new SslClientCertificateImpl(new Certificate[]{}));

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(true, clientToken.getCertificate().isPresent());
        assertEquals(false, clientToken.getUsername().isPresent());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_username() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, null));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("theId");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthUsername("user");

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals("user", clientToken.getUsername().get());
        assertEquals(false, clientToken.getPassword().isPresent());
        assertEquals(false, clientToken.getPasswordBytes().isPresent());
    }

    @Test
    public void test_token_from_channel_client_id_and_username_and_password() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, null));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("theId");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthUsername("user");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthPassword("pass".getBytes(UTF_8));

        final ClientToken clientToken = ChannelUtils.tokenFromChannel(channel);

        assertEquals("theId", clientToken.getClientId());
        assertEquals(false, clientToken.getCertificate().isPresent());
        assertEquals("user", clientToken.getUsername().get());
        assertEquals("pass", clientToken.getPassword().get());
        assertArrayEquals("pass".getBytes(UTF_8), clientToken.getPasswordBytes().get());
    }

    @Test
    public void test_messages_in_flight() {

        final EmbeddedChannel channel = new EmbeddedChannel(new DummyHandler());
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);

        clientConnection.setInFlightMessagesSent(true);
        assertFalse(ChannelUtils.messagesInFlight(channel));

        clientConnection.setInFlightMessages(new AtomicInteger(1));
        assertTrue(ChannelUtils.messagesInFlight(channel));

        clientConnection.setInFlightMessages(new AtomicInteger(0));
        assertFalse(ChannelUtils.messagesInFlight(channel));
    }
}
