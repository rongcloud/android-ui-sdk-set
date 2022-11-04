package io.rong.imkit.userinfo.db.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

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

    @ColumnInfo(name = "extra")
    public String extra;

    @Ignore
    public Group(String id, String name, String portraitUrl) {
        this(id, name, portraitUrl, "");
    }

    public Group(String id, String name, String portraitUrl, String extra) {
        this.id = id;
        this.name = name;
        this.portraitUrl = portraitUrl;
        this.extra = extra;
    }
}
