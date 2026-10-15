package com.reclaimx.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reclaimx.config.Database;
import com.reclaimx.dao.ClaimDao;
import com.reclaimx.dao.ItemDao;
import com.reclaimx.dao.UserDao;
import java.sql.*;
import java.util.*;

public class StatisticsService {
    private final UserDao userDao = new UserDao();
    private final ItemDao itemDao = new ItemDao();
    private final ClaimDao claimDao = new ClaimDao();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getGeneralStatistics() {
        int totalUsers = userDao.countTotalUsers();
        int totalLost = itemDao.countByType("lost");
        int totalFound = itemDao.countByType("found");
        int pendingClaims = claimDao.countByStatus("Pending");
        int returnedItems = itemDao.countByStatus("Returned");
        int totalItems = itemDao.countTotalItems();
        double recoveryRate = 0.0;
        if (totalItems > 0) {
            recoveryRate = Math.round((double) returnedItems / totalItems * 1000.0) / 10.0;
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_users", totalUsers);
        stats.put("total_lost", totalLost);
        stats.put("total_found", totalFound);
        stats.put("total_matches", 0);
        stats.put("pending_claims", pendingClaims);
        stats.put("returned_items", returnedItems);
        stats.put("recovery_rate", recoveryRate);
        return stats;
    }

    public Map<String, Object> getDetailedReportData() {
        Map<String, Object> report = new HashMap<>();

        // 1. Granular Item Counts by Type & Status
        int lostOpen = countItems("lost", "Lost");
        int lostClaimed = countItems("lost", "Claimed");
        int lostReturned = countItems("lost", "Returned");
        int lostClosed = countItems("lost", "Closed") + countItems("lost", "Rejected");
        int totalLost = lostOpen + lostClaimed + lostReturned + lostClosed;

        int foundOpen = countItems("found", "Found");
        int foundClaimed = countItems("found", "Claimed");
        int foundVerified = countItems("found", "Verified");
        int foundReturned = countItems("found", "Returned");
        int foundClosed = countItems("found", "Closed") + countItems("found", "Rejected");
        int totalFound = foundOpen + foundClaimed + foundVerified + foundReturned + foundClosed;

        int totalItems = totalLost + totalFound;
        int totalReturned = lostReturned + foundReturned;
        int totalClaimed = lostClaimed + foundClaimed + foundVerified;
        int totalActive = lostOpen + foundOpen;
        int totalClosed = lostClosed + foundClosed;

        double recoveryRate = calcPct(totalReturned, totalItems);
        double lostRecoveryRate = calcPct(lostReturned, totalLost);
        double foundRecoveryRate = calcPct(foundReturned, totalFound);

        // 2. Claims Breakdown & Metrics
        int claimsPending = claimDao.countByStatus("Pending");
        int claimsApproved = claimDao.countByStatus("Approved");
        int claimsRejected = claimDao.countByStatus("Rejected");
        int totalClaims = claimsPending + claimsApproved + claimsRejected;

        double claimApprovalRate = calcPct(claimsApproved, totalClaims);
        double claimRejectionRate = calcPct(claimsRejected, totalClaims);
        double claimPendingRate = calcPct(claimsPending, totalClaims);

        // Store summary numeric values
        report.put("total_items", totalItems);
        report.put("total_lost", totalLost);
        report.put("total_found", totalFound);
        report.put("total_returned", totalReturned);
        report.put("total_claimed", totalClaimed);
        report.put("total_active", totalActive);
        report.put("total_closed", totalClosed);
        report.put("recovery_rate", recoveryRate);
        report.put("lost_recovery_rate", lostRecoveryRate);
        report.put("found_recovery_rate", foundRecoveryRate);

        // Lost items specific numbers
        report.put("lost_open_count", lostOpen);
        report.put("lost_claimed_count", lostClaimed);
        report.put("lost_returned_count", lostReturned);
        report.put("lost_closed_count", lostClosed);
        report.put("lost_open_pct", calcPct(lostOpen, totalLost));
        report.put("lost_claimed_pct", calcPct(lostClaimed, totalLost));
        report.put("lost_returned_pct", calcPct(lostReturned, totalLost));
        report.put("lost_closed_pct", calcPct(lostClosed, totalLost));

        // Found items specific numbers
        report.put("found_open_count", foundOpen);
        report.put("found_claimed_count", foundClaimed);
        report.put("found_verified_count", foundVerified);
        report.put("found_returned_count", foundReturned);
        report.put("found_closed_count", foundClosed);
        report.put("found_open_pct", calcPct(foundOpen, totalFound));
        report.put("found_claimed_pct", calcPct(foundClaimed + foundVerified, totalFound));
        report.put("found_returned_pct", calcPct(foundReturned, totalFound));
        report.put("found_closed_pct", calcPct(foundClosed, totalFound));

        // Claims numbers
        report.put("total_claims", totalClaims);
        report.put("claims_pending", claimsPending);
        report.put("claims_approved", claimsApproved);
        report.put("claims_rejected", claimsRejected);
        report.put("claim_approval_rate", claimApprovalRate);
        report.put("claim_rejection_rate", claimRejectionRate);
        report.put("claim_pending_rate", claimPendingRate);

        // 3. Item Status Matrix Table
        List<Map<String, Object>> statusMatrix = new ArrayList<>();
        statusMatrix.add(createMatrixRow(
            "Open / Active",
            "Lost item missing or Found item waiting for claimant",
            "badge-status-open",
            lostOpen, calcPct(lostOpen, totalLost),
            foundOpen, calcPct(foundOpen, totalFound),
            openTotal(lostOpen, foundOpen), calcPct(openTotal(lostOpen, foundOpen), totalItems)
        ));
        statusMatrix.add(createMatrixRow(
            "Claimed / In Verification",
            "Claim submitted by student or verified awaiting handover",
            "badge-status-claimed",
            lostClaimed, calcPct(lostClaimed, totalLost),
            foundClaimed + foundVerified, calcPct(foundClaimed + foundVerified, totalFound),
            lostClaimed + foundClaimed + foundVerified, calcPct(lostClaimed + foundClaimed + foundVerified, totalItems)
        ));
        statusMatrix.add(createMatrixRow(
            "Returned to Owner",
            "Item successfully handed back to legitimate verified owner",
            "badge-status-returned",
            lostReturned, calcPct(lostReturned, totalLost),
            foundReturned, calcPct(foundReturned, totalFound),
            totalReturned, calcPct(totalReturned, totalItems)
        ));
        statusMatrix.add(createMatrixRow(
            "Closed / Unrecovered",
            "Case closed, expired, or archived by administration",
            "badge-status-closed",
            lostClosed, calcPct(lostClosed, totalLost),
            foundClosed, calcPct(foundClosed, totalFound),
            totalClosed, calcPct(totalClosed, totalItems)
        ));
        report.put("status_matrix", statusMatrix);

        // 4. Claims Status Breakdown Table
        List<Map<String, Object>> claimsBreakdown = new ArrayList<>();
        claimsBreakdown.add(createClaimRow(
            "Pending Review", claimsPending, claimPendingRate,
            "badge-status-pending",
            "Proof submitted by student; awaiting administrative verification"
        ));
        claimsBreakdown.add(createClaimRow(
            "Approved", claimsApproved, claimApprovalRate,
            "badge-status-approved",
            "Ownership verified; item ready for pickup / marked for collection"
        ));
        claimsBreakdown.add(createClaimRow(
            "Rejected", claimsRejected, claimRejectionRate,
            "badge-status-rejected",
            "Claim rejected due to mismatch, invalid details, or duplicate claim"
        ));
        report.put("claims_breakdown", claimsBreakdown);

        // 5. Category Breakdown Query
        List<Map<String, Object>> categoryStats = new ArrayList<>();
        String catSql = """
            SELECT category,
                   SUM(CASE WHEN type = 'lost' THEN 1 ELSE 0 END) as lost_cnt,
                   SUM(CASE WHEN type = 'found' THEN 1 ELSE 0 END) as found_cnt,
                   SUM(CASE WHEN status = 'Returned' THEN 1 ELSE 0 END) as ret_cnt,
                   COUNT(*) as tot_cnt
            FROM items
            WHERE category IS NOT NULL AND category != ''
            GROUP BY category
            ORDER BY tot_cnt DESC
        """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(catSql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                String cat = rs.getString("category");
                int lCnt = rs.getInt("lost_cnt");
                int fCnt = rs.getInt("found_cnt");
                int rCnt = rs.getInt("ret_cnt");
                int tCnt = rs.getInt("tot_cnt");
                row.put("category", cat);
                row.put("lost_count", lCnt);
                row.put("found_count", fCnt);
                row.put("returned_count", rCnt);
                row.put("total_count", tCnt);
                row.put("recovery_rate", calcPct(rCnt, tCnt));
                categoryStats.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        report.put("category_stats", categoryStats);

        // 6. Location Breakdown Query
        List<Map<String, Object>> locationStats = new ArrayList<>();
        String locSql = """
            SELECT location,
                   SUM(CASE WHEN type = 'lost' THEN 1 ELSE 0 END) as lost_cnt,
                   SUM(CASE WHEN type = 'found' THEN 1 ELSE 0 END) as found_cnt,
                   SUM(CASE WHEN status = 'Returned' THEN 1 ELSE 0 END) as ret_cnt,
                   COUNT(*) as tot_cnt
            FROM items
            WHERE location IS NOT NULL AND location != ''
            GROUP BY location
            ORDER BY tot_cnt DESC
            LIMIT 10
        """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(locSql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("location", rs.getString("location"));
                row.put("lost_count", rs.getInt("lost_cnt"));
                row.put("found_count", rs.getInt("found_cnt"));
                row.put("returned_count", rs.getInt("ret_cnt"));
                row.put("count", rs.getInt("tot_cnt"));
                locationStats.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        report.put("location_stats", locationStats);

        // 7. Monthly Trend Query
        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        String monthSql = """
            SELECT strftime('%Y-%m', created_at) as month,
                   SUM(CASE WHEN type = 'lost' THEN 1 ELSE 0 END) as lost_cnt,
                   SUM(CASE WHEN type = 'found' THEN 1 ELSE 0 END) as found_cnt,
                   SUM(CASE WHEN status = 'Returned' THEN 1 ELSE 0 END) as ret_cnt,
                   COUNT(*) as tot_cnt
            FROM items
            WHERE created_at IS NOT NULL
            GROUP BY month
            ORDER BY month ASC
        """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(monthSql)) {
            while (rs.next()) {
                String m = rs.getString("month");
                if (m != null) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("month", m);
                    row.put("lost_count", rs.getInt("lost_cnt"));
                    row.put("found_count", rs.getInt("found_cnt"));
                    row.put("returned_count", rs.getInt("ret_cnt"));
                    row.put("count", rs.getInt("tot_cnt"));
                    monthlyStats.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        report.put("monthly_stats", monthlyStats);

        // Top Category and Top Location highlights
        String topCategory = categoryStats.isEmpty() ? "None" : (String) categoryStats.get(0).get("category");
        String topLocation = locationStats.isEmpty() ? "None" : (String) locationStats.get(0).get("location");
        report.put("top_category", topCategory);
        report.put("top_location", topLocation);

        // 8. Build Chart.js Data Structure & JSON Payload
        Map<String, Object> chartData = new LinkedHashMap<>();

        // Chart 1: Status Comparison (Grouped Bar Chart)
        Map<String, Object> statusComp = new LinkedHashMap<>();
        statusComp.put("labels", Arrays.asList("Active / Open", "Claimed / In-Review", "Returned", "Closed"));
        statusComp.put("lost", Arrays.asList(lostOpen, lostClaimed, lostReturned, lostClosed));
        statusComp.put("found", Arrays.asList(foundOpen, foundClaimed + foundVerified, foundReturned, foundClosed));
        chartData.put("statusComparison", statusComp);

        // Chart 2: Item Type Ratio (Doughnut)
        Map<String, Object> typeDist = new LinkedHashMap<>();
        typeDist.put("labels", Arrays.asList("Lost Items", "Found Items"));
        typeDist.put("data", Arrays.asList(totalLost, totalFound));
        typeDist.put("colors", Arrays.asList("#ff4d6d", "#00c6ff"));
        chartData.put("typeDistribution", typeDist);

        // Chart 3: Item Status Overall (Doughnut)
        Map<String, Object> overallStatus = new LinkedHashMap<>();
        overallStatus.put("labels", Arrays.asList("Returned to Owner", "Claimed / Verification", "Active Lost", "Active Found", "Closed / Rejected"));
        overallStatus.put("data", Arrays.asList(totalReturned, totalClaimed, lostOpen, foundOpen, totalClosed));
        overallStatus.put("colors", Arrays.asList("#00e5a0", "#ffc947", "#ff4d6d", "#6c63ff", "#8d99ae"));
        chartData.put("overallStatus", overallStatus);

        // Chart 4: Claims Outcome Breakdown (Doughnut)
        Map<String, Object> claimsOutcome = new LinkedHashMap<>();
        claimsOutcome.put("labels", Arrays.asList("Approved", "Pending Review", "Rejected"));
        claimsOutcome.put("data", Arrays.asList(claimsApproved, claimsPending, claimsRejected));
        claimsOutcome.put("colors", Arrays.asList("#00e5a0", "#ffc947", "#ff4d6d"));
        chartData.put("claimsOutcome", claimsOutcome);

        // Chart 5: Category Breakdown Bar Chart
        List<String> catLabels = new ArrayList<>();
        List<Integer> catLost = new ArrayList<>();
        List<Integer> catFound = new ArrayList<>();
        for (Map<String, Object> c : categoryStats) {
            catLabels.add((String) c.get("category"));
            catLost.add((Integer) c.get("lost_count"));
            catFound.add((Integer) c.get("found_count"));
        }
        Map<String, Object> catChart = new LinkedHashMap<>();
        catChart.put("labels", catLabels);
        catChart.put("lost", catLost);
        catChart.put("found", catFound);
        chartData.put("categoryChart", catChart);

        // Chart 6: Monthly Trend Line Chart
        List<String> mLabels = new ArrayList<>();
        List<Integer> mLost = new ArrayList<>();
        List<Integer> mFound = new ArrayList<>();
        List<Integer> mRet = new ArrayList<>();
        for (Map<String, Object> m : monthlyStats) {
            mLabels.add((String) m.get("month"));
            mLost.add((Integer) m.get("lost_count"));
            mFound.add((Integer) m.get("found_count"));
            mRet.add((Integer) m.get("returned_count"));
        }
        Map<String, Object> mTrend = new LinkedHashMap<>();
        mTrend.put("labels", mLabels);
        mTrend.put("lost", mLost);
        mTrend.put("found", mFound);
        mTrend.put("returned", mRet);
        chartData.put("monthlyTrend", mTrend);

        String chartDataJson = "{}";
        try {
            chartDataJson = objectMapper.writeValueAsString(chartData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        report.put("chart_data_json", chartDataJson);

        return report;
    }

    private int openTotal(int a, int b) {
        return a + b;
    }

    private double calcPct(int count, int total) {
        if (total <= 0) return 0.0;
        return Math.round(((double) count / total) * 1000.0) / 10.0;
    }

    private Map<String, Object> createMatrixRow(String statusLabel, String desc, String badgeClass,
                                               int lostCount, double lostPct,
                                               int foundCount, double foundPct,
                                               int totalCount, double totalPct) {
        Map<String, Object> row = new HashMap<>();
        row.put("status_label", statusLabel);
        row.put("description", desc);
        row.put("badge_class", badgeClass);
        row.put("lost_count", lostCount);
        row.put("lost_pct", lostPct);
        row.put("found_count", foundCount);
        row.put("found_pct", foundPct);
        row.put("total_count", totalCount);
        row.put("total_pct", totalPct);
        return row;
    }

    private Map<String, Object> createClaimRow(String status, int count, double pct, String badgeClass, String desc) {
        Map<String, Object> row = new HashMap<>();
        row.put("status", status);
        row.put("count", count);
        row.put("pct", pct);
        row.put("badge_class", badgeClass);
        row.put("description", desc);
        return row;
    }

    private int countItems(String type, String status) {
        String sql = "SELECT COUNT(*) FROM items WHERE type = ? AND status = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Seeds comprehensive sample demonstration records across various statuses
     * (Lost, Found, Claimed, Verified, Returned, Closed, Pending, Approved, Rejected)
     * if the database has few items, ensuring visual presentation reports look rich.
     */
    public boolean seedDemoDataIfRequested() {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // Fetch a valid user ID to associate items with
            int studentUserId = 2; // fallback to Ayush Sharma STU101
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id FROM users WHERE role = 'student' LIMIT 1")) {
                if (rs.next()) {
                    studentUserId = rs.getInt(1);
                }
            }

            int claimantUserId = 3; // fallback to Ayush Dobhal STU
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id FROM users WHERE role = 'student' ORDER BY id DESC LIMIT 1")) {
                if (rs.next()) {
                    claimantUserId = rs.getInt(1);
                }
            }

            String insertItemSql = """
                INSERT INTO items (user_id, title, description, category, color, brand, location, date_lost_found, type, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String checkItemSql = "SELECT id FROM items WHERE title = ?";

            // Sample items across statuses
            Object[][] demoItems = new Object[][] {
                { studentUserId, "Apple MacBook Air M2", "Space Grey 13-inch laptop with stickers on back", "Laptop", "Grey", "Apple", "Central Library", "2026-09-10", "lost", "Returned", "2026-09-10 10:15:00", "2026-09-12 14:00:00" },
                { studentUserId, "Titan Neo Quartz Watch", "Stainless steel analog dial wrist watch with brown leather strap", "Watch", "Silver", "Titan", "Canteen", "2026-09-25", "lost", "Claimed", "2026-09-25 12:30:00", "2026-09-26 15:45:00" },
                { studentUserId, "Bose QuietComfort Earbuds", "Matte black wireless noise cancelling earbuds in black charging case", "Earphones", "Black", "Bose", "Computer Lab 3", "2026-10-02", "lost", "Lost", "2026-10-02 11:20:00", "2026-10-02 11:20:00" },
                { studentUserId, "Fastrack Blue College Backpack", "Navy blue waterproof backpack containing semester notebooks", "Bag", "Blue", "Fastrack", "Sports Complex", "2026-09-18", "lost", "Returned", "2026-09-18 16:00:00", "2026-09-20 10:30:00" },
                { studentUserId, "HP Pavilion Laptop Power Adapter", "65W blue pin barrel charger cord", "Electronics", "Black", "HP", "Seminar Hall", "2026-08-20", "lost", "Closed", "2026-08-20 09:45:00", "2026-08-30 18:00:00" },
                { studentUserId, "Leather Bi-fold Wallet with Cash", "Dark brown wallet containing college ID card and receipts", "Wallet", "Brown", "Wildhorn", "Football Ground", "2026-10-08", "lost", "Lost", "2026-10-08 17:10:00", "2026-10-08 17:10:00" },

                { claimantUserId, "Student College ID Card CSE", "Smart card issued for Computer Science Department B.Tech Year 3", "ID Card", "White", "College", "Admin Block", "2026-09-14", "found", "Returned", "2026-09-14 09:00:00", "2026-09-16 11:30:00" },
                { claimantUserId, "Milton Thermosteel Water Bottle", "Silver 750ml insulated flask with sports cap", "Bottle", "Silver", "Milton", "Ground Floor Corridor", "2026-10-05", "found", "Found", "2026-10-05 14:20:00", "2026-10-05 14:20:00" },
                { claimantUserId, "Set of 3 Bike Keys with Keychain", "Honda bike key with red safety lanyard and metal ring", "Keys", "Silver", "Honda", "Parking Lot A", "2026-10-01", "found", "Verified", "2026-10-01 08:30:00", "2026-10-03 16:00:00" },
                { claimantUserId, "Engineering Mathematics Textbook", "Higher Engineering Mathematics by B.S. Grewal 44th Edition", "Books", "Yellow", "Khanna", "Room 204", "2026-09-28", "found", "Found", "2026-09-28 15:40:00", "2026-09-28 15:40:00" },
                { claimantUserId, "Casio Scientific Calculator FX-991", "Standard black non-programmable engineering calculator", "Electronics", "Black", "Casio", "Physics Lab", "2026-10-06", "found", "Claimed", "2026-10-06 13:10:00", "2026-10-07 10:20:00" },
                { claimantUserId, "Parker Vector Fountain Pen", "Stainless steel body fountain pen with blue ink refill", "Other", "Silver", "Parker", "Reading Room", "2026-08-28", "found", "Closed", "2026-08-28 16:30:00", "2026-09-10 18:00:00" }
            };

            for (Object[] item : demoItems) {
                try (PreparedStatement checkStmt = conn.prepareStatement(checkItemSql)) {
                    checkStmt.setString(1, (String) item[1]);
                    ResultSet rs = checkStmt.executeQuery();
                    if (!rs.next()) {
                        try (PreparedStatement ins = conn.prepareStatement(insertItemSql)) {
                            for (int i = 0; i < item.length; i++) {
                                ins.setObject(i + 1, item[i]);
                            }
                            ins.executeUpdate();
                        }
                    }
                }
            }

            // Create sample claims with various statuses (Approved, Pending, Rejected)
            String insertClaimSql = """
                INSERT INTO claims (item_id, claimant_id, message, proof, status, created_at, reviewed_at, reviewed_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            """;

            // Find item IDs for claims
            createClaimIfMissing(conn, insertClaimSql, "Set of 3 Bike Keys with Keychain", studentUserId, "This is my Honda Activa bike key! Lost it near parking lot A.", "Key has a small red scratch on the lanyard and Honda engraving.", "Pending", "2026-10-02 10:00:00", null);
            createClaimIfMissing(conn, insertClaimSql, "Casio Scientific Calculator FX-991", studentUserId, "I left my calculator on table 4 during physics practicals.", "Has my roll number STU101 faintly engraved on the back lid.", "Pending", "2026-10-06 15:00:00", null);
            createClaimIfMissing(conn, insertClaimSql, "Student College ID Card CSE", studentUserId, "Found my misplaced ID card, matching my name and roll number.", "Matches my student profile photo and CS branch.", "Approved", "2026-09-14 11:00:00", "2026-09-15 14:00:00");
            createClaimIfMissing(conn, insertClaimSql, "Titan Neo Quartz Watch", claimantUserId, "Claiming the Titan wrist watch I lost in the cafeteria.", "Dial has Roman numerals and a small scratch near the 12 position.", "Pending", "2026-09-26 10:00:00", null);
            createClaimIfMissing(conn, insertClaimSql, "Milton Thermosteel Water Bottle", studentUserId, "I think this is my silver bottle.", "It has a regular cap without stickers.", "Rejected", "2026-10-07 09:30:00", "2026-10-08 11:00:00");
            createClaimIfMissing(conn, insertClaimSql, "Apple MacBook Air M2", claimantUserId, "I am the owner of this MacBook.", "Serial number starting with C02F, Apple account logged in.", "Approved", "2026-09-10 14:00:00", "2026-09-11 16:30:00");

            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createClaimIfMissing(Connection conn, String sql, String itemTitle, int claimantId,
                                      String msg, String proof, String status, String createdAt, String reviewedAt) {
        try {
            int itemId = 0;
            try (PreparedStatement s = conn.prepareStatement("SELECT id FROM items WHERE title = ?")) {
                s.setString(1, itemTitle);
                ResultSet rs = s.executeQuery();
                if (rs.next()) {
                    itemId = rs.getInt(1);
                }
            }
            if (itemId > 0) {
                try (PreparedStatement check = conn.prepareStatement("SELECT id FROM claims WHERE item_id = ? AND claimant_id = ?")) {
                    check.setInt(1, itemId);
                    check.setInt(2, claimantId);
                    ResultSet rs = check.executeQuery();
                    if (!rs.next()) {
                        try (PreparedStatement ins = conn.prepareStatement(sql)) {
                            ins.setInt(1, itemId);
                            ins.setInt(2, claimantId);
                            ins.setString(3, msg);
                            ins.setString(4, proof);
                            ins.setString(5, status);
                            ins.setString(6, createdAt);
                            if (reviewedAt != null) {
                                ins.setString(7, reviewedAt);
                            } else {
                                ins.setNull(7, Types.VARCHAR);
                            }
                            ins.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

