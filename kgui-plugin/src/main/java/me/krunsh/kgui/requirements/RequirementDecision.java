package me.krunsh.kgui.requirements;

/** Regle centrale: absence, erreur ou inversion mal formee valent DENY. */
public final class RequirementDecision {
    private RequirementDecision() {
    }

    public static boolean resolve(Boolean outcome, Boolean inverted) {
        return resolve(outcome, inverted, true);
    }

    public static boolean resolve(Boolean outcome, Boolean inverted, boolean validEvaluation) {
        if (!validEvaluation || outcome == null || inverted == null) return false;
        return inverted ? !outcome : outcome;
    }
}
