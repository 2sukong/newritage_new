package com.newritage.app.ui.measurement;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0014J\b\u0010\u0016\u001a\u00020\u0013H\u0014J\b\u0010\u0017\u001a\u00020\u0013H\u0002J\b\u0010\u0018\u001a\u00020\u0013H\u0002J\b\u0010\u0019\u001a\u00020\u0013H\u0002J\b\u0010\u001a\u001a\u00020\u0013H\u0002J\b\u0010\u001b\u001a\u00020\u0013H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/newritage/app/ui/measurement/MeasurementActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/newritage/app/databinding/ActivityMeasurementBinding;", "chartEntries", "", "Lcom/github/mikephil/charting/data/Entry;", "elapsedSeconds", "", "handler", "Landroid/os/Handler;", "measureLoop", "Ljava/lang/Runnable;", "measuring", "", "pressureReadings", "", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "setupChart", "showGuideDialog", "startMeasurement", "stopMeasurement", "updateChart", "app_debug"})
public final class MeasurementActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.newritage.app.databinding.ActivityMeasurementBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler handler = null;
    private boolean measuring = false;
    private int elapsedSeconds = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.Float> pressureReadings = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.github.mikephil.charting.data.Entry> chartEntries = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Runnable measureLoop = null;
    
    public MeasurementActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void showGuideDialog() {
    }
    
    private final void setupChart() {
    }
    
    private final void startMeasurement() {
    }
    
    private final void updateChart() {
    }
    
    private final void stopMeasurement() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}