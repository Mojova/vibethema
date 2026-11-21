package com.vibethema.model.mystic;

import com.google.gson.annotations.SerializedName;

/** Classification of a Charm, independent of its source book and purchase eligibility. */
public enum CharmKind {
    @SerializedName("exalted")
    EXALTED,
    @SerializedName("martialArts")
    MARTIAL_ARTS,
    @SerializedName("evocation")
    EVOCATION
}
