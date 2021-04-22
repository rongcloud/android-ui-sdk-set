package io.rong.imkit.userinfo.db.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.nio.file.attribute.GroupPrincipal;

@Entity(tableName = "group")
public class Group {
    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "portraitUri")
    public String portraitUrl;

    public Group(String id, String name, String portraitUrl) {
        this.id = id;
        this.name = name;
        this.portraitUrl = portraitUrl;
    }
}
