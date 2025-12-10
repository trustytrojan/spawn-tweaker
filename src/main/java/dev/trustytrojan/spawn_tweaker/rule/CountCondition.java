package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;

/**
 * Represents an entity count condition with optional at_least, at_most, and between constraints. Additionally supports
 * a 'per' key to specify how the count is calculated (e.g., "chunk" or "world").
 */
public class CountCondition extends NumberCondition
{
	public String per; // "chunk", "world", or null for default behavior

	public CountCondition()
	{
		super();
	}

	public CountCondition(Double atLeast, Double atMost, Double[] between, String per)
	{
		super(atLeast, atMost, between);
		this.per = per;
	}

	/**
	 * Returns true if counting should be per-chunk.
	 */
	public boolean isPerChunk()
	{
		return per != null && per.equalsIgnoreCase("chunk");
	}

	/**
	 * Returns true if counting should be per-world (global).
	 */
	public boolean isPerWorld()
	{
		return per == null || per.equalsIgnoreCase("world");
	}

	/**
	 * Parse a CountCondition from YAML data. Expects a Map with keys: at_least, at_most, between (inherited from
	 * NumberCondition), and optionally: per (for "chunk" or "world" counting). Returns null if the input is null or
	 * empty.
	 */
	@SuppressWarnings("unchecked")
	public static CountCondition fromYaml(final Object raw)
	{
		if (raw == null)
			return null;

		if (raw instanceof Map)
		{
			final var map = (Map<String, Object>) raw;
			final var condition = new CountCondition();

			// Parse the number constraints (at_least, at_most, between)
			if (map.containsKey("at_least"))
			{
				final var val = map.get("at_least");
				if (val instanceof Number)
					condition.atLeast = ((Number) val).doubleValue();
			}

			if (map.containsKey("at_most"))
			{
				final var val = map.get("at_most");
				if (val instanceof Number)
					condition.atMost = ((Number) val).doubleValue();
			}

			if (map.containsKey("between"))
			{
				final var val = map.get("between");
				if (val instanceof List)
				{
					final var list = (List<?>) val;
					if (list.size() >= 2 && list.get(0) instanceof Number && list.get(1) instanceof Number)
					{
						condition.between = new Double[] {
							((Number) list.get(0)).doubleValue(), ((Number) list.get(1)).doubleValue()
						};
					}
				}
			}

			// Parse the optional 'per' key
			if (map.containsKey("per"))
			{
				final var val = map.get("per");
				if (val instanceof String)
					condition.per = (String) val;
			}

			// Return the condition only if at least one constraint was set
			if (condition.atLeast != null || condition.atMost != null || condition.between != null)
				return condition;
			return null;
		}

		return null;
	}

	@Override
	public String toString()
	{
		final var sb = new StringBuilder("CountCondition{");
		if (atLeast != null)
			sb.append("atLeast=").append(atLeast).append(", ");
		if (atMost != null)
			sb.append("atMost=").append(atMost).append(", ");
		if (between != null && between.length >= 2)
			sb.append("between=[").append(between[0]).append(", ").append(between[1]).append("], ");
		if (per != null)
			sb.append("per=").append(per).append(", ");
		if (sb.length() > "CountCondition{".length())
			sb.setLength(sb.length() - 2); // remove trailing ", "
		sb.append("}");
		return sb.toString();
	}
}
