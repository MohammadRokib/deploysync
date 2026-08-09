package com.deploysync.model.patchbuilder;

import java.util.List;

public record ArchiveNode(String name, List<String> segments, boolean directory, boolean archive) {}
