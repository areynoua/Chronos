package com.reynouard.alexis.chronos.model;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(
        entities = {Task.class, Work.class},
        version = 1
)
public abstract class ChronosRoom extends RoomDatabase {

    private static volatile ChronosRoom INSTANCE;

    public static ChronosRoom getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChronosRoom.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ChronosRoom.class, "chronos_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ChronosDao getChronosDao();
}
