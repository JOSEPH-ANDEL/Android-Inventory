package portfolio.androidinventory;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Items extends AppCompatActivity {

    private EditText nameEditText, descEditText, qtyEditText, priceEditText;
    private DatabaseOpenHelper dbHelper;
    private long itemId;
    private DatabaseReference firebaseRef;
    private Button delButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        dbHelper = new DatabaseOpenHelper(this);
        firebaseRef = FirebaseDatabase.getInstance().getReference("items");

        nameEditText = findViewById(R.id.nameEditText);
        descEditText = findViewById(R.id.descriptionEditText);
        qtyEditText = findViewById(R.id.quantityEditText);
        priceEditText = findViewById(R.id.priceEditText);
        Button saveButton = findViewById(R.id.saveButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        delButton = findViewById(R.id.deleteButton);

        itemId = getIntent().getLongExtra("ITEM_ID", -1);

        if (itemId != -1) {
            nameEditText.setEnabled(false);
            new Handler().postDelayed(() -> loadItemData(itemId), 3000);
        }
        else
        {
            nameEditText.setEnabled(true);

        }

        saveButton.setOnClickListener(v -> {
            if (validateInput()) {
                if (itemId == -1) {
                    addNewItem();
                } else {
                    saveItemData();
                }
            }
        });

        cancelButton.setOnClickListener(v -> finish());

        delButton.setOnClickListener(v -> {
            if (itemId != -1) {
                deleteItem();
            }
        });
    }

    @SuppressLint("Range")
    private void loadItemData(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseOpenHelper.TABLE_NAME,
                new String[]{
                        DatabaseOpenHelper.ITEM_NAME,
                        DatabaseOpenHelper.DESCRIPTION,
                        DatabaseOpenHelper.QUANTITY,
                        DatabaseOpenHelper.PRICE
                },
                DatabaseOpenHelper._ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            nameEditText.setText(cursor.getString(cursor.getColumnIndex(DatabaseOpenHelper.ITEM_NAME)));
            descEditText.setText(cursor.getString(cursor.getColumnIndex(DatabaseOpenHelper.DESCRIPTION)));
            qtyEditText.setText(cursor.getString(cursor.getColumnIndex(DatabaseOpenHelper.QUANTITY)));
            priceEditText.setText(cursor.getString(cursor.getColumnIndex(DatabaseOpenHelper.PRICE)));

            cursor.close();

            delButton.setEnabled(true);
        } else {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput() {
        String name = nameEditText.getText().toString().trim();
        String description = descEditText.getText().toString().trim();
        String quantityString = qtyEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter the item name.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (quantityString.isEmpty()) {
            Toast.makeText(this, "Please enter the quantity.", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            Integer.parseInt(quantityString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (priceString.isEmpty()) {
            Toast.makeText(this, "Please enter the price.", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveItemData() {
        try {
            nameEditText.setEnabled(false);
            String name = nameEditText.getText().toString().trim();
            String description = descEditText.getText().toString().trim();
            int quantity = Integer.parseInt(qtyEditText.getText().toString().trim());
            double price = Double.parseDouble(priceEditText.getText().toString().trim());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseOpenHelper.ITEM_NAME, name);
            values.put(DatabaseOpenHelper.DESCRIPTION, description);
            values.put(DatabaseOpenHelper.QUANTITY, quantity);
            values.put(DatabaseOpenHelper.PRICE, price);

            int rowsUpdated = db.update(
                    DatabaseOpenHelper.TABLE_NAME,
                    values,
                    DatabaseOpenHelper._ID + " = ?",
                    new String[]{String.valueOf(itemId)}
            );

            if (rowsUpdated > 0) {
                firebaseRef.child(name).setValue(new Item(itemId, name, description, quantity, price))
                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "Item updated in Firebase."))
                        .addOnFailureListener(e -> Log.e("Firebase", "Update failed in Firebase.", e));

                Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update item in database.", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid quantity and price.", Toast.LENGTH_SHORT).show();
        }
    }


    private void addNewItem() {
        try {
            nameEditText.setEnabled(true);
            String name = nameEditText.getText().toString().trim();
            String description = descEditText.getText().toString().trim();
            int quantity = Integer.parseInt(qtyEditText.getText().toString().trim());
            double price = Double.parseDouble(priceEditText.getText().toString().trim());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseOpenHelper.ITEM_NAME, name);
            values.put(DatabaseOpenHelper.DESCRIPTION, description);
            values.put(DatabaseOpenHelper.QUANTITY, quantity);
            values.put(DatabaseOpenHelper.PRICE, price);

            long newRowId = db.insert(DatabaseOpenHelper.TABLE_NAME, null, values);

            if (newRowId == -1) {
                Toast.makeText(this, "Failed to add new item to database.", Toast.LENGTH_SHORT).show();
            } else {
                firebaseRef.child(name).setValue(new Item(newRowId, name, description, quantity, price))
                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "Item added to Firebase."))
                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to update Firebase.", e));

                Toast.makeText(this, "New item added successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid quantity and price.", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteItem() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String itemName = nameEditText.getText().toString().trim();

                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();

                        int rowsDeleted = db.delete(
                                DatabaseOpenHelper.TABLE_NAME,
                                DatabaseOpenHelper.ITEM_NAME + " = ?",
                                new String[]{itemName}
                        );

                        Log.d("SQLite", "Rows deleted in SQLite: " + rowsDeleted);

                        if (rowsDeleted > 0) {
                            Log.d("SQLite", "Item successfully deleted from local database.");

                            firebaseRef.child(itemName).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase", "Item successfully deleted from Firebase.");
                                        Toast.makeText(this, "Item successfully deleted", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firebase", "Failed to delete item from Firebase.", e);
                                        Toast.makeText(this, "Failed to delete from Firebase", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.e("SQLite", "Item not found in local database.");
                            Toast.makeText(this, "Item not found in the database.", Toast.LENGTH_SHORT).show();
                        }

                        db.close();

                    } catch (Exception e) {
                        Log.e("DeleteItem", "Failed to delete item.", e);
                        Toast.makeText(this, "An error occurred while deleting item.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

}

