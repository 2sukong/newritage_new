package com.newritage.app.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000e\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\bH\'J\u0018\u0010\n\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u000b\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0018\u0010\u000e\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u000f\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001c\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u000f\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001c\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u0014\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J$\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u0016\u001a\u00020\u00102\u0006\u0010\u0017\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u0016\u0010\u0019\u001a\u00020\f2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u001c\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u001d\u00a8\u0006\u001e"}, d2 = {"Lcom/newritage/app/data/SessionDao;", "", "delete", "", "session", "Lcom/newritage/app/data/Session;", "(Lcom/newritage/app/data/Session;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllSessionsFlow", "Lkotlinx/coroutines/flow/Flow;", "", "getById", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLatestSessionByDate", "date", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSessionsByDate", "getSessionsByMonth", "yearMonth", "getSessionsInRange", "startDate", "endDate", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "update", "updateEmotion", "emotion", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface SessionDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.newritage.app.data.Session session, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.newritage.app.data.Session session, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.newritage.app.data.Session session, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 특정 날짜의 세션 목록
     */
    @androidx.room.Query(value = "SELECT * FROM sessions WHERE date = :date ORDER BY createdAt DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSessionsByDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.newritage.app.data.Session>> $completion);
    
    /**
     * 특정 날짜의 가장 최근 세션 1개
     */
    @androidx.room.Query(value = "SELECT * FROM sessions WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatestSessionByDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.newritage.app.data.Session> $completion);
    
    /**
     * 특정 기간 세션 목록
     */
    @androidx.room.Query(value = "SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSessionsInRange(@org.jetbrains.annotations.NotNull()
    java.lang.String startDate, @org.jetbrains.annotations.NotNull()
    java.lang.String endDate, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.newritage.app.data.Session>> $completion);
    
    /**
     * 특정 연월의 세션 목록 (yyyy-MM)
     */
    @androidx.room.Query(value = "SELECT * FROM sessions WHERE date LIKE :yearMonth || \'%\' ORDER BY date ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSessionsByMonth(@org.jetbrains.annotations.NotNull()
    java.lang.String yearMonth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.newritage.app.data.Session>> $completion);
    
    /**
     * 전체 세션 (Flow)
     */
    @androidx.room.Query(value = "SELECT * FROM sessions ORDER BY date DESC, createdAt DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.newritage.app.data.Session>> getAllSessionsFlow();
    
    /**
     * ID로 조회
     */
    @androidx.room.Query(value = "SELECT * FROM sessions WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getById(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.newritage.app.data.Session> $completion);
    
    /**
     * 감정 업데이트
     */
    @androidx.room.Query(value = "UPDATE sessions SET emotion = :emotion WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateEmotion(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String emotion, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}