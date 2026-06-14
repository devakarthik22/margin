package com.margin.diff;

import com.margin.domain.model.CodeDiff;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The parser is a pure function, so it tests without any Spring context. */
class UnifiedDiffParserTest {

    private final UnifiedDiffParser parser = new UnifiedDiffParser();

    @Test
    void tracksNewLineNumbersForAddedLines() {
        String diff = """
            --- a/App.java
            +++ b/App.java
            @@ -10,2 +10,3 @@
             int a = 1;
            +int b = 2;
             int c = 3;
            """;

        CodeDiff result = parser.parse(diff);

        assertThat(result.files()).hasSize(1);
        var file = result.files().get(0);
        assertThat(file.path()).isEqualTo("App.java");
        // context line 10, added line 11, context line 12
        assertThat(file.addressableLines()).containsExactlyInAnyOrder(10, 11, 12);
    }

    @Test
    void emptyDiffYieldsNoFiles() {
        assertThat(parser.parse("").isEmpty()).isTrue();
    }
}
