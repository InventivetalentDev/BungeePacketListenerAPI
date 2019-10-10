/*
 *
 */

package org.inventivetalent.packetlistener.handler;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.inventivetalent.packetlistener.bungee.Cancellable;

/**
 * Wrapper class or received packets
 *
 * @see Packet
 * @see SentPacket
 */
public class ReceivedPacket extends Packet {

	public ReceivedPacket(Object packet, Cancellable cancel, ProxiedPlayer player) {
		super(packet, cancel, player);
	}
	
	public ReceivedPacket(Object packet, Cancellable cancel, PendingConnection connection) {
		super(packet, cancel, connection);
	}

}
