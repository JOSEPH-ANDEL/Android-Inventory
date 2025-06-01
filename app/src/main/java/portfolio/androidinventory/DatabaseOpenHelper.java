package portfolio.androidinventory;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;


public class DatabaseOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "inventory";
    public static final String _ID = "_id";
    public static final String NAME = "inventory_db";
    public static final int VERSION = 2;
    public static final String ITEM_NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String QUANTITY = "quantity";
    public static final String PRICE = "price";

    public DatabaseOpenHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CMD = "CREATE TABLE " + TABLE_NAME + " (" + _ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ITEM_NAME + " TEXT, " +
                DESCRIPTION + " TEXT, " +
                QUANTITY + " INTEGER, " +
                PRICE + " REAL) ";

        db.execSQL(CREATE_CMD);
    }

    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT _id, " +
                "name || ' | Qty: ' || quantity || ' | Price: $' || price AS display_text " +
                "FROM " + TABLE_NAME;

        return db.rawQuery(query, null);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String getDescription(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] column = {DESCRIPTION};

        Cursor cursor = db.query(
                TABLE_NAME,
                column,
                _ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
            cursor.close();
            return description;
        } else {
            return null;
        }
    }

    @SuppressLint("Range")
    public String[] getAllNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{ITEM_NAME}, null, null, null, null, null);

        String[] products = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            products[i++] = cursor.getString(cursor.getColumnIndex(ITEM_NAME));
        }
        cursor.close();
        return products;
    }

    @SuppressLint("Range")
    public int getQuantity(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{QUANTITY}, ITEM_NAME + " = ?", new String[]{name}, null, null, null);

        int quantity = 0;
        if (cursor.moveToFirst()) {
            quantity = cursor.getInt(cursor.getColumnIndex(QUANTITY));
        }
        cursor.close();
        return quantity;
    }

    @SuppressLint("Range")
    public double getPrice(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{PRICE}, ITEM_NAME + " = ?", new String[]{name}, null, null, null);

        double price = 0.0;
        if (cursor.moveToFirst()) {
            price = cursor.getDouble(cursor.getColumnIndex(PRICE));
        }
        cursor.close();
        return price;
    }

    public void deductQuantity(String name, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + QUANTITY + " = " + QUANTITY + " - " + quantity + " WHERE " + ITEM_NAME + " = ?", new String[]{name});
    }

    public interface SyncCallback {
        void onSyncComplete();
    }

    public void syncFirebaseToLocal(SyncCallback callback) {
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference("items");
        SQLiteDatabase db = getWritableDatabase();

        db.delete(TABLE_NAME, null, null);

        firebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);

                    if (item != null) {
                        ContentValues values = new ContentValues();
                        values.put(DatabaseOpenHelper._ID, item.getId());
                        values.put(DatabaseOpenHelper.ITEM_NAME, item.getName());
                        values.put(DatabaseOpenHelper.DESCRIPTION, item.getDescription());
                        values.put(DatabaseOpenHelper.QUANTITY, item.getQuantity());
                        values.put(DatabaseOpenHelper.PRICE, item.getPrice());

                        db.insert(DatabaseOpenHelper.TABLE_NAME, null, values);
                    }
                }

                if (callback != null) {
                    callback.onSyncComplete();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SyncFirebase", "Sync failed", error.toException());
                if (callback != null) {
                    callback.onSyncComplete();
                }
            }
        });
    }







}


