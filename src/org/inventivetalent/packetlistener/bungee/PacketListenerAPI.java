/*
 *
 */

package org.inventivetalent.packetlistener.bungee;

import org.inventivetalent.packetlistener.handler.PacketHandler;
import org.inventivetalent.packetlistener.handler.ReceivedPacket;
import org.inventivetalent.packetlistener.handler.SentPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import org.mcstats.MetricsLite;

public class PacketListenerAPI extends Plugin implements Listener {

	@Override
	public void onEnable() {
		ProxyServer.getInstance().getPluginManager().registerListener(this, this);

		ProxyServer.getInstance().getScheduler().runAsync(this, new Runnable() {

			@Override
			public void run() {
				try {
					MetricsLite metrics = new MetricsLite(PacketListenerAPI.this);
					if (metrics.start()) {
						System.out.println("[BungeePacketListenerAPI] Metrics started.");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@EventHandler
	public void onConnect(PlayerHandshakeEvent e) {
		addConnectionChannel(e.getConnection());
	}

	@EventHandler
	public void onJoin(PostLoginEvent e) {
		addChannel(e.getPlayer());

	}

	@EventHandler
	public void onQuit(PlayerDisconnectEvent e) {
		removeChannel(e.getPlayer());
	}

	private static Channel getWrapperChannel(Object wrapper) {
		try {
			return (Channel) wrapper.getClass().getDeclaredMethod("getHandle").invoke(wrapper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object getChannel(ProxiedPlayer player) throws Exception {
		return AccessUtil.setAccessible(UserConnection.class.getDeclaredField("ch")).get((UserConnection) player);
	}

	public static Object getChannel(PendingConnection conn) throws Exception {
		Object channel = null;
		if (conn instanceof InitialHandler) {
			channel = AccessUtil.setAccessible(InitialHandler.class.getDeclaredField("ch")).get((InitialHandler) conn);
		}
		return channel;
	}

	void addConnectionChannel(final PendingConnection connection) {
		try {
			if (connection instanceof InitialHandler) {
				Object channel = getChannel(connection);
				getWrapperChannel(channel).pipeline().addBefore("inbound-boss", "packet_listener_connection", new ChannelDuplexHandler() {

					@Override
					public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
						Cancellable cancellable = new Cancellable();
						Object pckt = msg;

						if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
							ByteBuf copy = ((ByteBuf) pckt).copy();
							int packetId = DefinedPacket.readVarInt(copy);
							if (packetId != 0) {
								onPacketSend(connection, packetId, cancellable);
							}
						}

						if (DefinedPacket.class.isAssignableFrom(msg.getClass())) {
							pckt = (DefinedPacket) onPacketSend(connection, (DefinedPacket) msg, cancellable);
						}
						if (PacketWrapper.class.isAssignableFrom(msg.getClass())) {
							pckt = (PacketWrapper) onPacketSend(connection, msg, cancellable);
						}
						if (cancellable.isCancelled()) { return; }
						super.write(ctx, pckt, promise);
					}

					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
						Cancellable cancellable = new Cancellable();
						Object pckt = msg;

						if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
							ByteBuf copy = ((ByteBuf) pckt).copy();
							int packetId = DefinedPacket.readVarInt(copy);
							if (packetId != 0) {
								onPacketReceive(connection, packetId, cancellable);
							}
						}

						if (DefinedPacket.class.isAssignableFrom(msg.getClass())) {
							pckt = (DefinedPacket) onPacketReceive(connection, (DefinedPacket) msg, cancellable);
						}
						if (PacketWrapper.class.isAssignableFrom(msg.getClass())) {
							pckt = (PacketWrapper) onPacketReceive(connection, msg, cancellable);
						}
						if (cancellable.isCancelled()) { return; }
						super.channelRead(ctx, pckt);
					}

				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addChannel(final ProxiedPlayer player) {
		try {
			Object channel = getChannel(player);
			Channel wrapped = getWrapperChannel(channel);
			if (wrapped.pipeline().get("packet_listener_connection") != null) {
				wrapped.pipeline().remove("packet_listener_connection");// Remove the connection listener
			}
			wrapped.pipeline().addBefore("inbound-boss", "packet_listener_player", new ChannelDuplexHandler() {

				@Override
				public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
					Cancellable cancellable = new Cancellable();
					Object pckt = msg;

					if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
						ByteBuf copy = ((ByteBuf) pckt).copy();
						int packetId = DefinedPacket.readVarInt(copy);
						if (packetId != 0) {
							onPacketSend(player, packetId, cancellable);
						}
					}

					if (DefinedPacket.class.isAssignableFrom(msg.getClass())) {
						pckt = (DefinedPacket) onPacketSend(player, (DefinedPacket) msg, cancellable);
					}
					if (PacketWrapper.class.isAssignableFrom(msg.getClass())) {
						pckt = (PacketWrapper) onPacketSend(player, msg, cancellable);
					}
					if (cancellable.isCancelled()) { return; }
					super.write(ctx, pckt, promise);
				}

				@Override
				public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
					Cancellable cancellable = new Cancellable();
					Object pckt = msg;

					if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
						ByteBuf copy = ((ByteBuf) pckt).copy();
						int packetId = DefinedPacket.readVarInt(copy);
						if (packetId != 0) {
							onPacketReceive(player, packetId, cancellable);
						}
					}

					if (DefinedPacket.class.isAssignableFrom(msg.getClass())) {
						pckt = (DefinedPacket) onPacketReceive(player, (DefinedPacket) msg, cancellable);
					}
					if (PacketWrapper.class.isAssignableFrom(msg.getClass())) {
						pckt = (PacketWrapper) onPacketReceive(player, msg, cancellable);
					}
					if (cancellable.isCancelled()) { return; }
					super.channelRead(ctx, pckt);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeChannel(ProxiedPlayer player) {
		try {
			Object channel = getChannel(player);
			getWrapperChannel(channel).pipeline().remove("packet_listener_player");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object onPacketReceive(Object p, Object packet, Cancellable cancellable) {
		if (packet == null) { return packet; }
		ReceivedPacket pckt = null;
		if (p instanceof ProxiedPlayer) {
			pckt = new ReceivedPacket(packet, cancellable, (ProxiedPlayer) p);
		}
		if (p instanceof PendingConnection) {
			pckt = new ReceivedPacket(packet, cancellable, (PendingConnection) p);
		}
		if (pckt == null) {
			return packet;
		}
		PacketHandler.notifyHandlers(pckt);
		return pckt.getSourcePacket();
	}

	public Object onPacketSend(Object p, Object packet, Cancellable cancellable) {
		if (packet == null) { return packet; }
		SentPacket pckt = null;
		if (p instanceof ProxiedPlayer) {
			pckt = new SentPacket(packet, cancellable, (ProxiedPlayer) p);
		}
		if (p instanceof PendingConnection) {
			pckt = new SentPacket(packet, cancellable, (PendingConnection) p);
		}
		if (pckt == null) { return packet; }
		PacketHandler.notifyHandlers(pckt);
		return pckt.getSourcePacket();
	}

}
