package com.margin.agent.tool;

import com.margin.agent.FileContextProvider;
import org.springframework.ai.tool.annotation.Tool;

/**
 * Exposes a single tool to the model: fetch a file's full contents. This is what
 * makes Margin an *agent* rather than a one-shot prompt — the model decides when
 * a hunk needs surrounding context and calls back for it, instead of us
 * pre-stuffing the whole repo into the prompt.
 *
 * A fresh instance is created per review and bound to that review's
 * {@link FileContextProvider}, so the tool always reads from the correct PR head.
 */
public class FileContextTool {

    private final FileContextProvider provider;

    public FileContextTool(FileContextProvider provider) {
        this.provider = provider;
    }

    @Tool(description = "Fetch the full current contents of a file in the pull request "
            + "by its repository-relative path. Use when a diff hunk lacks enough "
            + "surrounding context to judge correctness.")
    public String getFileContent(String path) {
        try {
            return provider.getFileContent(path);
        } catch (Exception e) {
            return "ERROR: could not read '" + path + "': " + e.getMessage();
        }
    }
}
