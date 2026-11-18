package sg.pilot.halalscan;

public final class VerdictCheck {
    public static void main(String[] args) {
        check("wheat, pork gelatin", true, null, Verdict.PROHIBITED, "pork gelatin");
        check("sugar, lard", true, null, Verdict.PROHIBITED, "lard");
        check("sugar, red wine", true, null, Verdict.PROHIBITED, "wine");
        check("gelatine, sugar", true, null, Verdict.CAUTION, "gelatine");
        check("ethanol, natural extract", true, null, Verdict.CAUTION, "ethanol");
        check("sorbitol (sugar alcohol), root beer", true, null, Verdict.CAUTION, "No flagged");
        check("rice, salt", true, null, Verdict.CAUTION, "No flagged");
        check("pork", false, null, Verdict.CAUTION, "needs review");
        check("", false, "Checked MUIS record", Verdict.CERTIFIED, "Checked MUIS record");
        check("lard", true, "Checked MUIS record", Verdict.CAUTION, "conflicts");
        System.out.println("10 verdict checks passed");
    }

    private static void check(String list, boolean complete, String certificate, String title, String reason) {
        Verdict.Result result = Verdict.assess(list, complete, certificate);
        if (!result.title().equals(title) || !result.reason().contains(reason)) {
            throw new AssertionError("Unexpected result for " + list + " → " + result);
        }
    }
}
