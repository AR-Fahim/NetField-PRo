package com.netfieldpro.core.latency;

import java.nio.charset.StandardCharsets;

public final class IcmpPacket {

    private IcmpPacket() {
    }

    public static byte[] echoRequest(int id, int seq, byte[] payload) {
        int payloadLen = (payload != null) ? payload.length : 0;
        byte[] packet = new byte[8 + payloadLen];
        packet[0] = 8; // Type 8: Echo Request
        packet[1] = 0; // Code 0
        packet[2] = 0; // Checksum placeholder
        packet[3] = 0;
        packet[4] = (byte) ((id >> 8) & 0xff);
        packet[5] = (byte) (id & 0xff);
        packet[6] = (byte) ((seq >> 8) & 0xff);
        packet[7] = (byte) (seq & 0xff);
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, packet, 8, payloadLen);
        }

        int checksum = calculateChecksum(packet, packet.length);
        packet[2] = (byte) ((checksum >> 8) & 0xff);
        packet[3] = (byte) (checksum & 0xff);
        return packet;
    }

    public static byte[] echoRequest(int id, int seq, String payload) {
        byte[] payloadBytes = (payload != null) ? payload.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return echoRequest(id, seq, payloadBytes);
    }

    public static int parseEchoReply(byte[] buf, int len) {
        if (buf == null || len < 8) {
            return -1;
        }
        int type = buf[0] & 0xff;
        int code = buf[1] & 0xff;
        if (type != 0 || code != 0) {
            return -1;
        }
        return ((buf[6] & 0xff) << 8) | (buf[7] & 0xff);
    }

    public static int calculateChecksum(byte[] data, int length) {
        int sum = 0;
        int i = 0;
        while (i < length - 1) {
            int high = data[i] & 0xff;
            int low = data[i + 1] & 0xff;
            sum += (high << 8) | low;
            i += 2;
        }
        if (i < length) {
            int high = data[i] & 0xff;
            sum += (high << 8);
        }
        while ((sum >> 16) != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        return (~sum) & 0xffff;
    }
}
