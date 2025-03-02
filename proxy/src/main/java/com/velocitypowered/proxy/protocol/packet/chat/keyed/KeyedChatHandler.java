/*
 * Copyright (C) 2022-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet.chat.keyed;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyedChatHandler implements
    com.velocitypowered.proxy.protocol.packet.chat.ChatHandler<KeyedPlayerChat> {

  private static final Logger logger = LogManager.getLogger(KeyedChatHandler.class);

  private final VelocityServer server;
  private final ConnectedPlayer player;

  public KeyedChatHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public Class<KeyedPlayerChat> packetClass() {
    return KeyedPlayerChat.class;
  }

  @Override
  public void handlePlayerChatInternal(KeyedPlayerChat packet) {
    ChatQueue chatQueue = this.player.getChatQueue();
    EventManager eventManager = this.server.getEventManager();
    PlayerChatEvent toSend = new PlayerChatEvent(player, packet.getMessage());
    CompletableFuture<PlayerChatEvent> future = eventManager.fire(toSend);

    CompletableFuture<MinecraftPacket> chatFuture = future.thenApply(pme -> {
      PlayerChatEvent.ChatResult chatResult = pme.getResult();
      if (!chatResult.isAllowed()) {
        return null;
      }

      return player.getChatBuilderFactory().builder()
          .message(chatResult.getMessage().orElse(packet.getMessage()))
          .setTimestamp(packet.getExpiry()).toServer();
    });
    chatQueue.queuePacket(
        chatFuture.exceptionally((ex) -> {
          logger.error("Exception while handling player chat for {}", player, ex);
          return null;
        }),
        packet.getExpiry()
    );
  }

}
