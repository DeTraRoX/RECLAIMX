package com.reclaimx.dao;

import com.reclaimx.config.Database;
import com.reclaimx.models.Claim;
import com.reclaimx.models.Item;
import com.reclaimx.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClaimDao {

    private final ItemDao itemDao = new ItemDao();
    private final UserDao userDao = new UserDao();

    public Claim mapRow(ResultSet rs) throws SQLException {
        Claim claim = new Claim();
        claim.setId(rs.getInt("id"));
        claim.setItemId(rs.getInt("item_id"));
        claim.setClaimantId(rs.getInt("claimant_id"));
        claim.setMessage(rs.getString("message"));
        claim.setProof(rs.getString("proof"));
        claim.setStatus(rs.getString("status"));
        claim.setCreatedAt(rs.getString("created_at"));
        claim.setReviewedAt(rs.getString("reviewed_at"));

        int rBy = rs.getInt("reviewed_by");
        if (!rs.wasNull()) {
            claim.setReviewedBy(rBy);
        }

        // Fetch relationships
        claim.setItem(itemDao.findById(claim.getItemId()));
        claim.setClaimant(userDao.findById(claim.getClaimantId()));

        if (claim.getReviewedBy() != null) {
            claim.setReviewer(userDao.findById(claim.getReviewedBy()));
        }

        return claim;
    }

    public Claim findById(int id) {
        String sql = "SELECT * FROM claims WHERE id = ?";
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

    public Claim findPendingClaim(int itemId, int claimantId) {
        String sql = "SELECT * FROM claims WHERE item_id = ? AND claimant_id = ? AND status = 'Pending'";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, claimantId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createClaim(Claim claim) {
        String sql = """
            INSERT INTO claims (item_id, claimant_id, message, proof, status, created_at)
            VALUES (?, ?, ?, ?, ?, datetime('now'))
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, claim.getItemId());
            pstmt.setInt(2, claim.getClaimantId());
            pstmt.setString(3, claim.getMessage());
            pstmt.setString(4, claim.getProof());
            pstmt.setString(5, claim.getStatus() != null ? claim.getStatus() : "Pending");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    claim.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(int claimId, String status, Integer reviewerId) {
        String sql = "UPDATE claims SET status = ?, reviewed_at = datetime('now'), reviewed_by = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            if (reviewerId != null) {
                pstmt.setInt(2, reviewerId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, claimId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Claim> findByClaimantId(int claimantId) {
        List<Claim> list = new ArrayList<>();
        String sql = "SELECT * FROM claims WHERE claimant_id = ? ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, claimantId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Claim> findByItemId(int itemId) {
        List<Claim> list = new ArrayList<>();
        String sql = "SELECT * FROM claims WHERE item_id = ? ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Claim> findAllFiltered(String status) {
        List<Claim> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM claims WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isEmpty() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_at DESC");

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

    public Claim findApprovedClaimForItem(int itemId) {
        String sql = "SELECT * FROM claims WHERE item_id = ? AND status = 'Approved' LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM claims WHERE status = ?";
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
}
