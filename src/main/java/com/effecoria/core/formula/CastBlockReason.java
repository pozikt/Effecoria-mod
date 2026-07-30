package com.effecoria.core.formula;

/** Player-facing reason a spell cannot be cast. */
public enum CastBlockReason {
    ZERO_FLUX,
    WRONG_SCHOOL,
    LOW_PHI,
    LOW_PSI,
    LOW_MASTERY,
    LOW_POWER;

    public String messageKey() {
        return switch (this) {
            case ZERO_FLUX -> "message.effecoria.cast_blocked_znphi";
            case WRONG_SCHOOL -> "message.effecoria.cast_blocked_school";
            case LOW_PHI -> "message.effecoria.cast_blocked_phi";
            case LOW_PSI -> "message.effecoria.cast_blocked_psi";
            case LOW_MASTERY -> "message.effecoria.cast_blocked_mastery";
            case LOW_POWER -> "message.effecoria.cast_blocked_power";
        };
    }
}
