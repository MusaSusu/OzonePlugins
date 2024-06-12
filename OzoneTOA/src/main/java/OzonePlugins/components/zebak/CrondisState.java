package OzonePlugins.components.zebak;


import lombok.AccessLevel;
import lombok.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum CrondisState {
    START("start"),
    START_TILE("start tile"),
    WATER_CONTAINER ("water container"),
    FIRST_TILE("first tile"),
    WATERFALL("water fall"),
    FIRST_PALM("first palm"),
    SECOND_WATERFALL("second waterfall"),
    SECOND_PALM("second palm"),
    ;

    private final String name;
}
