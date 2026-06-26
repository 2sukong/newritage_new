package com.newritage.app.ui.measurement;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001:\u0001\u001fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0011\u001a\u00020\n2\u0006\u0010\u0012\u001a\u00020\nH\u0002J\u0010\u0010\u0013\u001a\u00020\n2\u0006\u0010\u0003\u001a\u00020\u0004H\u0002J\u0012\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0014J\b\u0010\u0018\u001a\u00020\u0015H\u0002J\b\u0010\u0019\u001a\u00020\u0015H\u0002J\b\u0010\u001a\u001a\u00020\u0015H\u0002J\u0010\u0010\u001b\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u0015H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/newritage/app/ui/measurement/SessionCompleteActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "avgPressure", "", "binding", "Lcom/newritage/app/databinding/ActivitySessionCompleteBinding;", "durationSeconds", "", "generatedColor", "", "maxPressure", "minPressure", "prefs", "Lcom/newritage/app/data/UserPreferences;", "savedSessionId", "", "colorName", "hex", "generateThreadColor", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "populateRecord", "saveSession", "setupButtons", "showScreen", "screen", "Lcom/newritage/app/ui/measurement/SessionCompleteActivity$Screen;", "showThreadProvide", "Screen", "app_debug"})
public final class SessionCompleteActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.newritage.app.databinding.ActivitySessionCompleteBinding binding;
    private com.newritage.app.data.UserPreferences prefs;
    private long savedSessionId = -1L;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String generatedColor = "#8B9E7B";
    private int durationSeconds = 0;
    private float avgPressure = 0.0F;
    private float maxPressure = 0.0F;
    private float minPressure = 0.0F;
    
    public SessionCompleteActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupButtons() {
    }
    
    private final void populateRecord() {
    }
    
    private final void saveSession() {
    }
    
    private final void showThreadProvide() {
    }
    
    /**
     * 평균 압력 기반 실 색상 생성 (플레이스홀더 로직)
     */
    private final java.lang.String generateThreadColor(float avgPressure) {
        return null;
    }
    
    private final java.lang.String colorName(java.lang.String hex) {
        return null;
    }
    
    private final void showScreen(com.newritage.app.ui.measurement.SessionCompleteActivity.Screen screen) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/newritage/app/ui/measurement/SessionCompleteActivity$Screen;", "", "(Ljava/lang/String;I)V", "COMPLETE", "RECORD", "THREAD", "app_debug"})
    public static enum Screen {
        /*public static final*/ COMPLETE /* = new COMPLETE() */,
        /*public static final*/ RECORD /* = new RECORD() */,
        /*public static final*/ THREAD /* = new THREAD() */;
        
        Screen() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.newritage.app.ui.measurement.SessionCompleteActivity.Screen> getEntries() {
            return null;
        }
    }
}