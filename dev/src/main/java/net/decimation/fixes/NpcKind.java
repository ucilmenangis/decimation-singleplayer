package net.decimation.fixes;

/** Decimation's armed humans, as Deci.npcKind tells them apart. */
public enum NpcKind
{
    /** BanditEntity: hostile to players with humanity 50 and up. */
    BANDIT,
    /** SoldierEntity: hostile to players with humanity under 50. */
    SOLDIER,
    /** HazmatSoldierEntity (a soldier in a hazmat suit). */
    HAZMAT,
    /** SovietEntity: hostile to every player, the enemy military. */
    SOVIET
}
