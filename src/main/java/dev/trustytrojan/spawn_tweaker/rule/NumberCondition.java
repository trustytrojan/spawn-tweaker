package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;

/**
 * Represents a numerical condition with optional at_least, at_most, and between constraints.
 */
public class NumberCondition
{
	public Double atLeast; // minimum inclusive value
	public Double atMost; // maximum inclusive value
	public Double[] between; // [min, max] both inclusive

	public NumberCondition()
	{}

	public NumberCondition(Double atLeast, Double atMost, Double[] between)
	{
		this.atLeast = atLeast;
		this.atMost = atMost;
		this.between = between;
	}

	/**
	 * Check if the given value satisfies all constraints in this condition. Returns true if the value meets all
	 * specified constraints.
	 */
	public boolean matches(final double value)
	{
		if (atLeast != null && value < atLeast)
			return false;
		if (atMost != null && value > atMost)
			return false;
		if (between != null && between.length >= 2)
		{
			final double min = between[0];
			final double max = between[1];
			if (value < min || value > max)
				return false;
		}
		return true;
	}

	/**
	 * Parse a NumberCondition from YAML data. Expects a Map with optional keys: at_least, at_most, between Returns null
	 * if the input is null or empty.
	 */
	@SuppressWarnings("unchecked")
	public static NumberCondition fromYaml(final Object raw)
	{
		if (raw == null)
			return null;

		// Expected format: Map with at_least, at_most, between keys
		if (raw instanceof Map)
		{
			final var map = (Map<String, Object>) raw;
			final var condition = new NumberCondition();

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
		final var sb = new StringBuilder("NumberCondition{");
		if (atLeast != null)
			sb.append("atLeast=").append(atLeast).append(", ");
		if (atMost != null)
			sb.append("atMost=").append(atMost).append(", ");
		if (between != null && between.length >= 2)
			sb.append("between=[").append(between[0]).append(", ").append(between[1]).append("], ");
		if (sb.length() > "NumberCondition{".length())
			sb.setLength(sb.length() - 2); // remove trailing ", "
		sb.append("}");
		return sb.toString();
	}
}
