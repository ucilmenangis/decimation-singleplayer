package net.decimation.worldgen;

/**
 * Zone kinds our code places or checks, by their readable names. Decimation's
 * own enum is obfuscated (net.decimation.mod.server.zones.a, EnumZoneType);
 * {@link net.decimation.fixes.Deci#zoneType(ZoneKind)} converts where a zone
 * is handed to Decimation.
 */
public enum ZoneKind
{
    MILITARY, POLICE, SAFEZONE
}
