package com.margin.agent;

/**
 * Port the agent uses to pull additional source on demand. Implemented by an
 * adapter over the active {@link com.margin.scm.ScmProvider}. Keeps the agent
 * decoupled from how/where files actually come from.
 */
@FunctionalInterface
public interface FileContextProvider {
    String getFileContent(String path);
}
