package OzonePlugins.components.kephri.ScabarasPuzzle;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ScabarasState {

    START("Start"),
    ADDITION_PUZZLE("Addition Puzzle"),
    SEQUENCE_PUZZLE("Sequence Puzzle"),
    LIGHT_PUZZLE("Light Puzzle"),
    MATCHING_PUZZLE("Matching Puzzle"),
    END("End")
        ;

    private final String name;
}
