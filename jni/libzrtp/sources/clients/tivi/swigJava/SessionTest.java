/*
 * Copyright (c) 2013 Slient Circle LLC.  All rights reserved.
 *
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

import java.net.*;

import wd.tivi.*;

public class SessionTest {
    static {
        System.loadLibrary("zrtptivi");
    }

    CtZrtpSession session;
    TestCallbackAudio callback;
    TestSendCallbackAudio sendCallback;
    DatagramSocket dgSock;

    InetAddress localAddr;
    InetAddress remoteAddr;

    long uiSSRC = 0xfeedbacc;


    // This is the callback that we use for audio stream
    private class TestCallbackAudio extends CtZrtpCb {

        @Override
        public void onNewZrtpStatus(CtZrtpSession session, String p, CtZrtpSession.streamName streamNm) {
            System.out.println("new status: " + p);
        }

        @Override
        public void onNeedEnroll(CtZrtpSession session, CtZrtpSession.streamName streamNm, int info) {
            System.out.println("Need enroll\n");
        }

        @Override
        public void onPeer(CtZrtpSession session, String name, int iIsVerified, CtZrtpSession.streamName streamNm) {
            System.out.println("onPeer: " + name);

            byte[] buffer = new byte[30];

            session.getInfo("rs1", buffer, buffer.length);
            System.out.print("RS1: " + new String(buffer) + " ");

            session.getInfo("rs2", buffer, buffer.length);
            System.out.print("RS2: " + new String(buffer) + " ");

            session.getInfo("pbx", buffer, buffer.length);
            System.out.print("PBX: " + new String(buffer) + " ");

            session.getInfo("aux", buffer, buffer.length);
            System.out.println("AUX: " + new String(buffer) + " ");

            session.getInfo("lbClient", buffer, buffer.length);
            System.out.print("Client: " + new String(buffer) + " ");

            session.getInfo("lbVersion", buffer, buffer.length);
            System.out.print("Version: " + new String(buffer) + " ");

            session.getInfo("lbChiper", buffer, buffer.length);
            System.out.print("cipher: " + new String(buffer) + " ");

            session.getInfo("lbHash", buffer, buffer.length);
            System.out.print("hash: " + new String(buffer) + " ");

            session.getInfo("lbAuthTag", buffer, buffer.length);
            System.out.print("auth: " + new String(buffer) + " ");

            session.getInfo("lbKeyExchange", buffer, buffer.length);
            System.out.print("KeyEx:  " + new String(buffer) + " ");

            session.getInfo("sc_secure", buffer, buffer.length);
            System.out.print("SC secure:  " + new String(buffer) + " ");

            session.getInfo("sdp_hash", buffer, buffer.length);
            System.out.println("zrtp-hash: " + new String(buffer) + " ");

            session.setLastPeerNameVerify("TestName", 0);
        }

        @Override
        public void onZrtpWarning(CtZrtpSession session, String p, CtZrtpSession.streamName streamNm) {
            System.out.println("Warning: " + p);
        }
    }

    private class TestSendCallbackAudio extends CtZrtpSendCb {
        @Override
        public void sendRtp(CtZrtpSession session, byte[] packet, CtZrtpSession.streamName streamNm) {
//        hexdump("ZRTP packet", packet, length);
        System.out.println("ZRTP send packet, length: " + packet.length);
        sendData(packet);
        }
    }


    void sendData(byte[ ]buffer) {
        DatagramPacket dgram = new DatagramPacket(buffer, buffer.length, remoteAddr, 5004);
        try {
            dgSock.send(dgram);
        }
        catch (java.io.IOException ex) {
            System.out.println("cannot send: " + ex);
        }
    }

    public void simpleTest(String argv[]) {
        byte[] helloHash = new byte[100];
        byte[] dgramBuffer = new byte[2000];
        long[] newLength = new long[1];

        String local = "127.0.0.1";
        String remote = "127.0.0.1";

        if (argv.length >= 2) {
            local = argv[0];
            remote = argv[1];
        }
        // Setup the IP addresses to receive and send packets
        try {
            localAddr = InetAddress.getByName(local);
            remoteAddr = InetAddress.getByName(remote);
        }
        catch (java.net.UnknownHostException ex) {
            System.out.println("Host not known: " + ex);
        }
        System.out.println("Address: " + localAddr.getHostAddress() + ", " + remoteAddr.getHostAddress());

        session = new CtZrtpSession();
        callback = new TestCallbackAudio();
        sendCallback = new TestSendCallbackAudio();

        session.init(true, true);                      // audio and video

        session.setUserCallback(callback, CtZrtpSession.streamName.AudioStream);
        session.setSendCallback(sendCallback, CtZrtpSession.streamName.AudioStream);
        session.getSignalingHelloHash(helloHash, CtZrtpSession.streamName.AudioStream);

        System.out.println("Our Hello hash: " + new String(helloHash));

        // Our receive datagram socket
        try {
            dgSock = new DatagramSocket(5002, localAddr);
        }
        catch (java.net.SocketException ex) {
            System.out.println("cannot create datagram socket: " + ex);
        }

        if (!session.isStarted(CtZrtpSession.streamName.AudioStream)) {
            System.out.println("Starting ...");
            session.start(uiSSRC, CtZrtpSession.streamName.AudioStream);
        }
        DatagramPacket dgram = new DatagramPacket(dgramBuffer, dgramBuffer.length);
        for (;;) {
            try {
                dgSock.receive(dgram);
            }
            catch (java.io.IOException ex) {
                System.out.println("cannot receive: " + ex);
            }
//            hexdump("recv buffer before", dgramBuffer, dgram.getLength());
            int rc = session.processIncomingRtp(dgramBuffer, (long)dgram.getLength(), newLength, CtZrtpSession.streamName.AudioStream);

            if (rc == 0)
                continue;

            System.out.println("processing returns: " + rc + ", new length: " + newLength[0]);
            System.out.println("Received data: "+ new String(dgramBuffer, 12, (int)(newLength[0]-12)));
//            hexdump("recv buffer after", dgramBuffer, dgram.getLength());
        }
    }

    public static void main(String argv[]) {
        CtZrtpSession.initCache("testzid.dat");

        SessionTest stest = new SessionTest();
        stest.simpleTest(argv);
        System.out.println("Something works.");
        try {
            Thread.sleep(5000);
        }
        catch (java.lang.InterruptedException ex) {
        }
    }

    private static final char[] hex = "0123456789abcdef".toCharArray();

    /**
     * Dump a buffer in hex and readable format.
     *
     * @param title Printed at the beginning of the dump
     * @param buf   Byte buffer to dump
     * @param len   Number of bytes to dump, should be less or equal
     *              the buffer length
     */
    public void hexdump(String title, byte[] buf, int len) {
        byte b;
        System.err.println(title);
        for(int i = 0 ; ; i += 16) {
            for(int j=0; j < 16; ++j) {
                if (i+j >= len) {
                    System.err.print("   ");
                }
                else {
                    b = buf[i+j];
                    System.err.print(" "+ hex[(b>>>4) &0xf] + hex[b&0xf] );
                }
            }
            System.err.print("  ");
            for(int j = 0; j < 16; ++j) {
                if (i+j >= len) break;
                b = buf[i+j];
                if ( (byte)(b+1) < 32+1) {
                    System.err.print( '.' );
                }
                else {
                    System.err.print( (char)b );
                }
            }
            System.err.println();
            if (i+16 >= len) {
                break;
            }
        }
    }
}
