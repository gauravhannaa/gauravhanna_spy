package com.gauravhanna.spy.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import com.gauravhanna.spy.data.database.entities.LocationEntity;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insert(LocationEntity location);
    
    @Insert
    void insertAll(List<LocationEntity> locations);
    
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    List<LocationEntity> getAllLocations();
    
    @Query("SELECT * FROM locations WHERE id = :id")
    LocationEntity getLocationById(int id);
    
    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    LocationEntity getLatestLocation();
    
    @Query("DELETE FROM locations")
    void deleteAll();
    
    @Delete
    void delete(LocationEntity location);
    
    @Update
    void update(LocationEntity location);
}