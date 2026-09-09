package com.reclaimx.dao;

import com.reclaimx.config.Database;
import com.reclaimx.models.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDao {

    public Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setUserId(rs.getInt("user_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setColor(rs.getString("color"));
        item.setBrand(rs.getString("brand"));
        item.setLocation(rs.getString("location"));
        item.setDateLostFound(rs.getString("date_lost_found"));
        item.setImage(rs.getString("image"));
        item.setType(rs.getString("type"));
        item.setStatus(rs.getString("status"));
        item.setCreatedAt(rs.getString("created_at"));
        item.setUpdatedAt(rs.getString("updated_at"));

        try {
            item.setUserName(rs.getString("user_name"));
            item.setUserEmail(rs.getString("user_email"));
            item.setUserPhone(rs.getString("user_phone"));
        } catch (SQLException ignored) {}

        return item;
    }

    public Item findById(int id) {
        String sql = """
            SELECT i.*, u.name as user_name, u.email as user_email, u.phone as user_phone
            FROM items i
            LEFT JOIN users u ON i.user_id = u.id
            WHERE i.id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createItem(Item item) {
        String sql = """
            INSERT INTO items (user_id, title, description, category, color, brand, location, date_lost_found, image, type, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getUserId());
            pstmt.setString(2, item.getTitle());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory());
            pstmt.setString(5, item.getColor());
            pstmt.setString(6, item.getBrand());
            pstmt.setString(7, item.getLocation());
            pstmt.setString(8, item.getDateLostFound());
            pstmt.setString(9, item.getImage());
            pstmt.setString(10, item.getType());
            pstmt.setString(11, item.getStatus());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateItem(Item item) {
        String sql = """
            UPDATE items SET title = ?, description = ?, category = ?, color = ?, brand = ?, location = ?, date_lost_found = ?, image = ?, updated_at = datetime('now')
            WHERE id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getCategory());
            pstmt.setString(4, item.getColor());
            pstmt.setString(5, item.getBrand());
            pstmt.setString(6, item.getLocation());
            pstmt.setString(7, item.getDateLostFound());
            pstmt.setString(8, item.getImage());
            pstmt.setInt(9, item.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE items SET status = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteItem(int id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Item> findByUserId(int userId) {
        List<Item> list = new ArrayList<>();
        String sql = """
            SELECT i.*, u.name as user_name, u.email as user_email, u.phone as user_phone
            FROM items i
            LEFT JOIN users u ON i.user_id = u.id
            WHERE i.user_id = ?
            ORDER BY i.created_at DESC
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Item> findByUserIdAndType(int userId, String type) {
        List<Item> list = new ArrayList<>();
        String sql = """
            SELECT i.*, u.name as user_name, u.email as user_email, u.phone as user_phone
            FROM items i
            LEFT JOIN users u ON i.user_id = u.id
            WHERE i.user_id = ? AND i.type = ?
            ORDER BY i.created_at DESC
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Item> findBrowseItems(String q, String type, String category, String location, String color, String status, String sort) {
        List<Item> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT i.*, u.name as user_name, u.email as user_email, u.phone as user_phone
            FROM items i
            LEFT JOIN users u ON i.user_id = u.id
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND i.status = ?");
            params.add(status);
        } else {
            sql.append(" AND i.status IN ('Lost', 'Found', 'Matched', 'Claimed', 'Verified', 'Returned', 'Closed')");
        }

        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            sql.append(" AND i.type = ?");
            params.add(type);
        }

        if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND i.category = ?");
            params.add(category);
        }

        if (location != null && !location.isEmpty() && !"all".equalsIgnoreCase(location)) {
            sql.append(" AND i.location LIKE ?");
            params.add("%" + location + "%");
        }

        if (color != null && !color.isEmpty() && !"all".equalsIgnoreCase(color)) {
            sql.append(" AND i.color LIKE ?");
            params.add("%" + color + "%");
        }

        if (q != null && !q.trim().isEmpty()) {
            sql.append(" AND (i.title LIKE ? OR i.description LIKE ? OR i.brand LIKE ? OR i.color LIKE ? OR i.location LIKE ?)");
            String pattern = "%" + q.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY i.created_at ASC");
        } else {
            sql.append(" ORDER BY i.created_at DESC");
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Item> findAllFiltered(String q, String type, String status) {
        List<Item> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT i.*, u.name as user_name, u.email as user_email, u.phone as user_phone
            FROM items i
            LEFT JOIN users u ON i.user_id = u.id
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            sql.append(" AND i.type = ?");
            params.add(type);
        }
        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            sql.append(" AND i.status = ?");
            params.add(status);
        }
        if (q != null && !q.trim().isEmpty()) {
            sql.append(" AND (i.title LIKE ? OR i.description LIKE ? OR i.color LIKE ? OR i.location LIKE ?)");
            String pattern = "%" + q.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY i.created_at DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> findDistinctLocations() {
        List<String> locs = new ArrayList<>();
        String sql = "SELECT DISTINCT location FROM items WHERE location IS NOT NULL AND location != '' ORDER BY location ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                locs.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locs;
    }

    public int countByType(String type) {
        String sql = "SELECT COUNT(*) FROM items WHERE type = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM items WHERE status = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countTotalItems() {
        String sql = "SELECT COUNT(*) FROM items";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
