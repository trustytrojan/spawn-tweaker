package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.world.biome.Biome;

public final class GlobUtils
{
    private GlobUtils() {}

    public static String globToRegex(final String glob)
    {
        final var sb = new StringBuilder();
        for (var i = 0; i < glob.length(); i++)
        {
            final var c = glob.charAt(i);
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

    public static boolean matchesAnyCompiled(final String key, final java.util.List<Pattern> patterns)
    {
        if (patterns == null || patterns.isEmpty()) return false;
        for (final var pat : patterns)
        {
            if (pat.matcher(key).matches()) return true;
        }
        return false;
    }

    public static Biome[] resolveBiomesFromGlobs(final List<String> biomeKeys)
    {
        if (biomeKeys == null || biomeKeys.isEmpty())
            return new Biome[0];

        if (biomeKeys.contains("*"))
        {
            final var all = new Biome[Biome.REGISTRY.getKeys().size()];
            var i = 0;
            for (final var b : Biome.REGISTRY) all[i++] = b;
            return all;
        }

        final var matched = new LinkedHashSet<>();
        final var patterns = new ArrayList<Pattern>();
        for (final var key : biomeKeys)
        {
            patterns.add(Pattern.compile(globToRegex(key)));
        }

        for (final var rl : Biome.REGISTRY.getKeys())
        {
            final var name = rl.toString();
            for (final var p : patterns)
            {
                if (p.matcher(name).matches())
                {
                    final var b = Biome.REGISTRY.getObject(rl);
                    if (b != null) matched.add(b);
                    break;
                }
            }
        }
        return matched.toArray(new Biome[matched.size()]);
    }
}
