package com.reclaimx.util;

import com.reclaimx.dao.UserDao;
import com.reclaimx.models.User;

public class SeedData {

    private static final UserDao userDao = new UserDao();

    public static void seedIfEmpty() {
        if (userDao.findByEmail("admin@reclaimx.com") == null) {
            System.out.println("Seeding default admin user...");
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@reclaimx.com");
            admin.setPasswordHash(PasswordUtil.hashPassword("Admin@123"));
            admin.setStudentId("ADM001");
            admin.setDepartment("Administration");
            admin.setYear(5);
            admin.setPhone("+91 9999999999");
            admin.setRole("admin");
            admin.setIsActive(true);
            userDao.createUser(admin);

            System.out.println("===================================");
            System.out.println("Default Admin Account Created:");
            System.out.println("Email: admin@reclaimx.com");
            System.out.println("Password: Admin@123");
            System.out.println("===================================");
        }

        // Sample student account
        if (userDao.findByEmail("ayush@reclaimx.com") == null) {
            User student = new User();
            student.setName("Ayush Sharma");
            student.setEmail("ayush@reclaimx.com");
            student.setPasswordHash(PasswordUtil.hashPassword("Student@123"));
            student.setStudentId("STU101");
            student.setDepartment("Computer Science");
            student.setYear(3);
            student.setPhone("+91 9876543210");
            student.setRole("student");
            student.setIsActive(true);
            userDao.createUser(student);
        }
    }
}
