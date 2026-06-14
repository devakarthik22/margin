package com.margin.scm;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the right {@link ScmProvider} for a host. Spring injects every
 * provider bean; the factory indexes them by {@link ScmProviderType}. This keeps
 * provider selection in one place and open for extension (Open/Closed): a new
 * provider is discovered automatically with no change here.
 */
@Component
public class ScmProviderFactory {

    private final Map<ScmProviderType, ScmProvider> providers;

    public ScmProviderFactory(List<ScmProvider> beans) {
        this.providers = beans.stream()
                .collect(Collectors.toMap(ScmProvider::type, Function.identity()));
    }

    public ScmProvider get(ScmProviderType type) {
        ScmProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No SCM provider configured for " + type);
        }
        return provider;
    }
}
