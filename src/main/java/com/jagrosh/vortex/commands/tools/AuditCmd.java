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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandWarningException;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AuditCmd extends Command {
    private final static String LINESTART = "\u25AB"; // ▫
    private final static String UNKNOWN = "*Unknown*";

    private final String actions;

    public AuditCmd() {
        this.name = "audit";
        this.aliases = new String[]{"auditlog", "auditlogs", "audits"};
        this.arguments = "<ALL | FROM | ACTION> [target]";
        this.help = "fetches recent audit logs";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VIEW_AUDIT_LOGS, Permission.MESSAGE_EXT_EMOJI};
        this.userPermissions = new Permission[]{Permission.VIEW_AUDIT_LOGS};
        this.cooldown = 5;
        this.category = new Category("Tools");
        StringBuilder sb = new StringBuilder("\n\nValid actions: `");
        for (ActionType type : ActionType.values()) {
            sb.append(type.name()).append("`, `");
        }

        actions = sb.toString().substring(0, sb.length() - 3);
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] parts = event.getArgs().split("\\s+", 2);
        AuditLogPaginationAction action = event.getGuild().retrieveAuditLogs().cache(false).limit(10);
        switch (parts[0].toLowerCase()) {
            case "all":
                break;
            case "from":
                if (parts.length == 1) {
                    throw new CommandErrorException("Please include a user");
                }

                List<User> list = FinderUtil.findUsers(parts[1], event.getJDA());
                if (list.isEmpty()) {
                    throw new CommandWarningException("No users found matching `" + parts[1] + "`");
                }

                if (list.size() > 1) {
                    throw new CommandWarningException(FormatUtil.listOfUser(list, event.getArgs()));
                }

                action = action.user(list.get(0));
                break;
            case "action":
                if (parts.length == 1) {
                    throw new CommandErrorException("Please include an action" + actions);
                }

                ActionType type = null;
                for (ActionType t : ActionType.values()) {
                    if (t.name().toLowerCase().replace("_", "").equals(parts[1].toLowerCase().replace("_", "").replace(" ", ""))) {
                        type = t;
                        break;
                    }
                }

                if (type == null) {
                    throw new CommandErrorException("Please include a valid action" + actions);
                }

                action = action.type(type);
                break;
            default:
                throw new CommandErrorException("Valid subcommands:\n\n" + "`" + event.getClient().getPrefix() + name + " all` - shows recent audit log entries\n" + "`" + event.getClient().getPrefix() + name + " from <user>` - shows recent entries by a user\n" + "`" + event.getClient().getPrefix() + name + " action <action>` shows recent entries of a certain action");
        }

        event.getChannel().sendTyping().queue();
        action.queue(list -> {
            if (list.isEmpty()) {
                event.replyWarning("No audit log entries found matching your criteria");
                return;
            }

            EmbedBuilder eb = new EmbedBuilder().setColor(event.getSelfMember().getColor());
            list.forEach(ale -> {
                StringBuilder sb = new StringBuilder();
                sb.append(LINESTART).append("User: ").append(FormatUtil.formatFullUser(ale.getUser()));
                switch (ale.getTargetType()) {
                    case CHANNEL:
                        TextChannel tc = event.getGuild().getTextChannelById(ale.getTargetIdLong());
                        sb.append("\n").append(LINESTART).append("Channel: ").append(tc == null ? UNKNOWN : "**#" + tc.getName() + "**").append(" (ID:").append(ale.getTargetId()).append(")");
                        break;
                    case EMOJI:
                        Emoji e = event.getGuild().getEmojiById(ale.getTargetIdLong());
                        sb.append("\n").append(LINESTART).append("Emote: ").append(e == null ? UNKNOWN : e.getFormatted()).append(" (ID:").append(ale.getTargetId()).append(")");
                        break;
                    case GUILD:
                        break;
                    case INVITE:
                        break;
                    case MEMBER:
                        User u = event.getJDA().getUserById(ale.getTargetIdLong());
                        sb.append("\n").append(LINESTART).append("Member: ").append(u == null ? UNKNOWN : FormatUtil.formatUser(u)).append(" (ID:").append(ale.getTargetId()).append(")");
                        break;
                    case ROLE:
                        Role r = event.getGuild().getRoleById(ale.getTargetIdLong());
                        sb.append("\n").append(LINESTART).append("Role: ").append(r == null ? UNKNOWN : "**" + r.getName() + "**").append(" (ID:").append(ale.getTargetId()).append(")");
                        break;
                    case WEBHOOK:
                        sb.append("\n").append(LINESTART).append("Webhook ID: ").append(ale.getTargetId());
                        break;
                    case UNKNOWN:
                    default:
                        sb.append("\n").append(LINESTART).append("Target ID: ").append(ale.getTargetId());
                }

                ale.getChanges().keySet().forEach(change -> {
                    AuditLogChange alc = ale.getChangeByKey(change);
                    if (alc != null) {
                        sb.append("\n").append(LINESTART).append(fixCase(change)).append(": ").append(alc.getOldValue() == null ? "" : "**" + alc.getOldValue() + "**").append(alc.getOldValue() == null || alc.getNewValue() == null ? "" : " → ").append(alc.getNewValue() == null ? "" : "**" + alc.getNewValue() + "**");
                    }
                });
                ale.getOptions().keySet().forEach(option -> {
                    sb.append("\n").append(LINESTART).append(fixCase(option)).append(": ").append(ale.getOptionByName(option) == null ? UNKNOWN : "**" + ale.getOptionByName(option) + "**");
                });
                if (ale.getReason() != null) {
                    sb.append("\n").append(LINESTART).append("Reason: ").append(ale.getReason());
                }

                sb.append("\n").append(LINESTART).append("Time: **").append(ale.getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("**\n\u200B");
                String str = sb.length() > 1024 ? sb.substring(0, 1020) + " ..." : sb.toString();
                eb.addField(actionToEmote(ale.getType()) + " " + fixCase(ale.getType().name()), str, true);
            });
            event.reply(new MessageCreateBuilder().setContent(event.getClient().getSuccess() + " Recent Audit Logs in **" + FormatUtil.filterEveryone(event.getGuild().getName()) + "**:").addEmbeds(eb.build()).build());
        }, f -> event.replyWarning("Failed to retrieve audit logs"));
    }

    private String fixCase(String input) {
        String ret = "";
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '_') {
                ret += " ";
            } else if (i == 0 || input.charAt(i - 1) == '_') {
                ret += Character.toUpperCase(input.charAt(i));
            } else {
                ret += Character.toLowerCase(input.charAt(i));
            }
        }

        return ret;
    }

    private String actionToEmote(ActionType type) {
        return switch (type) {
            case BAN -> "<:Ban:274789152304660481>";
            case KICK -> "<:DeleteUser:274789150920671232>";
            case UNBAN -> "<:Unban:274789150689984513>";
            case PRUNE -> "<:removeMember:417574812718858250>";
            case CHANNEL_CREATE -> "<:addChannel:417574812425125888>";
            case CHANNEL_DELETE -> "<:deleteChannel:417574812622258186>";
            case CHANNEL_UPDATE -> "<:updateChannel:417574812240576514>";
            case CHANNEL_OVERRIDE_CREATE -> "<:addChannel:417574812425125888>";
            case CHANNEL_OVERRIDE_DELETE -> "<:deleteChannel:417574812622258186>";
            case CHANNEL_OVERRIDE_UPDATE -> "<:updateChannel:417574812240576514>";
            case EMOJI_CREATE -> "<:createEmoji:417574812689498112>";
            case EMOJI_DELETE -> "<:deleteEmoji:417574812521725962>";
            case EMOJI_UPDATE -> "<:updateEmoji:417574812601548800>";
            case GUILD_UPDATE -> "<:updateServer:417574812534177793>";
            case INVITE_CREATE -> "<:addInvite:417574812517662721>";
            case INVITE_DELETE -> "<:deleteInvite:417574812521725953>";
            case INVITE_UPDATE -> "<:updateInvite:417574812429320192>";
            case MEMBER_ROLE_UPDATE -> "<:updateMember:417574812504948736>";
            case MEMBER_UPDATE -> "<:updateMember:417574812504948736>";
            case MESSAGE_CREATE -> "<:createMessage:446853279926845452>";
            case MESSAGE_DELETE -> "<:deleteMessage:417574812399960065>";
            case MESSAGE_UPDATE -> "<:updateMessage:446853280308396032>";
            case ROLE_CREATE -> "<:createRole:417574812399960075>";
            case ROLE_DELETE -> "<:deleteRole:417574812463136769>";
            case ROLE_UPDATE -> "<:updateRole:417574812165210126>";
            case WEBHOOK_CREATE -> "<:createWebhook:417574812714532864>";
            case WEBHOOK_REMOVE -> "<:deleteWebhook:417574812098101250>";
            case WEBHOOK_UPDATE -> "<:updateWebhook:417574812534439946>";
            case UNKNOWN -> "\u2753"; // ❓
            default -> "\u2753"; // ❓
        };
    }
}
