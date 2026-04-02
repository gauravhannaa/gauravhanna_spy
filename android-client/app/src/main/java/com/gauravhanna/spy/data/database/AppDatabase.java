package com.gauravhanna.spy.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.gauravhanna.spy.data.database.dao.LocationDao;
import com.gauravhanna.spy.data.database.entities.LocationEntity;

@Database(
    entities = {LocationEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
    
    private static volatile AppDatabase instance;
    private static final String DATABASE_NAME = "spy_database";
    
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()  // Optional: handles schema changes
                    .build();
                }
            }
        }
        return instance;
    }
}