package com.newritage.app.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u001e\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001BU\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\t\u0012\u0006\u0010\u000b\u001a\u00020\t\u0012\b\b\u0002\u0010\f\u001a\u00020\u0005\u0012\b\b\u0002\u0010\r\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u000fJ\t\u0010\u001d\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\u0007H\u00c6\u0003J\t\u0010 \u001a\u00020\tH\u00c6\u0003J\t\u0010!\u001a\u00020\tH\u00c6\u0003J\t\u0010\"\u001a\u00020\tH\u00c6\u0003J\t\u0010#\u001a\u00020\u0005H\u00c6\u0003J\t\u0010$\u001a\u00020\u0005H\u00c6\u0003J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003Jc\u0010&\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\t2\b\b\u0002\u0010\u000b\u001a\u00020\t2\b\b\u0002\u0010\f\u001a\u00020\u00052\b\b\u0002\u0010\r\u001a\u00020\u00052\b\b\u0002\u0010\u000e\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\'\u001a\u00020(2\b\u0010)\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010*\u001a\u00020\u0007H\u00d6\u0001J\t\u0010+\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\f\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0015R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0013R\u0011\u0010\n\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0011R\u0011\u0010\u000b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0011R\u0011\u0010\r\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0015\u00a8\u0006,"}, d2 = {"Lcom/newritage/app/data/Session;", "", "id", "", "date", "", "durationSeconds", "", "avgPressure", "", "maxPressure", "minPressure", "emotion", "threadColor", "createdAt", "(JLjava/lang/String;IFFFLjava/lang/String;Ljava/lang/String;J)V", "getAvgPressure", "()F", "getCreatedAt", "()J", "getDate", "()Ljava/lang/String;", "getDurationSeconds", "()I", "getEmotion", "getId", "getMaxPressure", "getMinPressure", "getThreadColor", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
@androidx.room.Entity(tableName = "sessions")
public final class Session {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    
    /**
     * yyyy-MM-dd 형식 날짜
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String date = null;
    
    /**
     * 명상 시간 (초)
     */
    private final int durationSeconds = 0;
    
    /**
     * 평균 압력 (kPa)
     */
    private final float avgPressure = 0.0F;
    
    /**
     * 최고 압력 (kPa)
     */
    private final float maxPressure = 0.0F;
    
    /**
     * 최저 압력 (kPa)
     */
    private final float minPressure = 0.0F;
    
    /**
     * 오늘의 감정 메모
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String emotion = null;
    
    /**
     * 실 색상 (hex, 예: #FF5733) — 하드웨어 미연동 시 랜덤 할당
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String threadColor = null;
    
    /**
     * 생성 시각 (ms)
     */
    private final long createdAt = 0L;
    
    public Session(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String date, int durationSeconds, float avgPressure, float maxPressure, float minPressure, @org.jetbrains.annotations.NotNull()
    java.lang.String emotion, @org.jetbrains.annotations.NotNull()
    java.lang.String threadColor, long createdAt) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    /**
     * yyyy-MM-dd 형식 날짜
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDate() {
        return null;
    }
    
    /**
     * 명상 시간 (초)
     */
    public final int getDurationSeconds() {
        return 0;
    }
    
    /**
     * 평균 압력 (kPa)
     */
    public final float getAvgPressure() {
        return 0.0F;
    }
    
    /**
     * 최고 압력 (kPa)
     */
    public final float getMaxPressure() {
        return 0.0F;
    }
    
    /**
     * 최저 압력 (kPa)
     */
    public final float getMinPressure() {
        return 0.0F;
    }
    
    /**
     * 오늘의 감정 메모
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getEmotion() {
        return null;
    }
    
    /**
     * 실 색상 (hex, 예: #FF5733) — 하드웨어 미연동 시 랜덤 할당
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getThreadColor() {
        return null;
    }
    
    /**
     * 생성 시각 (ms)
     */
    public final long getCreatedAt() {
        return 0L;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final int component3() {
        return 0;
    }
    
    public final float component4() {
        return 0.0F;
    }
    
    public final float component5() {
        return 0.0F;
    }
    
    public final float component6() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component8() {
        return null;
    }
    
    public final long component9() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.newritage.app.data.Session copy(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String date, int durationSeconds, float avgPressure, float maxPressure, float minPressure, @org.jetbrains.annotations.NotNull()
    java.lang.String emotion, @org.jetbrains.annotations.NotNull()
    java.lang.String threadColor, long createdAt) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}