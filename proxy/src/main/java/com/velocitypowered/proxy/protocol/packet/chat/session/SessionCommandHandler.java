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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import java.util.concurrent.CompletableFuture;

public class SessionCommandHandler implements CommandHandler<SessionPlayerCommand> {

  private final ConnectedPlayer player;
  private final VelocityServer server;

  public SessionCommandHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  @Override
  public Class<SessionPlayerCommand> packetClass() {
    return SessionPlayerCommand.class;
  }

  @Override
  public void handlePlayerCommandInternal(SessionPlayerCommand packet) {
    queueCommandResult(this.server, this.player, event -> {
      CommandExecuteEvent.CommandResult result = event.getResult();
      if (result == CommandExecuteEvent.CommandResult.denied()) {
        return CompletableFuture.completedFuture(null);
      }

      String commandToRun = result.getCommand().orElse(packet.command);
      if (result.isForwardToServer()) {
        return CompletableFuture.completedFuture(this.player.getChatBuilderFactory()
            .builder()
            .setTimestamp(packet.timeStamp)
            .asPlayer(this.player)
            .message("/" + commandToRun)
            .toServer());
      }

      return runCommand(this.server, this.player, commandToRun, hasRun -> {
        if (!hasRun) {
          return this.player.getChatBuilderFactory()
              .builder()
              .setTimestamp(packet.timeStamp)
              .asPlayer(this.player)
              .message("/" + commandToRun)
              .toServer();
        }
        return null;
      });
    }, packet.command, packet.timeStamp);
  }
}
