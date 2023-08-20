/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.HybridEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Slf4j
public abstract class ModCommand extends SlashCommand {
    protected final Vortex vortex;

    public ModCommand(Vortex vortex, Permission... altPerms) {
        this.vortex = vortex;
        this.guildOnly = true;
        this.category = new Category("Moderation", event -> {
            if (!event.getChannelType().isGuild()) {
                event.replyError("This command is not available in Direct Messages!");
                return false;
            }

            Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
            if (modrole != null && event.getMember().getRoles().contains(modrole)) {
                return true;
            }

            for (Permission altPerm : altPerms) {
                if (altPerm.isChannel()) {
                    if (event.getMember().hasPermission(event.getGuildChannel(), altPerm)) {
                        return true;
                    }
                } else {
                    if (event.getMember().hasPermission(altPerm)) {
                        return true;
                    }
                }
            }

            return false;
        });
    }

    protected void handleError(HybridEvent event, Throwable t, Action action, long targetId) {
        String mention = FormatUtil.formatUserMention(targetId);
        String errMsg = null;
        if (t instanceof ErrorResponseException e) {
            errMsg = getErrorMessage(e.getErrorResponse(), action, mention);
        } else if (t instanceof InsufficientPermissionException e) {
            errMsg = getErrorMessage(ErrorResponse.MISSING_PERMISSIONS, action, mention);
        } else if (t instanceof HierarchyException e) {
            errMsg = String.format("I am unable to ban %s", mention);
        }

        if (errMsg == null) {
            log.warn("Failed to " + action.getVerb() + " a user", t);
            errMsg = String.format("Failed to %s %s", action.getVerb(), mention);
        }

        event.replyError(errMsg);
    }

    protected void handleError(HybridEvent event, ErrorResponse e, Action action, long targetId) {
        String mention = FormatUtil.formatUserMention(targetId);
        String errMsg = getErrorMessage(e, action, mention);

        if (errMsg == null) {
            log.warn("Failed to " + action.getVerb() + " a user", new RuntimeException(e.getMeaning()));
            errMsg = String.format("Failed to %s %s", action.getVerb(), mention);
        }

        event.replyError(errMsg);
    }

    private String getErrorMessage(ErrorResponse e, Action action, String mention) {
        return switch (e) {
            case MISSING_PERMISSIONS ->
                    String.format("I do not have any roles with permissions to %s users.", action.getVerb());
            case UNKNOWN_MEMBER ->
                    String.format("I am unable to %s %s as they are not in this server", action.getVerb(), mention);
            case UNKNOWN_USER ->
                    String.format("No %s role exists!", action.getPastVerb());
            case UNKNOWN_CHANNEL ->
                    "The vc was deleted before I could do anything";
            default -> null;
        };
    }
}
