package sg.pilot.halalscan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Verdict {
    static final String CAUTION = "Proceed with caution";
    static final String CERTIFIED = "Halal certified";
    static final String PROHIBITED = "Not halal";

    private static final Pattern EXCLUSIONS = Pattern.compile(
        "\\b(?:alcohol|pork|lard)[- ]free\\b|\\bnon[- ]alcoholic\\b|"
        + "\\b(?:wine|beer) vinegar\\b|\\bsugar alcohols?\\b|\\broot beer\\b",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN = Pattern.compile(
        "\\b(?:pork(?:[- ]derived)?(?: gelatine?)?|lard|bacon|daging babi|lemak babi|"
        + "minyak babi|gelatin babi|wine|beer|rum|vodka|whisk(?:e)?y|brandy|sake)\\b(?![- ]flavou?ring\\b|[- ]flavou?r\\b)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern AMBIGUOUS = Pattern.compile(
        "\\b(?:gelatine?|e441|e471|e472[a-f]?|ethanol|alcohol|alkohol|etanol|ham|"
        + "emulsifiers?|enzymes?|animal fats?|shortening|rennet|lecithin|flavou?r(?:ings?)?|mirin|chicken|beef|poultry|"
        + "daging ayam|daging sapi|daging lembu)\\b", Pattern.CASE_INSENSITIVE);

    record Result(String title, String reason, String next) {}

    static Result assess(String ingredients, boolean complete, String certificate) {
        String confirmed = complete ? ingredients.trim() : "";
        String neutralized = EXCLUSIONS.matcher(confirmed).replaceAll(" ");
        Matcher prohibited = FORBIDDEN.matcher(neutralized);
        if (prohibited.find()) {
            String hit = prohibited.group();
            if (certificate != null) {
                return new Result(CAUTION, "The label lists “" + hit + "” and conflicts with the recorded certificate.",
                    "Recheck the exact pack and the official record.");
            }
            return new Result(PROHIBITED, "The ingredient list includes “" + hit + "”.",
                "Choose another product.");
        }
        if (certificate != null) {
            return new Result(CERTIFIED, certificate, "Open the official directory to review the record.");
        }
        if (confirmed.isEmpty()) {
            return new Result(CAUTION, "Certification is unverified and the ingredient list needs review.",
                "Capture or enter the ingredient list and confirm it is complete.");
        }
        Matcher ambiguous = AMBIGUOUS.matcher(neutralized);
        if (ambiguous.find()) {
            return new Result(CAUTION, "The status of “" + ambiguous.group() + "” could not be determined from the label.",
                "Check the official directory or ask the manufacturer about this ingredient.");
        }
        return new Result(CAUTION, "No flagged ingredients found in the reviewed list. Certification is unverified.",
            "Use the official directory if you need certification.");
    }

    private Verdict() {}
}
