package io.rong.imkit.userinfo.db.model;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import io.rong.imlib.model.UserInfo;

@Entity(tableName = "user")
public class User {
    @ColumnInfo(name = "id")
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "alias")
    public String alias;

    @ColumnInfo(name = "portraitUri")
    public String portraitUrl;

    @ColumnInfo(name = "extra")
    public String extra;

    public User() {
        // default implementation ignored
    }

    public User(String id, String name, Uri portraitUrl) {
        this.id = id;
        this.name = name;
        if (portraitUrl != null) {
            this.portraitUrl = portraitUrl.toString();
        }
    }

    public User(UserInfo info) {
        this.id = info.getUserId();
        this.name = info.getName();
        this.alias = info.getAlias();
        if (info.getPortraitUri() != null) {
            this.portraitUrl = info.getPortraitUri().toString();
        }
        this.extra = info.getExtra();
    }
}
