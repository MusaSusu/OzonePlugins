package ozone.zulrah.rotationutils;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import ozone.zulrah.data.StandLocation;
import ozone.zulrah.data.ZulrahLocation;
import ozone.zulrah.data.ZulrahType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum RotationType
{
	ROT_A("Rotation A", ImmutableList.of(
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 28,9,0),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 21,2,3),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 18,4,3),
			add(ZulrahType.RANGE, ZulrahLocation.SOUTH, StandLocation.WEST_PILLAR_N, StandLocation.WEST_PILLAR_N2, Prayer.PROTECT_FROM_MISSILES, 39,11,0),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.WEST_PILLAR_N, (StandLocation) null, (Prayer) null, 22,2,2),
			add(ZulrahType.MAGIC, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 20,5,3),
			add(ZulrahType.RANGE, ZulrahLocation.SOUTH, StandLocation.EAST_PILLAR, (StandLocation) null, (Prayer) null, 28,7,0),
			add(ZulrahType.MAGIC, ZulrahLocation.SOUTH, StandLocation.EAST_PILLAR, StandLocation.EAST_PILLAR_N2, Prayer.PROTECT_FROM_MAGIC, 36,4,0,true),
			addJad(ZulrahType.RANGE, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_S, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MISSILES, 48,10,0),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 21,2,0))),
	ROT_B("Rotation B", ImmutableList.of(
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 28,9,0),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 21,2,3),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 18,4,3),
			add(ZulrahType.RANGE, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_N, (StandLocation) null, (Prayer) null, 28,7,0),
			add(ZulrahType.MAGIC, ZulrahLocation.SOUTH, StandLocation.WEST_PILLAR_N, StandLocation.WEST_PILLAR_N2, Prayer.PROTECT_FROM_MAGIC, 39,11,0), //special case for blowpipe where to stand
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.WEST_PILLAR_N, (StandLocation) null, (Prayer) null, 21,2,3),
			add(ZulrahType.RANGE, ZulrahLocation.EAST, StandLocation.CENTER, StandLocation.WEST_PILLAR_S, Prayer.PROTECT_FROM_MISSILES, 20,4,2), //counts an attack as turning to face?
			add(ZulrahType.MAGIC, ZulrahLocation.SOUTH, StandLocation.WEST_PILLAR_S, StandLocation.WEST_PILLAR_N2, Prayer.PROTECT_FROM_MAGIC, 36,10,0),
			addJad(ZulrahType.RANGE, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_S, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MISSILES, 48,10,0),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 21,2,0))),
	ROT_C("Rotation C", ImmutableList.of(
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 28,9,0),
			add(ZulrahType.RANGE, ZulrahLocation.EAST, StandLocation.NORTHEAST_TOP, (StandLocation) null, Prayer.PROTECT_FROM_MISSILES, 30,8,3),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.WEST, (StandLocation) null, (Prayer) null, 40,8,0),
			add(ZulrahType.MAGIC, ZulrahLocation.WEST, StandLocation.WEST, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 20,5,4),
			add(ZulrahType.RANGE, ZulrahLocation.SOUTH, StandLocation.EAST_PILLAR_S, StandLocation.EAST_PILLAR_N2, Prayer.PROTECT_FROM_MISSILES, 20,5,3),
			add(ZulrahType.MAGIC, ZulrahLocation.EAST, StandLocation.EAST_PILLAR_S, StandLocation.WEST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 20,5,3),
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.WEST_PILLAR_N, (StandLocation) null, (Prayer) null, 25,6,0),
			add(ZulrahType.RANGE, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_N, (StandLocation) null, Prayer.PROTECT_FROM_MISSILES, 20,5,3),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 36,10,3),
			addJad(ZulrahType.MAGIC, ZulrahLocation.EAST, StandLocation.EAST_PILLAR_N, (StandLocation) null, Prayer.PROTECT_FROM_MAGIC, 35,10,0),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 18,4,0))),
	ROT_D("Rotation D", ImmutableList.of(
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 28,9,0),
			add(ZulrahType.MAGIC, ZulrahLocation.EAST, StandLocation.NORTHEAST_TOP, (StandLocation) null, Prayer.PROTECT_FROM_MAGIC, 36,10,2),
			add(ZulrahType.RANGE, ZulrahLocation.SOUTH, StandLocation.WEST_PILLAR_N, StandLocation.WEST_PILLAR_N2, Prayer.PROTECT_FROM_MISSILES, 24,6,0),
			add(ZulrahType.MAGIC, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_N, (StandLocation) null, Prayer.PROTECT_FROM_MAGIC, 30,8,2),
			add(ZulrahType.MELEE, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, (StandLocation) null, (Prayer) null, 28,4,2),
			add(ZulrahType.RANGE, ZulrahLocation.EAST, StandLocation.EAST_PILLAR, (StandLocation) null, Prayer.PROTECT_FROM_MISSILES, 17,4,0),
			add(ZulrahType.RANGE, ZulrahLocation.SOUTH, StandLocation.EAST_PILLAR, (StandLocation) null, (Prayer) null, 34,5,0,true),
			add(ZulrahType.MAGIC, ZulrahLocation.WEST, StandLocation.WEST_PILLAR_S, (StandLocation) null, Prayer.PROTECT_FROM_MAGIC, 33,9,3),
			add(ZulrahType.RANGE, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MISSILES, 20,5,0),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.EAST_PILLAR_N, StandLocation.EAST_PILLAR_S, Prayer.PROTECT_FROM_MAGIC, 27,7,0),
			addJad(ZulrahType.MAGIC, ZulrahLocation.EAST, StandLocation.EAST_PILLAR_N, (StandLocation) null, Prayer.PROTECT_FROM_MAGIC, 29,8,0),
			add(ZulrahType.MAGIC, ZulrahLocation.NORTH, StandLocation.NORTHEAST_TOP, (StandLocation) null, (Prayer) null, 18,4,0)));

	private static final List<RotationType> lookup = new ArrayList<>();

	static
	{
		lookup.addAll(EnumSet.allOf(RotationType.class));
	}

	private final String rotationName;
	private final List<ZulrahPhase> zulrahPhases;

	public static List<RotationType> findPotentialRotations(NPC npc, int stage)
	{
		return lookup.stream().filter(type -> type.getZulrahPhases().get(stage).getZulrahNpc().equals(ZulrahNpc.valueOf(npc, false))).collect(Collectors.toList());
	}

	private static ZulrahPhase add(ZulrahType type, ZulrahLocation zulrahLocation, StandLocation standLocation, StandLocation stallLocation, Prayer prayer, int phaseTicks,int phaseAttacks,int ticksToMove)
	{
		return new ZulrahPhase(new ZulrahNpc(type, zulrahLocation, false), new ZulrahAttributes(standLocation, stallLocation, prayer, phaseTicks,phaseAttacks,ticksToMove,false));
	}
	private static ZulrahPhase add(ZulrahType type, ZulrahLocation zulrahLocation, StandLocation standLocation, StandLocation stallLocation, Prayer prayer, int phaseTicks,int phaseAttacks,int ticksToMove,boolean shouldAttack)
	{
		return new ZulrahPhase(new ZulrahNpc(type, zulrahLocation, false), new ZulrahAttributes(standLocation, stallLocation, prayer, phaseTicks,phaseAttacks,ticksToMove,true));
	}

	private static ZulrahPhase addJad(ZulrahType type, ZulrahLocation zulrahLocation, StandLocation standLocation, StandLocation stallLocation, Prayer prayer, int phaseTicks,int phaseAttacks,int ticksToMove)
	{
		return new ZulrahPhase(new ZulrahNpc(type, zulrahLocation, true), new ZulrahAttributes(standLocation, stallLocation, prayer, phaseTicks,phaseAttacks,ticksToMove,false));
	}

	public String getRotationName()
	{
		return this.rotationName;
	}

	public List<ZulrahPhase> getZulrahPhases()
	{
		return this.zulrahPhases;
	}

	public String toString()
	{
		return this.rotationName;
	}
}
