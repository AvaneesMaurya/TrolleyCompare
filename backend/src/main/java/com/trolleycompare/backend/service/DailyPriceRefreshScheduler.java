package com.trolleycompare.backend.service;

import com.trolleycompare.backend.model.Product;
import com.trolleycompare.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class DailyPriceRefreshScheduler {

    @Autowired
    private ProductRepository productRepository;

    private static final String[] STORES = {"Tesco", "Dunnes Stores", "SuperValu", "Aldi", "Lidl"};

    private static final List<BaseProductInfo> BASE_ITEMS = List.of(
        new BaseProductInfo("Irish Whole Milk", 1.0, "L", 1.15),
        new BaseProductInfo("Irish Whole Milk", 2.0, "L", 2.25),
        new BaseProductInfo("Bread", 800.0, "g", 1.69),
        new BaseProductInfo("Eggs", 6.0, "pcs", 2.10),
        new BaseProductInfo("Eggs", 12.0, "pcs", 3.75),
        new BaseProductInfo("Apples", 6.0, "pcs", 2.19),
        new BaseProductInfo("Potatoes", 2.5, "kg", 3.19),
        new BaseProductInfo("Chicken Breast", 500.0, "g", 5.29)
    );

    private static final Map<String, StoreFactor> STORE_FACTORS = Map.of(
        "Aldi", new StoreFactor(0.90),
        "Lidl", new StoreFactor(0.91),
        "Tesco", new StoreFactor(1.02),
        "Dunnes Stores", new StoreFactor(0.98),
        "SuperValu", new StoreFactor(1.05)
    );

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println("🚀 [APPLICATION LAUNCH] Running initial database seed...");
        refreshDatabasePrices();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void refreshDatabasePrices() {
        System.out.println("⏰ [CRON JOB STARTED] Wiping database and generating fresh daily prices...");

        productRepository.deleteAllInBatch();

        List<Product> newProducts = new ArrayList<>();
        Random random = new Random();
        LocalDate today = LocalDate.now();

        for (String store : STORES) {
            StoreFactor factor = STORE_FACTORS.get(store);

            for (BaseProductInfo base : BASE_ITEMS) {
                double dailyFluctuation = 0.96 + (0.08 * random.nextDouble());

                // Calculate regular price
                double regPrice = Math.round((base.basePrice * factor.regularFactor * dailyFluctuation) * 100.0) / 100.0;
                
                // Default: Club price equals regular price (no discount)
                double clubPrice = regPrice;

                // For stores with loyalty programs (Tesco, SuperValu), give ~60% of products a club discount
                if (store.equals("Tesco") || store.equals("SuperValu")) {
                    if (random.nextDouble() < 0.6) {
                        // Club price is 8% to 20% cheaper than regular price
                        double discountMultiplier = 0.80 + (0.12 * random.nextDouble());
                        clubPrice = Math.round((regPrice * discountMultiplier) * 100.0) / 100.0;
                    }
                }

                // Safety assurance: Club price can never exceed regular price
                if (clubPrice > regPrice) {
                    clubPrice = regPrice;
                }

                Product p = new Product();
                p.setStoreName(store);
                p.setName(base.name);
                p.setQuantity(base.quantity);
                p.setUnit(base.unit);
                p.setRegularPrice(regPrice);
                p.setClubPrice(clubPrice);
                p.setDate(today);

                newProducts.add(p);
            }
        }

        productRepository.saveAll(newProducts);
        System.out.println("✅ [CRON JOB COMPLETE] Inserted " + newProducts.size() + " fresh price entries into database!");
    }

    private record BaseProductInfo(String name, double quantity, String unit, double basePrice) {}
    private record StoreFactor(double regularFactor) {}
}