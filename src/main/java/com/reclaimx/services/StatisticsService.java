package com.reclaimx.services;

import com.reclaimx.config.Database;
import com.reclaimx.dao.ClaimDao;
import com.reclaimx.dao.ItemDao;
import com.reclaimx.dao.UserDao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class StatisticsService {

    private final UserDao userDao = new UserDao();
    private final ItemDao itemDao = new ItemDao();
    private final ClaimDao claimDao = new ClaimDao();

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
        stats.put("total_matches", 0); // Excluded as per requirement
        stats.put("pending_claims", pendingClaims);
        stats.put("returned_items", returnedItems);
        stats.put("recovery_rate", recoveryRate);
        return stats;
    }

    public Map<String, Object> getDetailedReportData() {
        Map<String, Object> report = new HashMap<>();

        List<String> categoriesList = Arrays.asList(
            "ID Card", "Wallet", "Mobile Phone", "Laptop", "Bag", "Books",
            "Notebook", "Earphones", "Headphones", "Bottle", "Keys",
            "Clothing", "Watch", "Jewellery", "Electronics", "Documents", "Other"
        );

        List<Integer> categoryLostCounts = new ArrayList<>();
        List<Integer> categoryFoundCounts = new ArrayList<>();

        for (String cat : categoriesList) {
            int lostC = countItemsByCatAndType(cat, "lost");
            int foundC = countItemsByCatAndType(cat, "found");
            categoryLostCounts.add(lostC);
            categoryFoundCounts.add(foundC);
        }

        List<Map<String, Object>> locationStats = new ArrayList<>();
        String locSql = "SELECT location, COUNT(id) as cnt FROM items GROUP BY location ORDER BY cnt DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(locSql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("location", rs.getString("location"));
                row.put("count", rs.getInt("cnt"));
                locationStats.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        String monthSql = "SELECT strftime('%Y-%m', created_at) as month, COUNT(id) as cnt FROM items GROUP BY month ORDER BY month ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(monthSql)) {
            while (rs.next()) {
                String m = rs.getString("month");
                if (m != null) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("month", m);
                    row.put("count", rs.getInt("cnt"));
                    monthlyStats.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int returned = itemDao.countByStatus("Returned");
        int active = countActiveItems();
        int closed = itemDao.countByStatus("Rejected") + itemDao.countByStatus("Closed");

        Map<String, Integer> recoveryBreakdown = new HashMap<>();
        recoveryBreakdown.put("returned", returned);
        recoveryBreakdown.put("active", active);
        recoveryBreakdown.put("closed", closed);

        String topCategory = getTopValue("category");
        String topLocation = getTopValue("location");

        report.put("category_labels", categoriesList);
        report.put("category_lost_counts", categoryLostCounts);
        report.put("category_found_counts", categoryFoundCounts);
        report.put("location_stats", locationStats);
        report.put("monthly_stats", monthlyStats);
        report.put("recovery_breakdown", recoveryBreakdown);
        report.put("top_category", topCategory);
        report.put("top_location", topLocation);

        return report;
    }

    private int countItemsByCatAndType(String cat, String type) {
        String sql = "SELECT COUNT(*) FROM items WHERE category = ? AND type = ?";
        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cat);
            pstmt.setString(2, type);
            var rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int countActiveItems() {
        String sql = "SELECT COUNT(*) FROM items WHERE status IN ('Lost', 'Found', 'Matched', 'Claimed', 'Verified')";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getTopValue(String column) {
        String sql = "SELECT " + column + ", COUNT(id) as cnt FROM items GROUP BY " + column + " ORDER BY cnt DESC LIMIT 1";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "None";
    }
}
