/*
 *
 */

package org.inventivetalent.packetlistener.handler;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.inventivetalent.packetlistener.bungee.Cancellable;

/**
 * Wrapper class for sent packets
 *
 * @see Packet
 * @see ReceivedPacket
 */
public class SentPacket extends Packet {

	public SentPacket(Object packet, Cancellable cancel, ProxiedPlayer player) {
		super(packet, cancel, player);
	}

	public SentPacket(Object packet, Cancellable cancel, PendingConnection connection) {
		super(packet, cancel, connection);
	}

}
