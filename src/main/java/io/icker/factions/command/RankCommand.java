package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class RankCommand implements Command {
    private int promote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot promote yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = User.get(player.getUuid()).getFaction();

        for (User users : faction.getUsers())
            if (users.getID().equals(target.getUuid())) {

                switch (users.getRank()) {
                    case MEMBER -> users.changeRank(User.Rank.COMMANDER);
                    case COMMANDER -> users.changeRank(User.Rank.LEADER);
                    case LEADER -> {
                        new Message("You cannot promote a member to owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                    case OWNER -> {
                        new Message("You cannot promote the owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message("Promoted " + target.getName().getString() + " to " + User.get(target.getUuid()).getRank().name().toLowerCase().replace("_", " ")).send(player, false);
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);
        return 0;
    }

    private int demote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot demote yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = User.get(player.getUuid()).getFaction();

        for (User user : faction.getUsers())
            if (user.getID().equals(target.getUuid())) {

                switch (user.getRank()) {
                    case MEMBER -> {
                        new Message("You cannot demote a civilian").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                    case COMMANDER -> user.changeRank(User.Rank.MEMBER);
                    case LEADER -> {
                        if (User.get(player.getUuid()).getRank() == User.Rank.LEADER) {
                            new Message("You cannot demote a fellow co-owner").format(Formatting.RED).send(player, false);
                            return 0;
                        }

                        user.changeRank(User.Rank.COMMANDER);
                    }
                    case OWNER -> {
                        new Message("You cannot demote the owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message("Demoted " + target.getName().getString() + " to " + User.get(target.getUuid()).getRank().name().toLowerCase().replace("_", " ")).send(player, false);
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);
        return 0;
    }

    private int transfer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot transfer ownership to yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = User.get(player.getUuid()).getFaction();

        for (User user : faction.getUsers()) // TODO change this. Why do we need to iterate a factions users?? so inefficent 
            if (user.getID().equals(target.getUuid())) {

                user.changeRank(User.Rank.OWNER);
                User.get(player.getUuid()).changeRank(User.Rank.LEADER);

                context.getSource().getServer().getPlayerManager().sendCommandTree(player);
                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message("Transferred ownership to " + target.getName().getString()).send(player, false);
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);

        return 0;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("rank")
            .requires(Requires.hasPerms("factions.rank", 0))
            .requires(Requires.isLeader())
            .then(
                CommandManager
                .literal("promote")
                .requires(Requires.hasPerms("factions.rank.promote", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::promote)
                )
            )
            .then(
                CommandManager
                .literal("demote")
                .requires(Requires.hasPerms("factions.rank.demote", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::demote)
                )
            )
            .then(
                CommandManager
                .literal("transfer")
                .requires(Requires.hasPerms("factions.transfer", 0))
                .requires(Requires.isOwner())
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::transfer)
                )
            )
            .build();
    }
}
