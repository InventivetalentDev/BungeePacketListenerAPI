/*
 *
 */

package org.inventivetalent.packetlistener.handler;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;

public abstract class PacketHandler {

	private static final List<PacketHandler> handlers = new ArrayList<PacketHandler>();

	public static boolean addHandler(PacketHandler handler) {
		boolean b = handlers.contains(handler);
		handlers.add(handler);
		return !b;
	}

	public static boolean removeHandler(PacketHandler handler) {
		return handlers.remove(handler);
	}

	public static void notifyHandlers(SentPacket packet) {
		for (PacketHandler handler : getHandlers()) {
			try {
				PacketOptions options = handler.getClass().getMethod("onSend", SentPacket.class).getAnnotation(PacketOptions.class);
				if (options != null) {
					if (options.forcePlayer() && options.forceServer()) { throw new IllegalArgumentException("Cannot force player and server packets at the same time!"); }
					if (options.forcePlayer()) {
						if (!packet.hasPlayer()) {
							continue;
						}
					} else if (options.forceServer()) {
						if (packet.hasPlayer()) {
							continue;
						}
					}
					if (options.ignoreRaw()) {
						if (packet.isRaw()) {
							continue;
						}
					}
				}
				handler.onSend(packet);
			} catch (Exception e) {
				System.err.println("[PacketListenerAPI] An exception occured while trying to execute 'onSend'" + (handler.plugin != null ? " in plugin " + handler.plugin.getDescription().getName() : "") + ": " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}

	public static void notifyHandlers(ReceivedPacket packet) {
		for (PacketHandler handler : getHandlers()) {
			try {
				PacketOptions options = handler.getClass().getMethod("onReceive", ReceivedPacket.class).getAnnotation(PacketOptions.class);
				if (options != null) {
					if (options.forcePlayer() && options.forceServer()) { throw new IllegalArgumentException("Cannot force player and server packets at the same time!"); }
					if (options.forcePlayer()) {
						if (!packet.hasPlayer()) {
							continue;
						}
					} else if (options.forceServer()) {
						if (packet.hasPlayer()) {
							continue;
						}
					}
					if (options.ignoreRaw()) {
						if (packet.isRaw()) {
							continue;
						}
					}
				}
				handler.onReceive(packet);
			} catch (Exception e) {
				System.err.println("[PacketListenerAPI] An exception occured while trying to execute 'onReceive'" + (handler.plugin != null ? " in plugin " + handler.plugin.getDescription().getName() : "") + ": " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}

	public static List<PacketHandler> getHandlers() {
		return new ArrayList<>(handlers);
	}

	public static List<PacketHandler> getForPlugin(Plugin plugin) {
		List<PacketHandler> handlers = new ArrayList<>();
		if (plugin == null) { return handlers; }
		for (PacketHandler h : getHandlers())
			if (plugin.equals(h.getPlugin())) {
				handlers.add(h);
			}
		return handlers;
	}

	// Sending methods
	public void sendPacket(ProxiedPlayer p, DefinedPacket packet) {
		if (p == null || packet == null) { throw new NullPointerException(); }
		p.unsafe().sendPacket(packet);
	}

	public void sendPacket(PendingConnection conn, DefinedPacket packet) {
		if (conn == null || packet == null) { throw new NullPointerException(); }
		if (conn instanceof InitialHandler) {
			InitialHandler handler = (InitialHandler) conn;
			handler.unsafe().sendPacket(packet);
		}
	}

	// //////////////////////////////////////////////////

	private Plugin plugin;

	@Deprecated
	public PacketHandler() {

	}

	public PacketHandler(Plugin plugin) {
		this.plugin = plugin;
	}

	public Plugin getPlugin() {
		return this.plugin;
	}

	public abstract void onSend(SentPacket packet);

	public abstract void onReceive(ReceivedPacket packet);

}
