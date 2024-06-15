package OzonePlugins.components.zebak;


import lombok.AccessLevel;
import lombok.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum CrondisState {
    START("start"),
    WATER_CONTAINER ("water container"),
    FIRST_TILE("first tile"),
    WATERFALL("water fall"),
    FIRST_PALM_RUN("first run"),
    FIRST_PALM("first palm"),
    SECOND_TILE("second tile"),
    SECOND_WATERFALL("second waterfall"),
    SECOND_PALM_RUN("second run"),
    SECOND_PALM("second palm"),
    END("END")
    ;

    private final String name;
}
