package com.investigator.jena;

public record ResolvedEntity(
        String searchKeyword,
        String entityUri,
        String entityLabel,
        String classUri,
        String classLabel,
        int sitelinks
) {}
