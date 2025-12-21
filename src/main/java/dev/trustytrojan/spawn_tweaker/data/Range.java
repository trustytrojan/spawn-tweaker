package dev.trustytrojan.spawn_tweaker.data;

import java.util.List;

import dev.trustytrojan.spawn_tweaker.Util;

public class Range
{
	private enum Comparator
	{
		AT_LEAST,
		AT_MOST,
		BETWEEN,
		NONE // all fields null
	}

	// Raw data (from JSON)
	public Integer at_least;
	public Integer at_most;
	public List<Integer> between;

	// Compiled data
	private transient Comparator type = Comparator.NONE;
	private transient int value1;
	private transient int value2;

	public void compile()
	{
		if (between != null && between.size() >= 2)
		{
			type = Comparator.BETWEEN;
			value1 = between.get(0);
			value2 = between.get(1);
		}
		else if (at_least != null)
		{
			type = Comparator.AT_LEAST;
			value1 = at_least;
		}
		else if (at_most != null)
		{
			type = Comparator.AT_MOST;
			value1 = at_most;
		}
		else
		{
			type = Comparator.NONE;
		}

		// Validate mutual exclusivity
		if (!Util.mutuallyExclusive(at_least != null, at_most != null, between != null))
			throw new IllegalArgumentException("range checkers are mutually exclusive");
	}

	public boolean check(final int value)
	{
		return check(value, 1);
	}

	public boolean check(final int value, final int scaleFactor)
	{
		return switch (type)
		{
		case AT_LEAST -> value >= value1 * scaleFactor;
		case AT_MOST -> value <= value1 * scaleFactor;
		case BETWEEN -> value >= value1 * scaleFactor && value <= value2 * scaleFactor;
		case NONE -> true;
		};
	}
}
