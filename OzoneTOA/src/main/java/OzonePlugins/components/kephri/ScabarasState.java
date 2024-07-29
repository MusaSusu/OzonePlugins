package OzonePlugins.components.kephri;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ScabarasState {

    START("Start"),
    END("End")
        ;

    private final String name;
}
