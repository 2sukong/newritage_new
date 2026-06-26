package com.newritage.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Session> __insertionAdapterOfSession;

  private final EntityDeletionOrUpdateAdapter<Session> __deletionAdapterOfSession;

  private final EntityDeletionOrUpdateAdapter<Session> __updateAdapterOfSession;

  private final SharedSQLiteStatement __preparedStmtOfUpdateEmotion;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSession = new EntityInsertionAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sessions` (`id`,`date`,`durationSeconds`,`avgPressure`,`maxPressure`,`minPressure`,`emotion`,`threadColor`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getDurationSeconds());
        statement.bindDouble(4, entity.getAvgPressure());
        statement.bindDouble(5, entity.getMaxPressure());
        statement.bindDouble(6, entity.getMinPressure());
        if (entity.getEmotion() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEmotion());
        }
        if (entity.getThreadColor() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getThreadColor());
        }
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSession = new EntityDeletionOrUpdateAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sessions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSession = new EntityDeletionOrUpdateAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sessions` SET `id` = ?,`date` = ?,`durationSeconds` = ?,`avgPressure` = ?,`maxPressure` = ?,`minPressure` = ?,`emotion` = ?,`threadColor` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getDurationSeconds());
        statement.bindDouble(4, entity.getAvgPressure());
        statement.bindDouble(5, entity.getMaxPressure());
        statement.bindDouble(6, entity.getMinPressure());
        if (entity.getEmotion() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEmotion());
        }
        if (entity.getThreadColor() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getThreadColor());
        }
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateEmotion = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sessions SET emotion = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Session session, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSession.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Session session, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Session session, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEmotion(final long id, final String emotion,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateEmotion.acquire();
        int _argIndex = 1;
        if (emotion == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, emotion);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateEmotion.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getSessionsByDate(final String date,
      final Continuation<? super List<Session>> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE date = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestSessionByDate(final String date,
      final Continuation<? super Session> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE date = ? ORDER BY createdAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Session>() {
      @Override
      @Nullable
      public Session call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Session _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSessionsInRange(final String startDate, final String endDate,
      final Continuation<? super List<Session>> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE date BETWEEN ? AND ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, startDate);
    }
    _argIndex = 2;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, endDate);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSessionsByMonth(final String yearMonth,
      final Continuation<? super List<Session>> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE date LIKE ? || '%' ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (yearMonth == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, yearMonth);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Session>> getAllSessionsFlow() {
    final String _sql = "SELECT * FROM sessions ORDER BY date DESC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super Session> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Session>() {
      @Override
      @Nullable
      public Session call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfAvgPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPressure");
          final int _cursorIndexOfMaxPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "maxPressure");
          final int _cursorIndexOfMinPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "minPressure");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfThreadColor = CursorUtil.getColumnIndexOrThrow(_cursor, "threadColor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Session _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final float _tmpAvgPressure;
            _tmpAvgPressure = _cursor.getFloat(_cursorIndexOfAvgPressure);
            final float _tmpMaxPressure;
            _tmpMaxPressure = _cursor.getFloat(_cursorIndexOfMaxPressure);
            final float _tmpMinPressure;
            _tmpMinPressure = _cursor.getFloat(_cursorIndexOfMinPressure);
            final String _tmpEmotion;
            if (_cursor.isNull(_cursorIndexOfEmotion)) {
              _tmpEmotion = null;
            } else {
              _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            }
            final String _tmpThreadColor;
            if (_cursor.isNull(_cursorIndexOfThreadColor)) {
              _tmpThreadColor = null;
            } else {
              _tmpThreadColor = _cursor.getString(_cursorIndexOfThreadColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Session(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpAvgPressure,_tmpMaxPressure,_tmpMinPressure,_tmpEmotion,_tmpThreadColor,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
