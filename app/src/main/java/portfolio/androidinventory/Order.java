package portfolio.androidinventory;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Order extends AppCompatActivity {

    private Spinner productSpinner;
    private TextView qtyText, totText;
    private ListView orderListView;
    private Button addButton, removeButton, finishButton;

    private DatabaseOpenHelper dbHelper;

    private Map<String, Integer> currentOrder = new HashMap<>();
    private double total = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        productSpinner = findViewById(R.id.productSpinner);
        qtyText = findViewById(R.id.quantityText);
        totText = findViewById(R.id.totalText);
        orderListView = findViewById(R.id.orderListView);
        addButton = findViewById(R.id.addButton);
        removeButton = findViewById(R.id.removeButton);
        finishButton = findViewById(R.id.finishButton);

        dbHelper = new DatabaseOpenHelper(this);

        try {
            dbHelper.syncFirebaseToLocal(new DatabaseOpenHelper.SyncCallback() {
                @Override
                public void onSyncComplete() {
                    setupSpinner();
                    setupListeners();
                    updateOrderList();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to sync with Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSpinner() {
        try {
            String[] products = dbHelper.getAllNames();

            if (products != null && products.length > 0) {
                ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, products);
                productAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                productSpinner.setAdapter(productAdapter);
            } else {
                Toast.makeText(this, "No products found in the database", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load products: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupListeners() {
        addButton.setOnClickListener(v -> addItemToOrder());
        removeButton.setOnClickListener(v -> removeItemFromOrder());
        finishButton.setOnClickListener(v -> finishOrder());
    }

    private void addItemToOrder() {
        try {
            String product = productSpinner.getSelectedItem().toString();

            if (qtyText.getText().toString().isEmpty()) {
                Toast.makeText(this, "Enter a quantity first", Toast.LENGTH_SHORT).show();
                return;
            }

            int requestedQty = Integer.parseInt(qtyText.getText().toString());
            int availableQty = dbHelper.getQuantity(product);

            int alreadyOrderedQty = currentOrder.getOrDefault(product, 0);
            int remainingQty = availableQty - alreadyOrderedQty;

            if (requestedQty > remainingQty) {
                Toast.makeText(this, "Only " + remainingQty + " available.", Toast.LENGTH_SHORT).show();
                requestedQty = remainingQty;
            }

            if (requestedQty > 0) {
                currentOrder.put(product, alreadyOrderedQty + requestedQty);
                total += dbHelper.getPrice(product) * requestedQty;
                updateOrderList();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid number for quantity", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeItemFromOrder() {
        String product = productSpinner.getSelectedItem().toString();

        if (currentOrder.containsKey(product)) {
            int quantity = currentOrder.remove(product);
            total -= dbHelper.getPrice(product) * quantity;
            updateOrderList();
        } else {
            Toast.makeText(this, "Item not in order.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrderList() {
        try {
            String[] orderSummary = currentOrder.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue() + " x $" + dbHelper.getPrice(entry.getKey()))
                    .toArray(String[]::new);

            ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, orderSummary);
            orderListView.setAdapter(orderAdapter);
            totText.setText("Total: $" + total);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to update the order list", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishOrder() {
        try {
            for (Map.Entry<String, Integer> entry : currentOrder.entrySet()) {
                String product = entry.getKey();
                int quantity = entry.getValue();

                dbHelper.deductQuantity(product, quantity);
            }

            DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference("items");

            for (String product : currentOrder.keySet()) {
                int updatedQuantity = dbHelper.getQuantity(product);
                firebaseRef.child(product).child("quantity").setValue(updatedQuantity);
            }

            Toast.makeText(this, "Order completed and synced with Firebase.", Toast.LENGTH_SHORT).show();

            currentOrder.clear();
            total = 0;
            updateOrderList();

        } catch (Exception e) {
            Log.e("OrderSync", "Order sync failed", e);
            Toast.makeText(this, "Order completion failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
