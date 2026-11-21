package com.vibethema.service;

import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.Charm;
import com.vibethema.model.mystic.CharmKind;
import java.util.ArrayList;
import java.util.List;

/** Homogeneous Charm collection; its metadata is inherited by every entry. */
public final class CharmCollection {
    public static final String VERSION = "0.3.0";
    public String version = VERSION;
    public String sourceId;
    public CharmKind kind;
    public ExaltType exaltType;
    public String id;
    public String ability;
    public String artifactId;
    public String artifactName;
    public List<Charm> charms = new ArrayList<>();
}
