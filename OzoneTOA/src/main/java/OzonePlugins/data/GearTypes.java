package OzonePlugins.data;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum GearTypes
{
    RANGE("range"),
    MAGIC("magic"),
    MELEE("melee"),
    ;

    private final String name;
    @Setter
    private GearSetup setup;

    public static void setRangedMeleePhaseGear(GearSetup gearSetup)
    {
        RANGE.setSetup(gearSetup);
    }

    public static void setMagePhaseGear(GearSetup gearSetup)
    {
        MAGIC.setSetup(gearSetup);
    }

    public static void setMeleePhaseGear(GearSetup gearSetup)
    {
        MELEE.setSetup(gearSetup);
    }

}
