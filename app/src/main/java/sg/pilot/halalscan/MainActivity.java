package sg.pilot.halalscan;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public final class MainActivity extends ComponentActivity {
    private static final int INK = 0xff142e25;
    private static final int PAPER = 0xfff6f5ed;
    private static final int MUTED = 0xff46594e;
    private static final String[] ISSUERS = {"MUIS", "JAKIM", "BPJPH"};
    private static final String[] DIRECTORIES = {
        "https://halal.muis.gov.sg/halal/products",
        "https://www.halal.gov.my/?lang=en",
        "https://bpjph.halal.go.id/cari/sertifikat"
    };

    private final ExecutorService requests = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private ScrollView scroll;
    private EditText product;
    private EditText ingredients;
    private TextView status;
    private TextView title;
    private TextView reason;
    private TextView next;
    private TextView source;
    private Button barcodeButton;
    private Button labelButton;
    private Button resetButton;
    private ActivityResultLauncher<Uri> camera;
    private File photo;
    private boolean barcodeMode;
    private boolean busy;
    private boolean complete;
    private String certificate;
    private int issuer = -1;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        androidx.activity.EdgeToEdge.enable(this);
        camera = registerForActivityResult(new ActivityResultContracts.TakePicture(), this::readPhoto);
        if (state != null) {
            String file = state.getString("photo");
            if (file != null) photo = new File(file);
            barcodeMode = state.getBoolean("barcodeMode");
        }
        buildScreen();
        if (state != null) {
            product.setText(state.getString("product", ""));
            ingredients.setText(state.getString("ingredients", ""));
            complete = state.getBoolean("complete");
            certificate = state.getString("certificate");
            issuer = state.getInt("issuer", -1);
            status.setText(state.getString("status", ""));
        }
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("product", value(product));
        state.putString("ingredients", value(ingredients));
        state.putString("status", status.getText().toString());
        state.putBoolean("complete", complete);
        state.putString("certificate", certificate);
        state.putInt("issuer", issuer);
        if (photo != null) state.putString("photo", photo.getAbsolutePath());
        state.putBoolean("barcodeMode", barcodeMode);
    }

    private void buildScreen() {
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(content);
        setContentView(scroll);

        text(content, "Halal Scanner", 32, INK, true);
        text(content, "Check a packaged food or drink", 16, MUTED, false);
        barcodeButton = button(content, "Scan barcode", true, () -> capture(true));
        labelButton = button(content, "Photograph ingredients", false, () -> capture(false));
        status = text(content, "", 15, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setBackground(background(0xffffffff));
        result.setPadding(dp(16), dp(12), dp(16), dp(16));
        add(content, result, 14);
        title = text(result, "", 24, INK, true);
        title.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        reason = text(result, "", 16, INK, false);
        next = text(result, "", 14, MUTED, false);
        source = text(result, "", 14, MUTED, false);
        source.setOnClickListener(view -> { if (issuer >= 0) open(DIRECTORIES[issuer]); });
        source.setFocusable(true);

        product = field("Product name and variant", false);
        button(content, "Check official directories", false, this::chooseDirectory);
        button(content, "Record certificate checked", false, this::recordCertificate);

        ingredients = field("Ingredients from the package", true);
        button(content, "Confirm complete list and check", false, () -> {
            if (value(ingredients).isEmpty()) {
                ingredients.setError("Enter or photograph the complete ingredient list.");
                return;
            }
            complete = true;
            render();
            scroll.smoothScrollTo(0, Math.max(0, title.getTop() + result.getTop() - dp(12)));
        });
        resetButton = button(content, "Start a new check", false, () -> {
            product.setText("");
            ingredients.setText("");
            status.setText("");
            render();
            scroll.smoothScrollTo(0, 0);
        });
        watch(product, () -> {
            certificate = null;
            issuer = -1;
            render();
        });
        watch(ingredients, () -> {
            complete = false;
            render();
        });
        content.requestFocus();
    }

    private void capture(boolean barcode) {
        if (busy || photo != null) return;
        barcodeMode = barcode;
        try {
            File scans = new File(getCacheDir(), "scans");
            if (!scans.isDirectory() && !scans.mkdirs()) throw new IOException("Cannot create photo folder");
            photo = File.createTempFile("label-", ".jpg", scans);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".photos", photo);
            camera.launch(uri);
        } catch (IOException | ActivityNotFoundException | SecurityException error) {
            discardPhoto();
            status.setText("Camera unavailable. Enter the product name or ingredients below.");
        }
    }

    private void readPhoto(boolean saved) {
        if (photo == null) return;
        if (!saved) {
            discardPhoto();
            return;
        }
        InputImage image;
        try {
            image = InputImage.fromFilePath(this, Uri.fromFile(photo));
        } catch (IOException | RuntimeException error) {
            discardPhoto();
            status.setText("Couldn’t read the photo. Try again in good light.");
            return;
        }
        discardPhoto();
        setBusy(true);
        status.setText(barcodeMode ? "Reading barcode…" : "Reading ingredient label…");
        if (barcodeMode) {
            var options = new BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build();
            var scanner = BarcodeScanning.getClient(options);
            scanner.process(image).addOnCompleteListener(task -> {
                scanner.close();
                if (isDestroyed()) return;
                Set<String> codes = new LinkedHashSet<>();
                if (task.isSuccessful()) {
                    for (Barcode barcode : task.getResult()) {
                        String code = barcode.getRawValue();
                        if (code != null && code.matches("[0-9]{8,13}")) codes.add(code);
                    }
                }
                if (codes.size() == 1) {
                    lookup(codes.iterator().next());
                } else {
                    setBusy(false);
                    status.setText("Frame one clear barcode, or enter the product name.");
                }
            });
        } else {
            var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image).addOnCompleteListener(task -> {
                recognizer.close();
                if (isDestroyed()) return;
                setBusy(false);
                if (task.isSuccessful() && !task.getResult().getText().trim().isEmpty()) {
                    ingredients.setText(task.getResult().getText());
                    status.setText("Review the scan. Keep only the complete ingredient list, then confirm it.");
                    ingredients.requestFocus();
                    scroll.post(() -> scroll.smoothScrollTo(0, ingredients.getTop() - dp(56)));
                } else {
                    status.setText("No readable text. Retake the photo or enter the list below.");
                }
            });
        }
    }

    private void lookup(String code) {
        // A new barcode starts a new product check. Clear evidence from the previous pack.
        product.setText("");
        ingredients.setText("");
        status.setText("Looking up barcode " + code + "…");
        requests.execute(() -> {
            String name = "";
            boolean failed = false;
            try {
                name = findProduct(code);
            } catch (Exception error) {
                failed = true;
            }
            String found = name;
            boolean unavailable = failed;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                setBusy(false);
                if (!value(product).isEmpty()) {
                    status.setText("Using the product name you entered.");
                } else if (unavailable) {
                    status.setText("Barcode lookup is unavailable. Enter the name on the pack.");
                } else if (found.isEmpty()) {
                    status.setText("Barcode " + code + " wasn’t found. Enter the name on the pack.");
                } else {
                    product.setText(found);
                    status.setText("Product candidate from Open Food Facts. Confirm the name and variant on the pack.");
                    chooseDirectory();
                }
            });
        });
    }

    private static String findProduct(String code) throws Exception {
        URL url = new URL("https://world.openfoodfacts.org/api/v2/product/" + code
            + ".json?fields=product_name,brands,quantity");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "HalalScanner/0.1 (private Android pilot)");
        try {
            int response = connection.getResponseCode();
            if (response == 404) return "";
            if (response != 200) throw new IOException("Product lookup failed");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 128_000) throw new IOException("Unexpected response size");
                    output.write(buffer, 0, count);
                }
                JSONObject data = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
                JSONObject item = data.optJSONObject("product");
                if (item == null || item.optString("product_name").trim().isEmpty()) return "";
                return (item.optString("brands") + " " + item.optString("product_name")
                    + " " + item.optString("quantity")).trim().replaceAll(" +", " ");
            }
        } finally {
            connection.disconnect();
        }
    }

    private void chooseDirectory() {
        new AlertDialog.Builder(this).setTitle("Check certification")
            .setMessage("Automatic certification lookup is unavailable. Start with MUIS, then check the issuer on the pack.")
            .setPositiveButton("MUIS", (dialog, which) -> openDirectory(0))
            .setNeutralButton("Other issuers", (dialog, which) -> new AlertDialog.Builder(this)
                .setTitle("Foreign certification")
                .setItems(new String[]{"JAKIM · Malaysia", "BPJPH · Indonesia", "MUIS-recognised issuers"},
                    (list, index) -> {
                        if (index == 2) open("https://www.muis.gov.sg/halal/fhcb/");
                        else openDirectory(index + 1);
                    })
                .show())
            .setNegativeButton("Back", null).show();
    }

    private void openDirectory(int index) {
        String name = value(product);
        if (!name.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Product lookup", name));
        }
        status.setText("Opened " + ISSUERS[index] + (name.isEmpty() ? "." : ". Product name copied for search."));
        open(DIRECTORIES[index]);
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            status.setText("A browser is needed to open the official directory.");
        }
    }

    private void recordCertificate() {
        if (value(product).isEmpty()) {
            product.setError("Enter the exact product and variant first.");
            product.requestFocus();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Which issuer did you check?")
            .setItems(ISSUERS, (dialog, index) -> {
                EditText reference = new EditText(this);
                reference.setSingleLine(true);
                reference.setHint("Certificate or official listing reference");
                String instructions = "Confirm that the official record is current and covers this exact pack: "
                    + value(product);
                if (index != 0) instructions += ". Confirm current MUIS recognition for this issuer and product category.";
                AlertDialog confirm = new AlertDialog.Builder(this).setTitle("Confirm official record")
                    .setMessage(instructions).setView(reference).setNegativeButton("Cancel", null)
                    .setPositiveButton("I verified this record", null).create();
                confirm.setOnShowListener(ignored -> confirm.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        String record = value(reference);
                        if (record.isEmpty()) {
                            reference.setError("Enter the official record reference.");
                            return;
                        }
                        issuer = index;
                        certificate = "Manually checked · " + ISSUERS[index] + " · " + record + " · "
                            + DateFormat.getDateInstance().format(new Date());
                        confirm.dismiss();
                        render();
                    }));
                confirm.show();
            }).show();
    }

    private void render() {
        Verdict.Result verdict = Verdict.assess(value(ingredients), complete, certificate);
        title.setText(verdict.title());
        title.setTextColor(Verdict.PROHIBITED.equals(verdict.title()) ? 0xff922b22
            : Verdict.CAUTION.equals(verdict.title()) ? 0xff704405 : INK);
        reason.setText(verdict.reason());
        next.setText(verdict.next());
        source.setVisibility(issuer >= 0 ? View.VISIBLE : View.GONE);
        if (issuer >= 0) source.setText("Open " + ISSUERS[issuer] + " official directory");
    }

    private void discardPhoto() {
        if (photo != null) {
            photo.delete();
            photo = null;
        }
    }

    private void setBusy(boolean running) {
        busy = running;
        barcodeButton.setEnabled(!running);
        labelButton.setEnabled(!running);
        resetButton.setEnabled(!running);
    }

    @Override
    protected void onDestroy() {
        requests.shutdownNow();
        if (isFinishing()) discardPhoto();
        super.onDestroy();
    }

    private EditText field(String label, boolean multiline) {
        TextView caption = text(content, label, 15, INK, true);
        EditText field = new EditText(this);
        field.setId(View.generateViewId());
        field.setSaveEnabled(false);
        caption.setLabelFor(field.getId());
        field.setTextColor(INK);
        field.setTextSize(16);
        field.setInputType(InputType.TYPE_CLASS_TEXT | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multiline) field.setMinLines(3);
        add(content, field, 4);
        return field;
    }

    private Button button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(primary ? Color.WHITE : INK);
        button.setTextSize(16);
        button.setMinHeight(dp(48));
        button.setBackground(background(primary ? INK : 0xffe4ece1));
        button.setOnClickListener(view -> action.run());
        add(parent, button, 8);
        return button;
    }

    private TextView text(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        add(parent, view, 6);
        return view;
    }

    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(top);
        parent.addView(child, layout);
    }

    private GradientDrawable background(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(12));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String value(EditText field) {
        return field.getText().toString().trim();
    }

    private static void watch(EditText field, Runnable changed) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { changed.run(); }
            @Override public void afterTextChanged(Editable text) {}
        });
    }
}
