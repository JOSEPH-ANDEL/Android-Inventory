package portfolio.androidinventory;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;


public class Inventory extends AppCompatActivity {

    private DatabaseOpenHelper dbHelper;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ListView list = findViewById(R.id.itemList);
        dbHelper = new DatabaseOpenHelper(this);

        dbHelper.syncFirebaseToLocal(new DatabaseOpenHelper.SyncCallback() {
            @Override
            public void onSyncComplete() {
                cursor = dbHelper.getAllItems();

                SimpleCursorAdapter items = new SimpleCursorAdapter(
                        Inventory.this,
                        android.R.layout.simple_list_item_1,
                        cursor,
                        new String[]{"display_text"},
                        new int[]{android.R.id.text1},
                        0
                );

                list.setAdapter(items);

                list.setOnItemClickListener((parent, view, position, id) -> {
                    String description = dbHelper.getDescription(id);
                    Snackbar.make(view, description, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Action", null)
                            .show();
                });

                list.setOnItemLongClickListener((parent, view, position, id) -> {
                    Intent intent = new Intent(Inventory.this, Items.class);
                    intent.putExtra("ITEM_ID", id);
                    startActivity(intent);
                    return true;
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
        }
        dbHelper.close();
    }

    public void addItem(View view)
    {
        Intent intent = new Intent(this, Items.class);
        startActivity(intent);
    }
}

