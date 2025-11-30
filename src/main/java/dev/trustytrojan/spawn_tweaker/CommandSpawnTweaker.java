package dev.trustytrojan.spawn_tweaker;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandSpawnTweaker extends CommandBase {
    @Override
    public String getName() {
        return "spawntweaker";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/spawntweaker import | /spawntweaker export <glob> [<glob> ...]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponentString("Usage: /spawntweaker import | /spawntweaker export <modid>"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "import":
                SpawnTweaker.importMonsterSpawnData();
                sender.sendMessage(new TextComponentString("Monster spawn data imported."));
                break;
            case "export":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /spawntweaker export <glob> [<glob> ...]"));
                    return;
                }
                java.util.List<String> patterns = java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 1, args.length));
                SpawnTweaker.exportMonsterSpawnData(patterns);
                sender.sendMessage(new TextComponentString("Monster spawn data exported for patterns: " + String.join(", ", patterns)));
                break;
            default:
                sender.sendMessage(new TextComponentString("Unknown subcommand: " + args[0]));
                break;
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }
}
