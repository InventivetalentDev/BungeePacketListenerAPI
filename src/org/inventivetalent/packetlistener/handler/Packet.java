/*
 *
 */

package org.inventivetalent.packetlistener.handler;

import org.inventivetalent.packetlistener.bungee.Cancellable;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;

import java.lang.reflect.Field;

/**
 * Base class for sent or received packets
 *
 * @see SentPacket
 * @see ReceivedPacket
 */
public abstract class Packet {

	private ProxiedPlayer     player;
	private PendingConnection connection;

	private int         packetId = -1;
	private Object      packet;
	private Cancellable cancel;

	public Packet(Object packet, Cancellable cancel, ProxiedPlayer player) {
		this.player = player;
		if (packet instanceof Integer) { this.packetId = (Integer) packet; } else { this.packet = packet; }
		if (packet instanceof PacketWrapper) {
			int packetId = DefinedPacket.readVarInt(((PacketWrapper) packet).buf.copy());
			if (packetId != 0) {
				this.packetId = packetId;
			}
		}
		this.cancel = cancel;
	}

	public Packet(Object packet, Cancellable cancel, PendingConnection connection) {
		this.connection = connection;
		if (packet instanceof Integer) { this.packetId = (Integer) packet; } else { this.packet = packet; }
		if (packet instanceof PacketWrapper) {
			int packetId = DefinedPacket.readVarInt(((PacketWrapper) packet).buf.copy());
			if (packetId != 0) {
				this.packetId = packetId;
			}
		}
		this.cancel = cancel;
	}

	/**
	 * Modify a value of the packet
	 *
	 * @param field Name of the field to modify
	 * @param value Value to be assigned to the field
	 */
	public void setPacketValue(String field, Object value) {
		try {
			Field f = this.packet.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.set(this.packet, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a value of the packet
	 *
	 * @param field Name of the field
	 * @return current value of the field
	 */
	public Object getPacketValue(String field) {
		Object value = null;
		try {
			Field f = this.packet.getClass().getDeclaredField(field);
			f.setAccessible(true);
			value = f.get(this.packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	/**
	 * @param b if set to <code>true</code> the packet will be cancelled
	 */
	public void setCancelled(boolean b) {
		this.cancel.setCancelled(b);
	}

	/**
	 * @return <code>true</code> if the packet has been cancelled
	 */
	public boolean isCancelled() {
		return this.cancel.isCancelled();
	}

	/**
	 * @return The receiving or sending player of the packet
	 * @see #hasPlayer()
	 */
	public ProxiedPlayer getPlayer() {
		return this.player;
	}

	public PendingConnection getConection() {
		return this.connection;
	}

	/**
	 * @return <code>true</code> if the packet has a player
	 */
	public boolean hasPlayer() {
		return this.player != null;
	}

	/**
	 * @return The name of the receiving or sending player
	 * @see #hasPlayer()
	 * @see #getPlayer()
	 */
	public String getPlayername() {
		if (!this.hasPlayer()) { return null; }
		return this.player.getName();
	}

	/**
	 * Change the packet that is sent
	 *
	 * @param packet new packet
	 */
	public void setPacket(DefinedPacket packet) {
		if (this.packet instanceof DefinedPacket) {
			this.packet = packet;
		}
		if (this.packet instanceof PacketWrapper) {
			this.packet = new PacketWrapper(packet, ((PacketWrapper) this.packet).buf);
		}
	}

	/**
	 * @return the sent or received packet as an Object
	 */
	public DefinedPacket getPacket() {
		if (packet instanceof DefinedPacket) {
			return (DefinedPacket) packet;
		}
		if (packet instanceof PacketWrapper) {
			return ((PacketWrapper) packet).packet;
		}
		return null;
	}

	/**
	 * @return the sent or received packet as an Object
	 */
	public Object getSourcePacket() {
		return this.packet;
	}

	public void setSourcePacket(Object packet) {
		this.packet = packet;
	}

	/**
	 * @return the class name of the sent or received packet
	 */
	public String getPacketName() {
		return isRaw() ? String.format("0x%02X", packetId) : this.packet.getClass().getSimpleName();
	}

	public int getPacketId() {
		return packetId;
	}

	public boolean isRaw() {
		return packetId != -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.cancel == null ? 0 : this.cancel.hashCode());
		result = prime * result + (this.packet == null ? 0 : this.packet.hashCode());
		result = prime * result + (this.player == null ? 0 : this.player.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (this.getClass() != obj.getClass()) { return false; }
		Packet other = (Packet) obj;
		if (this.cancel == null) {
			if (other.cancel != null) { return false; }
		} else if (!this.cancel.equals(other.cancel)) { return false; }
		if (this.packet == null) {
			if (other.packet != null) { return false; }
		} else if (!this.packet.equals(other.packet)) { return false; }
		if (this.player == null) {
			if (other.player != null) { return false; }
		} else if (!this.player.equals(other.player)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return "Packet{ " + (this.getClass().equals(SentPacket.class) ? "[> OUT >]" : "[< IN <]") + " " + this.getPacketName() + " " + (this.hasPlayer() ? this.getPlayername() : "#server#") + " }";
	}

}
