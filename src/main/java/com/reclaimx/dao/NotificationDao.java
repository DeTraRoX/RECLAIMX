package com.reclaimx.dao;

import com.reclaimx.config.Database;
import com.reclaimx.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {

    private final ItemDao itemDao = new ItemDao();

    public Notification mapRow(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setId(rs.getInt("id"));
        notif.setUserId(rs.getInt("user_id"));
        notif.setMessage(rs.getString("message"));
        notif.setType(rs.getString("type"));

        int relId = rs.getInt("related_item_id");
        if (!rs.wasNull()) {
            notif.setRelatedItemId(relId);
            notif.setRelatedItem(itemDao.findById(relId));
        }

        notif.setIsRead(rs.getInt("is_read") == 1);
        notif.setCreatedAt(rs.getString("created_at"));
        return notif;
    }

    public Notification findById(int id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
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

    public boolean createNotification(Notification notif) {
        String sql = """
            INSERT INTO notifications (user_id, message, type, related_item_id, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, datetime('now'))
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, notif.getUserId());
            pstmt.setString(2, notif.getMessage());
            pstmt.setString(3, notif.getType());
            if (notif.getRelatedItemId() != null) {
                pstmt.setInt(4, notif.getRelatedItemId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setInt(5, notif.getIsRead() ? 1 : 0);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    notif.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Notification> findByUserId(int userId, int limit) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }

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

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
