package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

public final class GlobUtils
{
    private GlobUtils() {}

    public static String globToRegex(String glob)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++)
        {
            char c = glob.charAt(i);
            switch (c)
            {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                case '\\': sb.append("\\\\"); break;
                case '+': case '(' : case ')': case '|': case '^': case '$': case '[': case ']': case '{': case '}':
                    sb.append('\\').append(c); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean matchesAnyCompiled(String key, java.util.List<Pattern> patterns)
    {
        if (patterns == null || patterns.isEmpty()) return false;
        for (Pattern pat : patterns)
        {
            if (pat.matcher(key).matches()) return true;
        }
        return false;
    }

    public static Biome[] resolveBiomesFromGlobs(List<String> biomeKeys)
    {
        if (biomeKeys == null || biomeKeys.isEmpty())
            return new Biome[0];

        if (biomeKeys.contains("*"))
        {
            Biome[] all = new Biome[Biome.REGISTRY.getKeys().size()];
            int i = 0;
            for (Biome b : Biome.REGISTRY) all[i++] = b;
            return all;
        }

        LinkedHashSet<Biome> matched = new LinkedHashSet<>();
        List<Pattern> patterns = new ArrayList<>();
        for (String key : biomeKeys)
        {
            patterns.add(Pattern.compile(globToRegex(key)));
        }

        for (ResourceLocation rl : Biome.REGISTRY.getKeys())
        {
            String name = rl.toString();
            for (Pattern p : patterns)
            {
                if (p.matcher(name).matches())
                {
                    Biome b = Biome.REGISTRY.getObject(rl);
                    if (b != null) matched.add(b);
                    break;
                }
            }
        }
        return matched.toArray(new Biome[matched.size()]);
    }
}
